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

import static io.mapsmessaging.logging.ServerLogMessages.COT_PROTOCOL_EVENT_REJECTED;
import static io.mapsmessaging.logging.ServerLogMessages.COT_PROTOCOL_INBOUND_PROCESSING_FAILED;
import static io.mapsmessaging.logging.ServerLogMessages.COT_PROTOCOL_OUTBOUND_SEND_FAILED;
import static io.mapsmessaging.logging.ServerLogMessages.COT_PROTOCOL_WRITE_TIMEOUT;

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
import java.nio.channels.SelectionKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
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
  private final Clock clock;
  private final CotEventTracker eventTracker;
  private final CotOutboundQueue outboundQueue;
  private final CotInboundQueue inboundQueue;
  private final ExecutorService inboundExecutor;
  private final AtomicBoolean closing;
  private final LongAdder malformedEvents;
  private final LongAdder expiredEvents;
  private final LongAdder notStartedEvents;
  private final LongAdder duplicateEvents;
  private final LongAdder directEchoes;
  private final LongAdder semanticEchoes;
  private final LongAdder hopLimitEvents;
  private final LongAdder olderEvents;
  private final LongAdder unsupportedEvents;
  private final LongAdder inboundOverflow;
  private final LongAdder outboundOverflow;
  private final AtomicBoolean warningPending;
  private ScheduledFuture<?> presenceFuture;
  private ScheduledFuture<?> writeMonitorFuture;

  public CotProtocol(EndPoint endPoint, Packet initialPacket) throws IOException {
    super(endPoint, endPoint.getConfig().getProtocolConfig("cot"));
    cotConfig = (CotConfigDTO) protocolConfig;
    validateConfiguration(cotConfig);
    clock = Clock.systemUTC();
    decoder = new CotStreamDecoder(cotConfig.getMaximumEventSize());
    encoder = new CotStreamEncoder();
    qos = QualityOfService.getInstance(cotConfig.getQualityOfService());
    inboundTopicName = resolveTopic(cotConfig.getInboundTopicName());
    outboundTopicName = resolveTopic(cotConfig.getOutboundTopicName());
    echoOrigin = resolveInterfaceName(cotConfig.getEchoOrigin());
    if (cotConfig.isSuppressEchoes() && (echoOrigin == null || echoOrigin.isBlank())) {
      throw new IOException("CoT echoOrigin must not be blank when echo suppression is enabled");
    }
    closing = new AtomicBoolean();
    malformedEvents = new LongAdder();
    expiredEvents = new LongAdder();
    notStartedEvents = new LongAdder();
    duplicateEvents = new LongAdder();
    directEchoes = new LongAdder();
    semanticEchoes = new LongAdder();
    hopLimitEvents = new LongAdder();
    olderEvents = new LongAdder();
    unsupportedEvents = new LongAdder();
    inboundOverflow = new LongAdder();
    outboundOverflow = new LongAdder();
    warningPending = new AtomicBoolean();
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
    eventTracker = new CotEventTracker(
        echoOrigin,
        cotConfig.getMaximumHopCount(),
        cotConfig.getFingerprintCacheSize(),
        Duration.ofSeconds(cotConfig.getFingerprintCacheTtlSeconds()),
        Duration.ofSeconds(cotConfig.getClockSkewSeconds()),
        cotConfig.getMaximumTrackedUids(),
        clock);
    inboundExecutor = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "CoT-inbound-" + endPoint.getName());
      thread.setDaemon(true);
      return thread;
    });
    inboundQueue = new CotInboundQueue(
        cotConfig.getInboundQueueDepth(),
        inboundExecutor,
        this::publishAcceptedInbound,
        (xml, event) -> { },
        this::recordInboundFailure);
    outboundQueue = new CotOutboundQueue(
        cotConfig.getOutboundQueueDepth(),
        writerChunkSize(endPoint),
        selectorTask::push,
        this::isStillValidOutbound,
        eventTracker::rememberOutbound,
        ignored -> sentMessage(),
        ignored -> { },
        clock);
    try {
      if (initialPacket != null) {
        connect(null, null, null);
        processPacket(initialPacket);
      } else {
        selectorTask.register(SelectionKey.OP_READ);
      }
      writeMonitorFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(
          this::checkWriteProgress,
          1,
          1,
          TimeUnit.SECONDS);
    } catch (IOException | RuntimeException exception) {
      try {
        close();
      } catch (IOException cleanupException) {
        exception.addSuppressed(cleanupException);
      }
      throw exception;
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
      receivedMessage();
      processInbound(event);
    }
    selectorTask.register(SelectionKey.OP_READ);
    return true;
  }

  private void processInbound(byte[] xml) {
    CotEchoSuppressor.CotEventInfo event;
    try {
      event = CotEchoSuppressor.inspect(xml, cotConfig.getMaximumXmlDepth());
    } catch (IOException exception) {
      malformedEvents.increment();
      warnControlled("malformed event");
      return;
    }
    CotEventTracker.Decision decision = eventTracker.evaluateInbound(event);
    if (decision != CotEventTracker.Decision.ACCEPT) {
      recordDecision(decision);
      return;
    }
    recordUnsupported(event);
    CotInboundQueue.OfferResult result = inboundQueue.offer(xml, event);
    if (result == CotInboundQueue.OfferResult.DROPPED
        || result == CotInboundQueue.OfferResult.CLOSED) {
      inboundOverflow.increment();
      warnControlled("inbound queue overflow");
    }
  }

  private void publishAcceptedInbound(
      byte[] xml,
      CotEchoSuppressor.CotEventInfo event) throws IOException {
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
    Runnable completion = messageEvent.getCompletionTask();
    try {
      byte[] xml = parsedMessage.getMessage().getOpaqueData();
      if (xml == null || xml.length == 0) {
        complete(completion);
        return;
      }
      enqueueOutbound(xml, completion);
    } catch (IOException exception) {
      complete(completion);
      LOGGER.log(
          COT_PROTOCOL_OUTBOUND_SEND_FAILED,
          exception,
          endPoint.getName(),
          exception.getMessage());
    }
  }

  @Override
  public void close() throws IOException {
    if (!closing.compareAndSet(false, true)) {
      return;
    }
    stopPresence();
    if (writeMonitorFuture != null) {
      writeMonitorFuture.cancel(false);
      writeMonitorFuture = null;
    }
    inboundQueue.close();
    inboundExecutor.shutdownNow();
    outboundQueue.close();
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
    information.setMalformedEvents(malformedEvents.sum());
    information.setExpiredEvents(expiredEvents.sum());
    information.setNotStartedEvents(notStartedEvents.sum());
    information.setDuplicateEvents(duplicateEvents.sum());
    information.setDirectEchoes(directEchoes.sum());
    information.setSemanticEchoes(semanticEchoes.sum());
    information.setHopLimitEvents(hopLimitEvents.sum());
    information.setOlderEvents(olderEvents.sum());
    information.setUnsupportedEvents(unsupportedEvents.sum());
    information.setInboundQueueDepth(inboundQueue.size());
    information.setInboundOverflow(inboundOverflow.sum());
    information.setOutboundQueueDepth(outboundQueue.size());
    information.setOutboundOverflow(outboundOverflow.sum());
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
          clock.instant());
      enqueueOutbound(presence, null);
    } catch (XMLStreamException exception) {
      throw new IOException("Unable to encode CoT presence event", exception);
    }
  }

  private void enqueueOutbound(byte[] xml, Runnable completion) throws IOException {
    CotEchoSuppressor.CotEventInfo event =
        CotEchoSuppressor.inspect(xml, cotConfig.getMaximumXmlDepth());
    CotEventTracker.Decision decision = eventTracker.evaluateOutbound(event);
    if (decision != CotEventTracker.Decision.ACCEPT) {
      recordDecision(decision);
      complete(completion);
      return;
    }
    recordUnsupported(event);
    byte[] marked = cotConfig.isSuppressEchoes()
        ? CotEchoSuppressor.mark(xml, echoOrigin, cotConfig.getMaximumXmlDepth())
        : xml;
    byte[] encoded = encoder.encode(marked);
    if (encoded.length > cotConfig.getMaximumEventSize()) {
      throw new IOException("Outbound CoT event exceeds maximumEventSize");
    }
    if (cotConfig.isAppendNewLine() && encoded[encoded.length - 1] != '\n') {
      encoded = Arrays.copyOf(encoded, encoded.length + 1);
      encoded[encoded.length - 1] = '\n';
    }
    CotOutboundQueue.OfferResult result = outboundQueue.offer(encoded, event, completion);
    if (result == CotOutboundQueue.OfferResult.DROPPED
        || result == CotOutboundQueue.OfferResult.CLOSED) {
      outboundOverflow.increment();
      warnControlled("outbound queue overflow");
    }
  }

  private boolean isStillValidOutbound(CotEchoSuppressor.CotEventInfo event) {
    CotEventTracker.Decision decision = eventTracker.evaluateOutbound(event);
    if (decision == CotEventTracker.Decision.ACCEPT) {
      return true;
    }
    recordDecision(decision);
    return false;
  }

  private void recordDecision(CotEventTracker.Decision decision) {
    switch (decision) {
      case DIRECT_ECHO -> directEchoes.increment();
      case SEMANTIC_ECHO -> semanticEchoes.increment();
      case HOP_LIMIT -> hopLimitEvents.increment();
      case DUPLICATE -> duplicateEvents.increment();
      case EXPIRED -> expiredEvents.increment();
      case NOT_STARTED -> notStartedEvents.increment();
      case OLDER_UPDATE -> olderEvents.increment();
      case ACCEPT -> {
        return;
      }
    }
    warnControlled(decision.name().toLowerCase());
  }

  private void recordInboundFailure(
      CotEchoSuppressor.CotEventInfo event,
      Exception exception) {
    LOGGER.log(
        COT_PROTOCOL_INBOUND_PROCESSING_FAILED,
        exception,
        endPoint.getName(),
        event.type());
  }

  private void recordUnsupported(CotEchoSuppressor.CotEventInfo event) {
    if (event.eventClass() != CotEchoSuppressor.CotEventClass.UNSUPPORTED) {
      return;
    }
    unsupportedEvents.increment();
    warnControlled("unsupported event type");
  }

  private void checkWriteProgress() {
    if (closing.get()) {
      return;
    }
    Duration timeout = Duration.ofSeconds(cotConfig.getWriteTimeoutSeconds());
    if (!outboundQueue.isWriteTimedOut(clock.instant(), timeout)) {
      return;
    }
    LOGGER.log(COT_PROTOCOL_WRITE_TIMEOUT, endPoint.getName(), cotConfig.getWriteTimeoutSeconds());
    try {
      close();
    } catch (IOException exception) {
      LOGGER.log(
          COT_PROTOCOL_OUTBOUND_SEND_FAILED,
          exception,
          endPoint.getName(),
          exception.getMessage());
    }
  }

  private void warnControlled(String reason) {
    if (!warningPending.compareAndSet(false, true)) {
      return;
    }
    LOGGER.log(COT_PROTOCOL_EVENT_REJECTED, endPoint.getName(), reason);
    SimpleTaskScheduler.getInstance().schedule(
        () -> warningPending.set(false),
        60,
        TimeUnit.SECONDS);
  }

  private static void complete(Runnable completion) {
    if (completion != null) {
      completion.run();
    }
  }

  private static void validateConfiguration(CotConfigDTO config) throws IOException {
    if (config.getMaximumEventSize() < 256) {
      throw new IOException("CoT maximumEventSize must be at least 256");
    }
    if (config.getMaximumHopCount() < 1
        || config.getFingerprintCacheSize() < 1
        || config.getFingerprintCacheTtlSeconds() < 1
        || config.getMaximumTrackedUids() < 1
        || config.getInboundQueueDepth() < 1
        || config.getOutboundQueueDepth() < 1
        || config.getWriteTimeoutSeconds() < 1) {
      throw new IOException("CoT queue, cache, hop and timeout limits must be positive");
    }
    if (config.getClockSkewSeconds() < 0) {
      throw new IOException("CoT clockSkewSeconds must not be negative");
    }
    if (config.getMaximumXmlDepth() < 4) {
      throw new IOException("CoT maximumXmlDepth must be at least 4");
    }
  }

  private static int writerChunkSize(EndPoint endPoint) throws IOException {
    long configured = endPoint.getConfig().getEndPointConfig().getServerWriteBufferSize();
    if (configured < 1 || configured > Integer.MAX_VALUE) {
      throw new IOException("CoT serverWriteBufferSize must be between 1 and "
          + Integer.MAX_VALUE);
    }
    return (int) configured;
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
