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
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.extension.Extension;
import io.mapsmessaging.network.protocol.impl.tak.codec.CotXmlCodec;
import io.mapsmessaging.network.protocol.impl.tak.codec.TakPayloadCodec;
import io.mapsmessaging.network.protocol.impl.tak.codec.TakProtobufCodec;
import io.mapsmessaging.network.protocol.impl.tak.framing.TakFrameReader;
import io.mapsmessaging.network.protocol.impl.tak.framing.TakStreamFramer;
import io.mapsmessaging.network.protocol.impl.tak.transport.TakMulticastTransport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TakExtension extends Extension {

  private static final String PROTOCOL_NAME = "TAK";
  private static final String PROTOCOL_VERSION = "1.0";
  private static final String DEFAULT_EVENT_TYPE = "unknown";
  private static final String DEFAULT_DESTINATION_PREFIX = "tak/cot/";
  private static final String WILDCARD_MARKER = "#";

  private final EndPointURL url;
  private final Logger logger;
  private final EndPoint endPoint;
  private final TakExtensionConfig config;
  private final TakPayloadCodec payloadCodec;
  private final TakStreamFramer streamFramer;
  private final TakMulticastTransport multicastTransport;
  private final TakFrameReader frameReader;
  private final Set<String> remoteLinks;
  private final AtomicBoolean running;
  private volatile Thread multicastReaderThread;

  public TakExtension(EndPoint endPoint, @Nullable ExtensionConfigDTO extensionConfig) {
    this.endPoint = endPoint;
    this.url = new EndPointURL(endPoint.getConfig().getUrl());
    this.logger = LoggerFactory.getLogger(TakExtension.class);
    this.config = TakExtensionConfig.from(extensionConfig);
    CotXmlCodec cotXmlCodec = CotXmlCodec.withSchemaFormatter(
        config.isXmlNamespaceAware(),
        config.isXmlCoalescing(),
        config.isXmlValidating(),
        config.getXmlRootEntry(),
        config.getXmlSchemaId());
    this.payloadCodec = TakExtensionConfig.PAYLOAD_TAK_PROTO_V1.equalsIgnoreCase(config.getPayload())
        ? TakProtobufCodec.withSchemaFormatter(
            cotXmlCodec,
            config.getMaxPayloadBytes(),
            config.getProtobufDescriptorBase64(),
            config.getProtobufMessageName(),
            config.getProtobufSchemaId())
        : cotXmlCodec;
    this.streamFramer = new TakStreamFramer(config.getFramingMode(), config.getMaxPayloadBytes());
    this.multicastTransport = config.isMulticastEnabled() ? new TakMulticastTransport(config) : null;
    this.frameReader = new TakFrameReader(streamFramer, config.getReadBufferBytes());
    this.remoteLinks = ConcurrentHashMap.newKeySet();
    this.running = new AtomicBoolean(false);
  }

  @Override
  public void initialise() throws IOException {
    if (multicastTransport != null) {
      multicastTransport.start();
    }
    logger.log(ServerLogMessages.TAK_EXTENSION_INITIALISED, url.toString());
    running.set(true);
    if (multicastTransport != null && config.isMulticastIngressEnabled()) {
      multicastReaderThread = new Thread(this::multicastReaderLoop, "tak-mcast-reader-" + config.getMulticastGroup() + "-" + config.getMulticastPort());
      multicastReaderThread.setDaemon(true);
      multicastReaderThread.start();
    }
  }

  @Override
  public void close() throws IOException {
    running.set(false);
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
    logger.log(ServerLogMessages.TAK_EXTENSION_CLOSED, url.toString());
  }

  @Override
  public String getName() {
    return PROTOCOL_NAME;
  }

  @Override
  public String getVersion() {
    return PROTOCOL_VERSION;
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
        endPoint.sendPacket(new Packet(ByteBuffer.wrap(framed)));
      } catch (IOException ignored) {
        // Stream path is best-effort; multicast egress may still succeed.
      }
      if (multicastTransport != null && config.isMulticastEgressEnabled()) {
        try {
          multicastTransport.send(payload);
          endPoint.updateWriteBytes(payload.length);
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
    // Egress mapping is already handled by ExtensionProtocol/Protocol.parseOutboundMessage.
  }

  @Override
  public boolean processPacket(@NotNull Packet packet) throws IOException {
    if (!running.get()) {
      return false;
    }
    ByteBuffer raw = packet.getRawBuffer();
    if (raw == null || raw.remaining() <= 0) {
      return false;
    }
    for (byte[] frame : frameReader.read(raw)) {
      processInboundFrame(frame, "stream");
    }
    packet.position(packet.limit());
    return true;
  }

  private void multicastReaderLoop() {
    while (running.get()) {
      try {
        Optional<byte[]> frame = multicastTransport.read();
        if (frame.isEmpty()) {
          continue;
        }
        endPoint.updateReadBytes(frame.get().length);
        processInboundFrame(frame.get(), "multicast");
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
      if (eventType == null || eventType.isBlank()) {
        eventType = message.getMeta().get("tak_type");
      }
    }
    if (eventType == null || eventType.isBlank()) {
      eventType = DEFAULT_EVENT_TYPE;
    }
    if (!remoteLinks.isEmpty()) {
      String remoteDestination = remoteLinks.iterator().next();
      if (remoteDestination.contains(WILDCARD_MARKER)) {
        return remoteDestination.replace(WILDCARD_MARKER, eventType);
      }
      return remoteDestination;
    }
    return DEFAULT_DESTINATION_PREFIX + eventType;
  }

  private void processInboundFrame(byte[] frame, String source) {
    try {
      Message message = payloadCodec.decode(frame);
      String destination = resolveInboundDestination(message);
      inbound(destination, message);
    } catch (IOException decodeFailure) {
      logger.log(ServerLogMessages.TAK_EXTENSION_DECODE_FAILED, source);
    }
  }
}
