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

package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.impl.CotProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Receives CoT XML events and publishes each complete document through the protocol session
 * to the configured inbound namespace. Outbound subscription events use the shared selector
 * writer. The session uses the built-in anonymous identity; TCP/TLS is managed by the endpoint.
 *
 * <p>Discovered via the standard ProtocolImplFactory ServiceLoader registration.
 */
public class CotProtocol extends Protocol {

  private static final String DESTINATION_NAME = "/tak/cot/inbound";
  private static final int MAX_BUFFER_SIZE = 1_048_576; // 1MB safety cap against a malformed/never-terminated stream
  // A TLS record is indivisible and decrypts to up to 16 KB, so every read must have room for a
  // whole record. The packet handed over by protocol detection is only 1 KB and must not be reused
  // for reads: over ssl:// any record carrying more than 1 KB of CoT would never decode.
  private static final int MIN_READ_BUFFER_SIZE = 32 * 1024;

  private final Logger logger = LoggerFactory.getLogger(CotProtocol.class);
  private final SelectorTask selectorTask;
  private final CotStreamCodec streamCodec;
  private final String sessionId;
  private volatile String inboundDestinationName = DESTINATION_NAME;
  private String resolvedDestinationName;
  private CompletableFuture<Destination> destinationFuture;
  private CompletableFuture<Void> publicationFuture = CompletableFuture.completedFuture(null);

  private Session session;
  private boolean closed;

  public CotProtocol(EndPoint endPoint, Packet initialPacket, CotProtocolConfigDTO config) throws IOException {
    this(endPoint, initialPacket, config, new CotXmlStreamCodec(MAX_BUFFER_SIZE));
  }

  CotProtocol(EndPoint endPoint, Packet initialPacket, CotProtocolConfigDTO config, CotStreamCodec streamCodec) throws IOException {
    super(endPoint, config != null ? config : new CotProtocolConfigDTO());
    this.streamCodec = Objects.requireNonNull(streamCodec, "streamCodec");
    this.selectorTask = new SelectorTask(this, selectorConfig(endPoint));
    this.sessionId = "cot-" + UUID.randomUUID();
    createSession();
    logger.log(ServerLogMessages.COT_CONNECTION_CREATED, endPoint.getConfig().getUrl());
    if (initialPacket != null && initialPacket.available() > 0) {
      appendAndProcess(initialPacket);
    }
    selectorTask.register(SelectionKey.OP_READ);
  }

  /** The configured server read buffer size, never smaller than one TLS record and never above the 1 MB cap. */
  static int readBufferSize(EndPoint endPoint) {
    long configured = 0;
    if (endPoint.getConfig() != null && endPoint.getConfig().getEndPointConfig() != null) {
      configured = endPoint.getConfig().getEndPointConfig().getServerReadBufferSize();
    }
    if (configured < MIN_READ_BUFFER_SIZE) {
      return MIN_READ_BUFFER_SIZE;
    }
    return (int) Math.min(configured, MAX_BUFFER_SIZE);
  }

  private void createSession() throws IOException {
    try {
      SessionContextBuilder scb = new SessionContextBuilder(sessionId, new ProtocolClientConnection(this));
      scb.setUsername("anonymous")
          .setPassword("".toCharArray())
          .setPersistentSession(false)
          .setSessionExpiry(0)
          .setReceiveMaximum(1);
      session = SessionManager.getInstance().create(scb.build(), this);
      findInboundDestination(inboundDestinationName);
    } catch (LoginException e) {
      logger.log(ServerLogMessages.COT_SESSION_CREATE_FAILED, e);
      throw new IOException("Unable to create CoT passthrough session", e);
    }
  }

  private static EndPointConfigDTO selectorConfig(EndPoint endPoint) {
    EndPointConfigDTO config = new EndPointConfigDTO("tcp");
    config.setServerReadBufferSize(readBufferSize(endPoint));
    EndPointConfigDTO configured = endPoint.getConfig().getEndPointConfig();
    if (configured != null) {
      config.setServerWriteBufferSize(configured.getServerWriteBufferSize());
    }
    return config;
  }

  private void appendAndProcess(Packet packet) throws IOException {
    byte[] chunk = new byte[packet.available()];
    packet.get(chunk);
    for (byte[] document : streamCodec.decode(chunk)) {
      publish(document);
    }
  }

  private synchronized void publish(byte[] xml) {
    Message message = new MessageBuilder()
        .setOpaqueData(xml)
        .setContentType("text/xml")
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setRetain(false)
        .build();

    String destinationName = inboundDestinationName;
    // Preserve receive order while an asynchronous destination lookup is pending.
    publicationFuture = publicationFuture.handle((unused, previousFailure) -> null)
        .thenCompose(unused -> findInboundDestination(destinationName).handle((destination, throwable) -> {
          if (throwable != null) {
            logger.log(ServerLogMessages.COT_PUBLISH_FAILED, throwable, destinationName);
          } else if (destination != null) {
            try {
              destination.storeMessage(message);
            } catch (IOException e) {
              logger.log(ServerLogMessages.COT_PUBLISH_FAILED, e, destinationName);
            }
          }
          return null;
        }));
  }

  private synchronized CompletableFuture<Destination> findInboundDestination(String destinationName) {
    if (destinationFuture != null && destinationName.equals(resolvedDestinationName)) {
      if (!destinationFuture.isDone()) {
        return destinationFuture;
      }
      if (!destinationFuture.isCompletedExceptionally()) {
        Destination destination = destinationFuture.getNow(null);
        if (destination != null && !destination.isClosed()) {
          return destinationFuture;
        }
      }
    }
    resolvedDestinationName = destinationName;
    destinationFuture = session.findDestination(destinationName, DestinationType.TOPIC);
    return destinationFuture;
  }

  private void closeQuietly() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      if (session != null) {
        SessionManager.getInstance().close(session, false);
      }
    } catch (IOException e) {
      logger.log(ServerLogMessages.COT_CONNECTION_EXCEPTION, e);
    }
    try {
      super.close();
    } catch (IOException e) {
      logger.log(ServerLogMessages.COT_CONNECTION_EXCEPTION, e);
    }
    selectorTask.close();
    logger.log(ServerLogMessages.COT_CONNECTION_CLOSED);
  }

  @Override
  public void close() throws IOException {
    closeQuietly();
  }

  @Override
  public Subject getSubject() {
    return session != null ? session.getSecurityContext().getSubject() : null;
  }

  @Override
  public void sendMessage(@NotNull MessageEvent messageEvent) {
    try {
      ParsedMessage parsedMessage = parseOutboundMessage(messageEvent);
      if (parsedMessage == null) {
        return;
      }
      byte[] encoded = streamCodec.encode(parsedMessage.getMessage().getOpaqueData());
      selectorTask.push(new CotFrame(encoded, messageEvent.getCompletionTask(), this));
    } catch (IOException | RuntimeException e) {
      logger.log(ServerLogMessages.COT_SEND_FAILED, e);
      if (messageEvent.getCompletionTask() != null) {
        messageEvent.getCompletionTask().run();
      }
    }
  }

  @Override
  public void subscribeLocal(
      @NonNull @NotNull String resource,
      @NonNull @NotNull String mappedResource,
      @NonNull @NotNull QualityOfService qualityOfService,
      @Nullable String selector,
      @Nullable InterServerTransformation transformer,
      @Nullable NamespaceFilters namespaceFilters,
      @Nullable StatisticsConfigDTO statistics,
      @Nullable Map<String, Object> linkProperties) throws IOException {
    super.subscribeLocal(resource, mappedResource, qualityOfService, selector, transformer, namespaceFilters, statistics, linkProperties);
    session.addSubscription(createSubscriptionContextBuilder(resource, selector, qualityOfService, 10).build());
  }

  @Override
  public void subscribeRemote(
      @NonNull @NotNull String resource,
      @NonNull @NotNull String mappedResource,
      @NonNull @NotNull QualityOfService qualityOfService,
      @Nullable ParserExecutor parser,
      @Nullable InterServerTransformation transformer,
      @Nullable StatisticsConfigDTO statistics,
      @Nullable Map<String, Object> linkProperties) throws IOException {
    super.subscribeRemote(resource, mappedResource, qualityOfService, parser, transformer, statistics, linkProperties);
    inboundDestinationName = mappedResource;
    findInboundDestination(mappedResource);
  }

  @Override
  public void sendKeepAlive() {
    // No keep alive required for this passthrough listener
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    if (packet.hasRemaining()) {
      EndPoint.totalReceived.increment();
    }
    appendAndProcess(packet);
    return true;
  }

  @Override
  public String getName() {
    return "CoT";
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public String getVersion() {
    return streamCodec.getVersion();
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    ProtocolInformationDTO dto = new ProtocolInformationDTO();
    updateInformation(dto);
    return dto;
  }
}
