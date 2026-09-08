/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import static io.mapsmessaging.logging.ServerLogMessages.COT_PROTOCOL_OUTBOUND_SEND_FAILED;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.cot.CotStreamDecoder;
import io.mapsmessaging.cot.CotStreamEncoder;
import io.mapsmessaging.dto.rest.config.protocol.impl.CotConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.CotProtocolInformation;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.security.auth.Subject;
import javax.xml.stream.XMLStreamException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CotProtocol extends Protocol {

  private static final Logger LOGGER = LoggerFactory.getLogger(CotProtocol.class);

  private final CotConfigDTO cotConfig;
  private final CotStreamDecoder decoder;
  private final CotStreamEncoder encoder;
  private final SelectorTask selectorTask;
  private final Session session;
  private final String inboundTopicName;
  private final String outboundTopicName;
  private final QualityOfService qos;
  private final List<InboundBinding> inboundBindings;
  private final Set<String> outboundSubscriptions;
  private final String echoOrigin;
  private ScheduledFuture<?> presenceFuture;

  public CotProtocol(EndPoint endPoint, Packet initialPacket) throws IOException {
    super(endPoint, endPoint.getConfig().getProtocolConfig("cot"));
    cotConfig = (CotConfigDTO) protocolConfig;
    decoder = new CotStreamDecoder(cotConfig.getMaximumEventSize());
    encoder = new CotStreamEncoder();
    qos = QualityOfService.getInstance(cotConfig.getQualityOfService());
    inboundTopicName = resolveTopic(cotConfig.getInboundTopicName());
    outboundTopicName = resolveTopic(cotConfig.getOutboundTopicName());
    echoOrigin = resolveInterfaceName(cotConfig.getEchoOrigin());
    if (cotConfig.isSuppressEchoes() && (echoOrigin == null || echoOrigin.isBlank())) {
      throw new IOException("CoT echoOrigin must not be blank when echo suppression is enabled");
    }
    inboundBindings = new CopyOnWriteArrayList<>();
    outboundSubscriptions = ConcurrentHashMap.newKeySet();
    try {
      session = buildSession("cot_" + endPoint.getName() + "_" + endPoint.getId(),
          cotConfig.getMaximumSessionExpiry());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while creating CoT session", e);
    } catch (ExecutionException | TimeoutException e) {
      throw new IOException("Unable to create CoT session", e);
    }
    if (!endPoint.isClient()) {
      addOutboundSubscription(outboundTopicName, null, qos);
    }
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    if (initialPacket != null) {
      connect(null, null, null);
      processPacket(initialPacket);
    } else {
      selectorTask.register(SelectionKey.OP_READ);
    }
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException {
    setConnected(true);
    completedConnection();
    startPresenceIfConfigured();
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
    super.subscribeLocal(
        resource,
        mappedResource,
        qualityOfService,
        selector,
        transformer,
        namespaceFilters,
        statistics,
        linkProperties);
    addOutboundSubscription(resource, selector, qualityOfService);
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
    super.subscribeRemote(
        resource,
        mappedResource,
        qualityOfService,
        parser,
        transformer,
        statistics,
        linkProperties);
    inboundBindings.add(new InboundBinding(mappedResource, parser));
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    byte[] bytes = new byte[packet.available()];
    packet.get(bytes);
    List<byte[]> events = decoder.accept(bytes);
    for (byte[] event : events) {
      publishInbound(event);
      receivedMessage();
    }
    selectorTask.register(SelectionKey.OP_READ);
    return true;
  }

  private void publishInbound(byte[] xml) throws IOException {
    if (cotConfig.isSuppressEchoes() && CotEchoSuppressor.isEcho(xml, echoOrigin)) {
      return;
    }
    if (inboundBindings.isEmpty()) {
      if (inboundTopicName != null && !inboundTopicName.isBlank()) {
        publishInbound(xml, inboundTopicName, null);
      }
      return;
    }
    for (InboundBinding binding : inboundBindings) {
      publishInbound(xml, binding.destination(), binding.parser());
    }
  }

  private void publishInbound(
      byte[] xml,
      String destinationName,
      @Nullable ParserExecutor parser) throws IOException {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("protocol", "CoT");
    metadata.put("version", "2.0");
    metadata.put("sessionId", session.getName());
    Message message = new MessageBuilder()
        .setOpaqueData(xml)
        .setContentType("text/xml")
        .setSchemaId(SchemaManager.DEFAULT_XML_SCHEMA.toString())
        .setQoS(qos)
        .setRetain(false)
        .setResponseTopic(outboundTopicName)
        .storeOffline(cotConfig.isStoreOffline())
        .setMeta(metadata)
        .build();
    if (parser != null && !parser.evaluate(message)) {
      return;
    }
    ParsedMessage parsedMessage = parseInboundMessage(destinationName, message);
    if (parsedMessage == null) {
      return;
    }
    try {
      Destination destination =
          session.findDestination(parsedMessage.getDestinationName(), DestinationType.TOPIC).get();
      destination.storeMessage(parsedMessage.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while publishing CoT event", e);
    } catch (ExecutionException e) {
      throw new IOException("Unable to publish CoT event", e);
    }
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    ParsedMessage parsedMessage = parseOutboundMessage(messageEvent);
    if (parsedMessage == null) {
      return;
    }
    try {
      byte[] xml = parsedMessage.getMessage().getOpaqueData();
      if (xml == null || xml.length == 0) {
        return;
      }
      byte[] encoded = encoder.encode(markForEchoSuppression(xml));
      sendEncoded(encoded);
    } catch (IOException exception) {
      LOGGER.log(
          COT_PROTOCOL_OUTBOUND_SEND_FAILED,
          exception,
          endPoint.getName(),
          exception.getMessage());
    } finally {
      if (messageEvent.getCompletionTask() != null) {
        messageEvent.getCompletionTask().run();
      }
    }
  }

  @Override
  public void close() throws IOException {
    stopPresence();
    if (!session.isClosed()) {
      SessionManager.getInstance().close(session, false);
    }
    super.close();
  }

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    CotProtocolInformation information = new CotProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

  @Override
  public String getName() {
    return "cot";
  }

  @Override
  public String getSessionId() {
    return session.getName();
  }

  @Override
  public String getVersion() {
    return "2.0";
  }

  private String resolveTopic(String topic) {
    return topic == null ? null : resolveInterfaceName(topic);
  }

  private void addOutboundSubscription(String resource, String selector, QualityOfService qualityOfService)
      throws IOException {
    if (resource == null || resource.isBlank()) {
      return;
    }
    if (!outboundSubscriptions.add(resource)) {
      return;
    }
    try {
      SubscriptionContextBuilder builder =
          createSubscriptionContextBuilder(resource, selector, qualityOfService, 10);
      builder.setNoLocalMessages(true);
      session.addSubscription(builder.build());
    } catch (IOException exception) {
      outboundSubscriptions.remove(resource);
      throw exception;
    }
  }

  private void startPresenceIfConfigured() throws IOException {
    if (!endPoint.isClient() || !cotConfig.getPresence().isEnabled()) {
      return;
    }
    CotPresenceBuilder.validate(cotConfig.getPresence());
    stopPresence();
    sendPresence();
    int intervalSeconds = cotConfig.getPresence().getIntervalSeconds();
    presenceFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(
        this::sendScheduledPresence,
        intervalSeconds,
        intervalSeconds,
        TimeUnit.SECONDS);
  }

  private void sendScheduledPresence() {
    try {
      sendPresence();
    } catch (IOException exception) {
      LOGGER.log(
          COT_PROTOCOL_OUTBOUND_SEND_FAILED,
          exception,
          endPoint.getName(),
          exception.getMessage());
    }
  }

  private void sendPresence() throws IOException {
    try {
      byte[] presence = CotPresenceBuilder.build(
          cotConfig.getPresence(),
          endPoint.getConfig().getName(),
          Instant.now());
      sendEncoded(encoder.encode(markForEchoSuppression(presence)));
    } catch (XMLStreamException exception) {
      throw new IOException("Unable to encode CoT presence event", exception);
    }
  }

  private void sendEncoded(byte[] encoded) throws IOException {
    byte[] output = encoded;
    if (cotConfig.isAppendNewLine() && encoded[encoded.length - 1] != '\n') {
      output = Arrays.copyOf(encoded, encoded.length + 1);
      output[output.length - 1] = '\n';
    }
    endPoint.sendPacket(new Packet(ByteBuffer.wrap(output)));
    sentMessage();
  }

  private byte[] markForEchoSuppression(byte[] xml) throws IOException {
    return cotConfig.isSuppressEchoes() ? CotEchoSuppressor.mark(xml, echoOrigin) : xml;
  }

  private String resolveInterfaceName(String value) {
    return value == null ? null : value.replace("{interfaceName}", endPoint.getConfig().getName());
  }

  private void stopPresence() {
    ScheduledFuture<?> future = presenceFuture;
    presenceFuture = null;
    if (future != null) {
      future.cancel(false);
    }
  }

  private record InboundBinding(
      String destination,
      @Nullable ParserExecutor parser) {
  }
}
