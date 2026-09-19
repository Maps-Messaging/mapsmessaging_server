/*
 *
 *  Copyright [ 2026 ] Ralf Himmelein and Claude
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

package io.mapsmessaging.state.adapter.cot;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.state.adapter.StateMessageAdapter;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.tak.CotToTwinMapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * Consumes CoT events replicated in from edge MAPS nodes over a MAPS-to-MAPS
 * {@code NetworkConnectionManager} queue bridge (an edge's own {@code TwinManagerConfig.tak.topic}
 * pushed onto this node's {@link #topic} pattern, e.g. {@code /tak/cot/inbound/<edge-name>}) and
 * routes each one through {@code TwinManager} via {@link CotToTwinMapper} - the same shared
 * MTI-aware pipeline {@code CotProtocol} (raw TLS stream ingest) uses.
 *
 * <p>Subscribes with a wildcard ({@code /tak/cot/inbound/#} by default) so the fleet of edge
 * nodes can grow without touching this node's configuration: each edge picks its own leaf topic
 * under the wildcard in its own {@code NetworkConnectionManager.yaml}, and this adapter's single
 * subscription already covers it. {@code MessageEvent.getDestinationName()} gives the exact
 * matched topic per message, so the originating edge is tagged onto the twin update without
 * maintaining a registry of edges here.
 *
 * <p>Subscribes via MAPS' own internal session API (same pattern as {@code MtiStatusAdapter})
 * rather than an external MQTT client - runs in the same JVM as the broker.
 */
public class CotIngestAdapter implements StateMessageAdapter, ClientConnection, MessageListener {

  static final String LOCAL_ARCHIVE_TOPIC = "/tak/cot/inbound";
  private static final String UPDATE_SOURCE_PREFIX = "cot-bridge-ingest";

  private final Logger logger = LoggerFactory.getLogger(CotIngestAdapter.class);
  private final CotToTwinMapper cotToTwinMapper = new CotToTwinMapper();
  private final String topic;
  private final TwinManager twinManager;

  // Metrics, exposed to Grafana via the JMX->Prometheus exporter (see CotIngestAdapterJMX).
  private final LongAdder routedCount = new LongAdder();
  private final LongAdder droppedCount = new LongAdder();
  private volatile long lastMessageAt = 0L;

  private Session session;
  private CotIngestAdapterJMX jmxBean;

  public CotIngestAdapter(String topic, TwinManager twinManager) {
    this.topic = topic;
    this.twinManager = twinManager;
  }

  @Override
  public String getName() {
    return "cot-ingest";
  }

  @Override
  public void start() {
    try {
      SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("cot-ingest-adapter", this);
      sessionContextBuilder.setUsername("anonymous")
          .setPassword("".toCharArray())
          .isInternal(true)
          .setPersistentSession(false)
          .setSessionExpiry(0)
          .setReceiveMaximum(100);
      session = SessionManager.getInstance().create(sessionContextBuilder.build(), this);
      session.addSubscription(buildSubscriptionContext());
      jmxBean = new CotIngestAdapterJMX(this);
      logger.info("CoT ingest adapter subscribed to {}", topic);
    } catch (Throwable t) {
      // Deliberately broad: StateManagerAgent.start() calls each Lifecycle's start() in an
      // unguarded loop, so an uncaught Throwable here takes down the ENTIRE state subsystem
      // (TwinManager, mavlink, N2K, everything), not just this adapter - same reasoning as
      // MtiStatusAdapter.start(). This adapter not working is a real but contained problem (no
      // edge CoT gets routed/MTI-corrected); the rest of the server has no reason to go down.
      logger.error("CoT ingest adapter failed to start on topic {} - edge CoT will not be routed", topic, t);
    }
  }

  @Override
  public void stop() {
    if (jmxBean != null) {
      jmxBean.close();
      jmxBean = null;
    }
    if (session != null) {
      try {
        SessionManager.getInstance().close(session, false);
      } catch (IOException e) {
        logger.warn("CoT ingest adapter failed to close its session cleanly", e);
      }
    }
  }

  @Override
  public void sendMessage(@NotNull MessageEvent messageEvent) {
    try {
      byte[] payload = messageEvent.getMessage().getOpaqueData();
      if (payload != null && payload.length > 0) {
        handle(messageEvent.getDestinationName(), payload);
      }
    } catch (Exception e) {
      logger.warn("CoT ingest adapter failed to process an incoming message, dropped", e);
    } finally {
      if (messageEvent.getCompletionTask() != null) {
        messageEvent.getCompletionTask().run();
      }
    }
  }

  void handle(String destinationName, byte[] xml) {
    lastMessageAt = System.currentTimeMillis();

    String updateSource = UPDATE_SOURCE_PREFIX + ":" + edgeNameFrom(destinationName);
    if (cotToTwinMapper.routeToTwinManager(twinManager, xml, updateSource)) {
      routedCount.increment();
    } else {
      droppedCount.increment();
      logger.warn("CoT event on {} had no usable uid, dropped", destinationName);
    }
  }

  /** Best-effort edge identifier: the last path segment of the matched topic. */
  private String edgeNameFrom(String destinationName) {
    if (destinationName == null || destinationName.isBlank()) {
      return "unknown";
    }
    int lastSlash = destinationName.lastIndexOf('/');
    String leaf = lastSlash >= 0 ? destinationName.substring(lastSlash + 1) : destinationName;
    return leaf.isBlank() ? "unknown" : leaf;
  }

  io.mapsmessaging.engine.destination.subscription.SubscriptionContext buildSubscriptionContext() {
    return new SubscriptionContextBuilder(topic, ClientAcknowledgement.AUTO)
        .setQos(QualityOfService.AT_LEAST_ONCE)
        .setNoLocalMessages(true)
        .build();
  }

  public boolean publishLocal(byte[] xml) {
    Session current = session;
    if (current == null || xml == null || xml.length == 0) {
      return false;
    }

    Message message = buildArchiveMessage(xml, current.getName());
    current.findDestination(LOCAL_ARCHIVE_TOPIC, DestinationType.TOPIC)
        .whenComplete((destination, throwable) -> publishArchive(destination, message, throwable));
    return true;
  }

  Message buildArchiveMessage(byte[] xml, String sessionId) {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("protocol", "CoT");
    meta.put("version", getVersion());
    meta.put("sessionId", sessionId);
    meta.put("time_ms", Long.toString(System.currentTimeMillis()));

    return new MessageBuilder()
        .setOpaqueData(xml)
        .setContentType("text/xml")
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setRetain(false)
        .setMeta(meta)
        .build();
  }

  private void publishArchive(Destination destination, Message message, Throwable throwable) {
    if (throwable != null) {
      logger.error("Failed to resolve CoT archive topic {}", LOCAL_ARCHIVE_TOPIC, throwable);
      return;
    }
    if (destination == null) {
      logger.warn("CoT archive topic {} was not available", LOCAL_ARCHIVE_TOPIC);
      return;
    }
    try {
      destination.storeMessage(message);
    } catch (IOException e) {
      logger.error("Failed to publish CoT event to {}", LOCAL_ARCHIVE_TOPIC, e);
    }
  }

  // --- Metrics, read by CotIngestAdapterJMX. ---

  public long getRoutedCount() {
    return routedCount.sum();
  }

  public long getDroppedCount() {
    return droppedCount.sum();
  }

  /** -1 if no message has ever been received on this topic. */
  public long getLastMessageAgeMillis() {
    long at = lastMessageAt;
    return at == 0L ? -1L : System.currentTimeMillis() - at;
  }

  // --- ClientConnection: this adapter has no real network endpoint of its own, it rides an
  // internal session - these are simple, honest stubs, same as MtiStatusAdapter's. ---

  @Override
  public long getTimeOut() {
    return 0;
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public void sendKeepAlive() {
  }

  @Override
  public Principal getPrincipal() {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return "";
  }

  @Override
  public String getUniqueName() {
    return "cot-ingest-adapter";
  }

  @Override
  public String getProtocolName() {
    return "internal";
  }

  @Override
  public String getRemoteIp() {
    return "";
  }
}
