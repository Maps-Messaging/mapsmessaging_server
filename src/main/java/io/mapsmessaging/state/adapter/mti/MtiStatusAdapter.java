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

package io.mapsmessaging.state.adapter.mti;

import com.google.gson.Gson;
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
import io.mapsmessaging.state.drone.tak.MtiLookupResult;
import io.mapsmessaging.state.drone.tak.MtiStatusRegistry;
import io.mapsmessaging.utilities.GsonFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * Consumes the MTI server's {@code mti.asset.health/v1} feed on one configured MQTT topic (their
 * spec default: {@code /tak/cot} - but see the topic-collision note in this repo's README, most
 * deployments will want a different value) and keeps a live {@code uid -> MtiStatus} cache,
 * registering itself with {@code MtiStatusRegistry} so {@code CotEventPolicy} can look twins up
 * by twinId at CoT-composition time.
 *
 * <p>Subscribes via MAPS' own internal session API (same pattern as
 * {@code state.drone.tak.EventPublisher}, just subscribing instead of publishing) rather than an
 * external MQTT client - runs in the same JVM as the broker, so there's no reason to pay for a
 * network hop or need to know the broker's own listener port.
 *
 * <p>Important: this is ONE shared topic, not one topic per asset - the broker only ever retains
 * the single most-recently-published message on it, not one per uid. This cache is built entirely
 * from consuming the live stream (upsert on op=new/update, remove on op=delete); it cannot recover
 * full multi-asset state from retained-message replay after its own restart.
 */
public class MtiStatusAdapter implements StateMessageAdapter, ClientConnection, MessageListener {

  private static final int COLOR_ARGB_MITIGATE = -23296; // 0xFFFFA500, opaque orange
  private static final int COLOR_ARGB_HOLD = -65536;     // 0xFFFF0000, opaque red

  private final Logger logger = LoggerFactory.getLogger(MtiStatusAdapter.class);
  private final Gson gson = GsonFactory.getInstance().getSimpleGson();
  private final Map<String, MtiStatus> cache = new ConcurrentHashMap<>();
  private final String topic;

  // Metrics, exposed to Grafana via the JMX->Prometheus exporter (see MtiStatusAdapterJMX).
  private final LongAdder upsertCount = new LongAdder();
  private final LongAdder deleteCount = new LongAdder();
  private final LongAdder lookupHitCount = new LongAdder();
  private final LongAdder lookupMissCount = new LongAdder();
  private volatile long lastMessageAt = 0L;

  private Session session;
  private MtiStatusAdapterJMX jmxBean;

  public MtiStatusAdapter(String topic) {
    this.topic = topic;
  }

  @Override
  public String getName() {
    return "mti-status";
  }

  @Override
  public void start() {
    try {
      SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("mti-status-adapter", this);
      sessionContextBuilder.setUsername("anonymous")
          .setPassword("".toCharArray())
          .isInternal(true)
          .setPersistentSession(false)
          .setSessionExpiry(0)
          .setReceiveMaximum(100);
      session = SessionManager.getInstance().create(sessionContextBuilder.build(), this);
      session.addSubscription(new SubscriptionContextBuilder(topic, ClientAcknowledgement.AUTO)
          .setQos(QualityOfService.AT_LEAST_ONCE)
          .build());
      MtiStatusRegistry.setDelegate(this::lookup);
      jmxBean = new MtiStatusAdapterJMX(this);
      logger.info("MTI status adapter subscribed to {}", topic);
    } catch (Throwable t) {
      // Deliberately broad: StateManagerAgent.start() calls each Lifecycle's start() in an
      // unguarded loop, so an uncaught Throwable here - not just the checked exceptions below -
      // takes down the ENTIRE state subsystem (TwinManager, mavlink, N2K, everything), not just
      // this adapter. Confirmed the hard way: a classpath mismatch threw NoClassDefFoundError
      // (an Error, not an Exception) here and crash-looped the whole maps-messaging container.
      // This adapter not working is a real but contained problem (no MTI status applied); the
      // rest of the server has no reason to go down with it.
      logger.error("MTI status adapter failed to start on topic {} - MTI status will not be applied", topic, t);
    }
  }

  @Override
  public void stop() {
    MtiStatusRegistry.setDelegate(null);
    if (jmxBean != null) {
      jmxBean.close();
      jmxBean = null;
    }
    if (session != null) {
      try {
        SessionManager.getInstance().close(session, false);
      } catch (IOException e) {
        logger.warn("MTI status adapter failed to close its session cleanly", e);
      }
    }
  }

  @Override
  public void sendMessage(@NotNull MessageEvent messageEvent) {
    try {
      byte[] payload = messageEvent.getMessage().getOpaqueData();
      if (payload != null && payload.length > 0) {
        handle(new String(payload, StandardCharsets.UTF_8));
      }
    } catch (Exception e) {
      logger.warn("MTI status adapter failed to process an incoming message, dropped", e);
    } finally {
      if (messageEvent.getCompletionTask() != null) {
        messageEvent.getCompletionTask().run();
      }
    }
  }

  void handle(String json) {
    MtiWireMessage message = gson.fromJson(json, MtiWireMessage.class);
    if (message == null || message.uid() == null || message.uid().isBlank()) {
      logger.warn("MTI status message had no uid, dropped");
      return;
    }

    lastMessageAt = System.currentTimeMillis();
    Instant observedAt = MtiStatus.parseTimestamp(message.observedAt());
    if (observedAt == null) {
      logger.warn("MTI status message for {} had an invalid observed_at, dropped", message.uid());
      return;
    }

    if ("delete".equalsIgnoreCase(message.op())) {
      AtomicBoolean accepted = new AtomicBoolean();
      cache.compute(message.uid(), (uid, current) -> {
        if (current == null || !observedAt.isBefore(current.observedAt())) {
          accepted.set(true);
          return null;
        }
        return current;
      });
      deleteCount.increment();
      if (accepted.get()) {
        logger.debug("MTI status cleared for {}", message.uid());
      } else {
        logger.debug("Ignored out-of-order MTI delete for {}", message.uid());
      }
      return;
    }

    MtiStatus incoming = MtiStatus.from(message);
    if (incoming == null) {
      logger.warn("MTI status message for {} had invalid validity timestamps, dropped", message.uid());
      return;
    }

    Instant now = Instant.now();
    AtomicBoolean accepted = new AtomicBoolean();
    cache.compute(message.uid(), (uid, current) -> {
      if (current != null && incoming.observedAt().isBefore(current.observedAt())) {
        return current;
      }
      accepted.set(true);
      return incoming.isExpired(now) ? null : incoming;
    });
    upsertCount.increment();

    if (accepted.get()) {
      if (incoming.isExpired(now)) {
        logger.debug("MTI status for {} was already expired and cleared any older cached status", message.uid());
      } else {
        logger.debug("MTI status updated for {}: state={}", message.uid(), message.state());
      }
    } else {
      logger.debug("Ignored out-of-order MTI status update for {}", message.uid());
    }
  }

  /** Called by {@code CotEventPolicy} via {@code MtiStatusRegistry}, keyed on twinId. */
  MtiLookupResult lookup(String twinId) {
    MtiStatus status = cache.get(twinId);
    if (status == null) {
      lookupMissCount.increment();
      return null;
    }

    Instant now = Instant.now();
    if (status.isExpired(now)) {
      cache.remove(twinId, status);
      lookupMissCount.increment();
      return null;
    }

    lookupHitCount.increment();
    if (status.state() == null) {
      return null;
    }

    return switch (status.state().toLowerCase(Locale.ROOT)) {
      // Semantically exact: MTI genuinely can't assess this asset, so "unknown" affiliation is
      // both visually and factually correct - not just a convenient colour choice. No cyber
      // icon here - "we can't tell" isn't the same signal as "we can tell, and it's bad".
      case "unknown" -> new MtiLookupResult("u", null, status.remarks(), null, null);
      // mitigate/hold deliberately do NOT change affiliation - a friendly asset with a
      // technical/trust fault must never render as unknown/suspect/hostile. Severity is
      // communicated through marker colour + remarks + readiness=false, not identity.
      // Field-tested 2026-09-15: WebTAK doesn't visibly render colorArgb for the vehicle icons
      // this deployment uses, so readiness=false carries the actual glanceable signal here -
      // colorArgb is kept for clients/icon types that do respect it.
      //
      // cyberIconFile: a fixed two-level severity split (not a 1:1 map of the ddos1-5 gradient
      // in the shared iconset) - mitigate is "degraded but still trusted enough to act",
      // hold is "trust withdrawn", so hold gets the more severe icon. Ddos2/4/5 and exploit are
      // unused for now - there's no MTI wire field (domain/finding count, signal_id) confirmed
      // to correlate with them yet; revisit if/when the MTI team specifies one.
      case "mitigate" -> new MtiLookupResult(null, COLOR_ARGB_MITIGATE, status.remarks(), false, "ddos3_64x64.png");
      case "hold" -> new MtiLookupResult(null, COLOR_ARGB_HOLD, status.remarks(), false, "ddos1_64x64.png");
      // "go", or anything not in the MTI team's fixed 4-value alphabet - no override.
      default -> null;
    };
  }

  // --- Metrics, read by MtiStatusAdapterJMX. ---

  public int getCacheSize() {
    pruneExpired();
    return cache.size();
  }

  public long getUpsertCount() {
    return upsertCount.sum();
  }

  public long getDeleteCount() {
    return deleteCount.sum();
  }

  public long getLookupHitCount() {
    return lookupHitCount.sum();
  }

  public long getLookupMissCount() {
    return lookupMissCount.sum();
  }

  /** -1 if no message has ever been received on this topic. */
  public long getLastMessageAgeMillis() {
    long at = lastMessageAt;
    return at == 0L ? -1L : System.currentTimeMillis() - at;
  }

  /** Number of cached assets currently reporting MTI state mitigate/hold. */
  public int getDegradedAssetCount() {
    pruneExpired();
    int count = 0;
    for (MtiStatus status : cache.values()) {
      String state = status.state();
      if (state != null) {
        String normalised = state.toLowerCase(Locale.ROOT);
        if (normalised.equals("mitigate") || normalised.equals("hold")) {
          count++;
        }
      }
    }
    return count;
  }

  /**
   * Fraction (0.0-1.0) of the MTI-covered fleet NOT currently flagged mitigate/hold. Scoped to
   * assets this feed has an opinion on - a twin with no MTI status entry is implicitly assumed
   * nominal (same convention {@link #lookup} already uses), not counted against readiness.
   * Vacuously 1.0 (fully ready) when the cache is empty.
   */
  public double getReadinessRate() {
    pruneExpired();
    int total = cache.size();
    return total == 0 ? 1.0d : 1.0d - ((double) getDegradedAssetCount() / total);
  }

  private void pruneExpired() {
    Instant now = Instant.now();
    cache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
  }

  // --- ClientConnection: this adapter has no real network endpoint of its own, it rides an
  // internal session - these are simple, honest stubs, same as EventPublisher's. ---

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
    return "mti-status-adapter";
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
