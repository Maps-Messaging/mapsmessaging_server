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
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.extension.Extension;
import io.mapsmessaging.network.protocol.impl.tak.codec.CotXmlCodec;
import io.mapsmessaging.network.protocol.impl.tak.codec.TakPayloadCodec;
import io.mapsmessaging.network.protocol.impl.tak.codec.TakProtobufCodec;
import io.mapsmessaging.network.protocol.impl.tak.framing.TakFrameReader;
import io.mapsmessaging.network.protocol.impl.tak.framing.TakStreamFramer;
import io.mapsmessaging.network.protocol.impl.tak.transport.TakConnectionManager;
import io.mapsmessaging.network.protocol.impl.tak.transport.TakMulticastTransport;
import io.mapsmessaging.network.protocol.impl.tak.transport.TakServerConnection;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TakExtension extends Extension {

  private final EndPointURL url;
  private final Logger logger;
  private final TakExtensionConfig config;
  private final TakPayloadCodec payloadCodec;
  private final TakStreamFramer streamFramer;
  private final TakConnectionManager connectionManager;
  private final TakMulticastTransport multicastTransport;
  private final TakFrameReader frameReader;
  private final Set<String> remoteLinks;
  private final Set<String> localLinks;
  private final AtomicBoolean running;
  private volatile Thread readerThread;
  private volatile Thread multicastReaderThread;

  public TakExtension(EndPoint endPoint, @Nullable ExtensionConfigDTO extensionConfig) {
    this.url = new EndPointURL(endPoint.getConfig().getUrl());
    this.logger = LoggerFactory.getLogger(TakExtension.class);
    this.config = TakExtensionConfig.from(extensionConfig);
    this.payloadCodec = TakExtensionConfig.PAYLOAD_TAK_PROTO_V1.equalsIgnoreCase(config.getPayload())
        ? TakProtobufCodec.withSchemaFormatter(
            new CotXmlCodec(),
            config.getMaxPayloadBytes(),
            config.getProtobufDescriptorBase64(),
            config.getProtobufMessageName())
        : new CotXmlCodec();
    this.streamFramer = new TakStreamFramer(config.getFramingMode(), config.getMaxPayloadBytes());
    this.connectionManager = new TakConnectionManager(new TakServerConnection(url, Duration.ofSeconds(30)));
    this.multicastTransport = config.isMulticastEnabled() ? new TakMulticastTransport(config) : null;
    this.frameReader = new TakFrameReader(streamFramer, config.getReadBufferBytes());
    this.remoteLinks = ConcurrentHashMap.newKeySet();
    this.localLinks = ConcurrentHashMap.newKeySet();
    this.running = new AtomicBoolean(false);
  }

  @Override
  public void initialise() throws IOException {
    connectionManager.connect();
    if (multicastTransport != null) {
      multicastTransport.start();
    }
    logger.log(ServerLogMessages.TAK_EXTENSION_INITIALISED, url.toString());
    running.set(true);
    readerThread = new Thread(this::readerLoop, "tak-reader-" + url.getHost() + "-" + url.getPort());
    readerThread.setDaemon(true);
    readerThread.start();
    if (multicastTransport != null && config.isMulticastIngressEnabled()) {
      multicastReaderThread = new Thread(this::multicastReaderLoop, "tak-mcast-reader-" + config.getMulticastGroup() + "-" + config.getMulticastPort());
      multicastReaderThread.setDaemon(true);
      multicastReaderThread.start();
    }
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
    Thread multicastThread = multicastReaderThread;
    if (multicastThread != null) {
      multicastThread.interrupt();
      try {
        multicastThread.join(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    if (multicastTransport != null) {
      multicastTransport.close();
    }
    connectionManager.close();
    logger.log(ServerLogMessages.TAK_EXTENSION_CLOSED, url.toString());
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
      try {
        byte[] framed = streamFramer.frame(payload);
        connectionManager.write(framed);
      } catch (IOException ignored) {
        // Stream path is best-effort; multicast egress may still succeed.
      }
      if (multicastTransport != null && config.isMulticastEgressEnabled()) {
        try {
          multicastTransport.send(payload);
        } catch (IOException ignored) {
          logger.log(ServerLogMessages.TAK_MULTICAST_IO_FAILED, config.getMulticastGroup(), config.getMulticastPort());
        }
      }
    } catch (IOException ignored) {
      logger.log(ServerLogMessages.TAK_EXTENSION_OUTBOUND_FAILED, destinationName);
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
          try {
            Message message = payloadCodec.decode(frame);
            String destination = resolveInboundDestination(message);
            inbound(destination, message);
          } catch (IOException decodeFailure) {
            logger.log(ServerLogMessages.TAK_EXTENSION_DECODE_FAILED, "stream");
          }
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
    long delayMs = config.getReconnectDelayMs();
    while (running.get()) {
      try {
        long sleepFor = applyJitter(delayMs, config.getReconnectJitterMs());
        logger.log(ServerLogMessages.TAK_EXTENSION_RECONNECT_ATTEMPT, sleepFor, url.toString());
        Thread.sleep(sleepFor);
        connectionManager.reconnect();
        logger.log(ServerLogMessages.TAK_EXTENSION_RECONNECT_SUCCESS, url.toString());
        return;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (IOException ignored) {
        logger.log(ServerLogMessages.TAK_EXTENSION_RECONNECT_FAILED, url.toString());
        delayMs = nextDelay(delayMs);
      }
    }
  }

  private long nextDelay(long currentDelayMs) {
    long multiplied = (long) Math.ceil(currentDelayMs * config.getReconnectBackoffMultiplier());
    return Math.min(config.getReconnectMaxDelayMs(), Math.max(config.getReconnectDelayMs(), multiplied));
  }

  private long applyJitter(long delayMs, int jitterMs) {
    if (jitterMs <= 0) {
      return delayMs;
    }
    int jitter = ThreadLocalRandom.current().nextInt(jitterMs + 1);
    return delayMs + jitter;
  }

  private void multicastReaderLoop() {
    while (running.get()) {
      try {
        Optional<byte[]> frame = multicastTransport.read();
        if (frame.isEmpty()) {
          continue;
        }
        try {
          Message message = payloadCodec.decode(frame.get());
          String destination = resolveInboundDestination(message);
          inbound(destination, message);
        } catch (IOException decodeFailure) {
          logger.log(ServerLogMessages.TAK_EXTENSION_DECODE_FAILED, "multicast");
        }
      } catch (IOException ex) {
        if (!running.get()) {
          break;
        }
        logger.log(ServerLogMessages.TAK_MULTICAST_IO_FAILED, config.getMulticastGroup(), config.getMulticastPort());
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
