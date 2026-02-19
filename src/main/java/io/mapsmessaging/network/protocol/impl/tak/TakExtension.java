/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.tak;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.impl.ExtensionConfigDTO;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.extension.Extension;
import io.mapsmessaging.network.protocol.impl.tak.codec.CotXmlCodec;
import io.mapsmessaging.network.protocol.impl.tak.codec.TakPayloadCodec;
import io.mapsmessaging.network.protocol.impl.tak.codec.TakProtobufCodec;
import io.mapsmessaging.network.protocol.impl.tak.framing.TakFrameReader;
import io.mapsmessaging.network.protocol.impl.tak.framing.TakStreamFramer;
import io.mapsmessaging.network.protocol.impl.tak.transport.TakConnectionManager;
import io.mapsmessaging.network.protocol.impl.tak.transport.TakServerConnection;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TakExtension extends Extension {

  private final EndPointURL url;
  private final TakExtensionConfig config;
  private final TakPayloadCodec payloadCodec;
  private final TakStreamFramer streamFramer;
  private final TakConnectionManager connectionManager;
  private final TakFrameReader frameReader;
  private final Set<String> remoteLinks;
  private final Set<String> localLinks;
  private final AtomicBoolean running;
  private volatile Thread readerThread;

  public TakExtension(EndPoint endPoint, @Nullable ExtensionConfigDTO extensionConfig) {
    this.url = new EndPointURL(endPoint.getConfig().getUrl());
    this.config = TakExtensionConfig.from(extensionConfig);
    this.payloadCodec = TakExtensionConfig.PAYLOAD_TAK_PROTO_V1.equalsIgnoreCase(config.getPayload())
        ? new TakProtobufCodec()
        : new CotXmlCodec();
    this.streamFramer = new TakStreamFramer(config.getFramingMode(), config.getMaxPayloadBytes());
    this.connectionManager = new TakConnectionManager(new TakServerConnection(url, Duration.ofSeconds(30)));
    this.frameReader = new TakFrameReader(streamFramer, config.getReadBufferBytes());
    this.remoteLinks = ConcurrentHashMap.newKeySet();
    this.localLinks = ConcurrentHashMap.newKeySet();
    this.running = new AtomicBoolean(false);
  }

  @Override
  public void initialise() throws IOException {
    connectionManager.connect();
    running.set(true);
    readerThread = new Thread(this::readerLoop, "tak-reader-" + url.getHost() + "-" + url.getPort());
    readerThread.setDaemon(true);
    readerThread.start();
  }

  @Override
  public void close() throws IOException {
    running.set(false);
    Thread thread = readerThread;
    if (thread != null) {
      thread.interrupt();
      try {
        thread.join(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    connectionManager.close();
  }

  @Override
  public String getName() {
    return "TAK";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public boolean supportsRemoteFiltering() {
    return false;
  }

  @Override
  public void outbound(String destinationName, Message message) {
    if (!running.get()) {
      return;
    }
    try {
      byte[] payload = payloadCodec.encode(message);
      byte[] framed = streamFramer.frame(payload);
      connectionManager.write(framed);
    } catch (IOException ignored) {
      // Connection failures are handled by reader loop reconnect logic.
    }
  }

  @Override
  public void registerRemoteLink(String destination, @Nullable String filter) {
    remoteLinks.add(destination);
  }

  @Override
  public void registerLocalLink(String destination) {
    localLinks.add(destination);
  }

  private void readerLoop() {
    while (running.get()) {
      try {
        List<byte[]> frames = frameReader.read(connectionManager.input());
        for (byte[] frame : frames) {
          Message message = payloadCodec.decode(frame);
          String destination = resolveInboundDestination(message);
          inbound(destination, message);
        }
      } catch (IOException ex) {
        if (!running.get()) {
          break;
        }
        reconnectWithDelay();
      }
    }
  }

  private void reconnectWithDelay() {
    while (running.get()) {
      try {
        Thread.sleep(config.getReconnectDelayMs());
        connectionManager.reconnect();
        return;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (IOException ignored) {
        // Keep trying while running.
      }
    }
  }

  private String resolveInboundDestination(Message message) {
    String eventType = null;
    if (message.getMeta() != null) {
      eventType = message.getMeta().get("tak.type");
    }
    if (eventType == null || eventType.isBlank()) {
      eventType = "unknown";
    }
    if (!remoteLinks.isEmpty()) {
      String remote = remoteLinks.iterator().next();
      if (remote.contains("#")) {
        return remote.replace("#", eventType);
      }
      return remote;
    }
    return "tak/cot/" + eventType;
  }
}
