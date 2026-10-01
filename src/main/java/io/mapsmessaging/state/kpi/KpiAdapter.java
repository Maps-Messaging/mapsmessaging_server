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

package io.mapsmessaging.state.kpi;

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
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.tak.ClassificationRegistry;
import io.mapsmessaging.state.drone.tak.MtiStatusRegistry;
import io.mapsmessaging.state.metrics.FeedActivityRegistry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs the IC26 KPI evaluator on the node that holds the full picture (central): a once-per-tick
 * evaluation, a JSON control topic for fault injection markers and roster changes, and the JMX
 * beans the Prometheus exporter scrapes. See {@link KpiConfig} for the configuration.
 */
public class KpiAdapter implements StateMessageAdapter, ClientConnection, MessageListener {

  private static final List<String> KNOWN_FAILURE_TYPES =
      List.of(FaultRunTracker.ALL, "FEED_LOSS", "RESTART", "CACHE_LOSS", "CLASSIFICATION_FAILURE");

  private final Logger logger = LoggerFactory.getLogger(KpiAdapter.class);
  private final KpiConfig config;
  private final TwinManager twinManager;
  private final List<KpiBean> beans = new ArrayList<>();
  private final Set<String> registeredFeeds = new HashSet<>();
  private final Set<String> registeredFailureTypes = new HashSet<>();

  private KpiEvaluator evaluator;
  private KpiEventLog eventLog;
  private ScheduledExecutorService scheduler;
  private Session session;

  public KpiAdapter(KpiConfig config, TwinManager twinManager) {
    this.config = config;
    this.twinManager = twinManager;
  }

  @Override
  public String getName() {
    return "kpi";
  }

  @Override
  public void start() {
    try {
      eventLog = new KpiEventLog(config.getDataDirectory());
      KpiEvaluator.Sources sources = new KpiEvaluator.Sources(
          this::twins,
          MtiStatusRegistry::snapshot,
          ClassificationRegistry::get,
          FeedActivityRegistry::snapshot,
          MtiStatusRegistry::clearCache);
      evaluator = new KpiEvaluator(config, sources, Clock.systemUTC(), eventLog, new KpiStateStore(config.getDataDirectory()));
      evaluator.start();
      MtiStatusRegistry.addStatusListener(evaluator);
      registerBeans();
      subscribeControlTopic();
      scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "kpi-evaluator");
        thread.setDaemon(true);
        return thread;
      });
      long interval = config.getEvaluationInterval().toMillis();
      scheduler.scheduleAtFixedRate(this::tick, interval, interval, TimeUnit.MILLISECONDS);
      logger.info("KPI evaluator started, data in {}, control topic {}", config.getDataDirectory(), config.getControlTopic());
    } catch (Throwable t) {
      // Same reasoning as MtiStatusAdapter.start(): a failure here must not take down the whole
      // state subsystem - no KPIs is a contained problem.
      logger.error("KPI evaluator failed to start - IC26 KPIs will not be evaluated", t);
    }
  }

  private Collection<KpiEvaluator.TwinInfo> twins() {
    List<KpiEvaluator.TwinInfo> twins = new ArrayList<>();
    for (EntityTwin twin : twinManager.listTwins()) {
      twins.add(new KpiEvaluator.TwinInfo(twin.getTwinId(), twin.getTwinType(), twin.getLastSeenAt()));
    }
    return twins;
  }

  private void tick() {
    try {
      evaluator.evaluate();
      registerDiscoveredBeans();
    } catch (RuntimeException e) {
      logger.warn("KPI evaluation failed", e);
    }
  }

  private void registerBeans() {
    for (KpiId kpi : KpiId.values()) {
      beans.add(new KpiJMX(evaluator, kpi));
    }
    beans.add(new KpiPictureJMX(evaluator));
    for (CacheRebuildTracker.Trigger trigger : CacheRebuildTracker.Trigger.values()) {
      for (CacheRebuildTracker.Target target : CacheRebuildTracker.Target.values()) {
        beans.add(new DurationJMX(List.of("type=Integration", "name=CacheRebuild",
            "trigger=" + trigger.name(), "target=" + target.name()),
            evaluator.getRebuilds().stats(trigger, target)));
      }
    }
    for (String failureType : KNOWN_FAILURE_TYPES) {
      registerFailureType(failureType);
    }
  }

  /** Feeds and ad-hoc failure types only become known at runtime. */
  private void registerDiscoveredBeans() {
    for (FeedOutageMonitor.FeedStats feed : evaluator.getFeedMonitor().getFeeds()) {
      if (registeredFeeds.add(feed.getFeed())) {
        beans.add(new FeedOutageJMX(feed, evaluator.getClock()));
      }
    }
    for (String failureType : evaluator.getRuns().getStats().keySet()) {
      registerFailureType(failureType);
    }
  }

  private void registerFailureType(String failureType) {
    if (!registeredFailureTypes.add(failureType)) {
      return;
    }
    FaultRunTracker.RunTypeStats stats = evaluator.getRuns().statsFor(failureType);
    beans.add(new FaultRunsJMX(failureType, stats));
    for (FaultRunTracker.Stage stage : FaultRunTracker.Stage.values()) {
      beans.add(new DurationJMX(List.of("type=Integration", "name=FaultRun",
          "failureType=" + FeedOutageJMX.sanitise(failureType), "stage=" + stage.name()), stats.stage(stage)));
    }
  }

  private void subscribeControlTopic() throws IOException, LoginException {
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("kpi-adapter", this);
    sessionContextBuilder.setUsername("anonymous")
        .setPassword("".toCharArray())
        .isInternal(true)
        .setPersistentSession(false)
        .setSessionExpiry(0)
        .setReceiveMaximum(100);
    session = SessionManager.getInstance().create(sessionContextBuilder.build(), this);
    session.addSubscription(new SubscriptionContextBuilder(config.getControlTopic(), ClientAcknowledgement.AUTO)
        .setQos(QualityOfService.AT_LEAST_ONCE)
        .build());
  }

  @Override
  public void stop() {
    if (scheduler != null) {
      scheduler.shutdownNow();
      scheduler = null;
    }
    if (evaluator != null) {
      MtiStatusRegistry.removeStatusListener(evaluator);
      evaluator.stop();
    }
    for (KpiBean bean : beans) {
      bean.close();
    }
    beans.clear();
    registeredFeeds.clear();
    registeredFailureTypes.clear();
    if (session != null) {
      try {
        SessionManager.getInstance().close(session, false);
      } catch (IOException e) {
        logger.warn("KPI adapter failed to close its session cleanly", e);
      }
      session = null;
    }
    if (eventLog != null) {
      eventLog.close();
    }
  }

  @Override
  public void sendMessage(@NotNull MessageEvent messageEvent) {
    try {
      byte[] payload = messageEvent.getMessage().getOpaqueData();
      if (payload != null && payload.length > 0 && evaluator != null) {
        evaluator.handleControl(new String(payload, StandardCharsets.UTF_8));
      }
    } catch (Exception e) {
      logger.warn("KPI adapter failed to process a control message, dropped", e);
    } finally {
      if (messageEvent.getCompletionTask() != null) {
        messageEvent.getCompletionTask().run();
      }
    }
  }

  // --- ClientConnection: internal session only, same stubs as MtiStatusAdapter. ---

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
    return "kpi-adapter";
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
