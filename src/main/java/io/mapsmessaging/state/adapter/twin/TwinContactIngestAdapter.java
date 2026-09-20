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

package io.mapsmessaging.state.adapter.twin;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.state.adapter.StateMessageAdapter;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.concurrent.atomic.LongAdder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Raises the detections an edge node relays, on the node that can draw them.
 *
 * <p>A node that owns a vehicle publishes the contacts it detects to
 * {@code /state/twins/<id>/contacts}, and an aggregator receives those documents verbatim over the
 * node's outbound bridge. The aggregator holds the TAK connection, so it has to raise the
 * detection itself: this adapter subscribes to the relayed leaves and hands each document to
 * {@link TwinContactMapper}, which attaches it to the twin the aggregator already shows.
 *
 * <p>Subscribed with {@code noLocal}, because this node publishes its own twins onto the same
 * tree -- without it the adapter would read its own output back.
 */
public class TwinContactIngestAdapter implements StateMessageAdapter, ClientConnection, MessageListener {

  private static final String UPDATE_SOURCE = "twin-contact-ingest";

  private final Logger logger = LoggerFactory.getLogger(TwinContactIngestAdapter.class);
  private final TwinContactMapper mapper;
  private final String topic;

  private final LongAdder raisedCount = new LongAdder();
  private final LongAdder droppedCount = new LongAdder();

  private Session session;

  public TwinContactIngestAdapter(String topic, TwinManager twinManager) {
    this.topic = topic;
    this.mapper = new TwinContactMapper(twinManager);
  }

  @Override
  public String getName() {
    return "twin-contact-ingest";
  }

  @Override
  public void start() {
    try {
      SessionContextBuilder sessionContextBuilder = new SessionContextBuilder(sessionId(), this);
      sessionContextBuilder.setUsername("anonymous")
          .setPassword("".toCharArray())
          .isInternal(true)
          .setPersistentSession(false)
          .setSessionExpiry(0)
          .setReceiveMaximum(100);
      session = SessionManager.getInstance().create(sessionContextBuilder.build(), this);
      session.addSubscription(new SubscriptionContextBuilder(topic, ClientAcknowledgement.AUTO)
          .setQos(QualityOfService.AT_LEAST_ONCE)
          .setNoLocalMessages(true)
          .build());
      logger.info("Twin contact ingest adapter subscribed to {}", topic);
    } catch (Throwable t) {
      closeSessionQuietly();
      // Deliberately broad, as in the CoT ingest adapter: StateManagerAgent.start() runs each
      // Lifecycle's start() in an unguarded loop, so an uncaught Throwable here would take the
      // whole state subsystem down. This adapter failing costs relayed detections, nothing else.
      logger.error("Twin contact ingest adapter failed to start on topic {} - relayed detections "
          + "will not be drawn", topic, t);
    }
  }

  @Override
  public void stop() {
    closeSessionQuietly();
  }

  private void closeSessionQuietly() {
    Session current = session;
    session = null;
    if (current != null) {
      try {
        SessionManager.getInstance().close(current, false);
      } catch (IOException e) {
        logger.warn("Twin contact ingest adapter failed to close its session cleanly", e);
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
      logger.warn("Twin contact ingest adapter failed to process an incoming message, dropped", e);
    } finally {
      if (messageEvent.getCompletionTask() != null) {
        messageEvent.getCompletionTask().run();
      }
    }
  }

  void handle(String destinationName, byte[] document) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setUpdateSource(UPDATE_SOURCE);
    context.setSourceNamespace(destinationName);

    if (mapper.ingest(new String(document, StandardCharsets.UTF_8), context)) {
      raisedCount.increment();
    } else {
      // Most documents on this tree carry no contact at all: a twin publishes its contacts leaf
      // whenever its state changes. Not worth a log line each time.
      droppedCount.increment();
    }
  }

  public long getRaisedCount() {
    return raisedCount.sum();
  }

  public long getDroppedCount() {
    return droppedCount.sum();
  }

  String sessionId() {
    return "twin-contact-ingest-adapter:" + MessageDaemon.getInstance().getId();
  }

  // --- ClientConnection: no network endpoint of its own, it rides an internal session ---

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
    return "twin-contact-ingest-adapter";
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
