/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.route.select;


import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.route.link.LinkId;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.mapsmessaging.logging.ServerLogMessages.EXCEPTION_DURING_EVALUATION;

/**
 * Thin layer that coalesces link events and invokes LinkSelector.evaluateOnce().
 * Supports optional periodic scans without changing LinkSelector.
 */
@RequiredArgsConstructor
public final class SelectionOrchestrator implements AutoCloseable {

  private final Logger logger = LoggerFactory.getLogger(SelectionOrchestrator.class);

  private final LinkSelector linkSelector;
  private final SelectionOrchestratorConfig orchestratorConfig;

  private final Set<LinkId> dirtyLinkIds = ConcurrentHashMap.newKeySet();
  private final AtomicBoolean coalesceScheduled = new AtomicBoolean(false);

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread thread = new Thread(r, "selection-orchestrator");
    thread.setDaemon(true);
    return thread;
  });

  public static SelectionOrchestrator start(
      LinkSelector linkSelector,
      SelectionOrchestratorConfig orchestratorConfig
  ) {
    Objects.requireNonNull(linkSelector);
    Objects.requireNonNull(orchestratorConfig);
    SelectionOrchestrator orchestrator = new SelectionOrchestrator(linkSelector, orchestratorConfig);
    orchestrator.startInternal();
    return orchestrator;
  }

  public void onLinkStateChanged(LinkStateChangedEvent linkStateChangedEvent) {
    if (linkStateChangedEvent == null) return;
    dirtyLinkIds.add(linkStateChangedEvent.linkId());
    scheduleCoalescedEvaluation();
  }

  public void onLinkMetricsUpdated(LinkMetricsUpdatedEvent linkMetricsUpdatedEvent) {
    if (linkMetricsUpdatedEvent == null) return;
    dirtyLinkIds.add(linkMetricsUpdatedEvent.linkId());
    scheduleCoalescedEvaluation();
  }

  @Override
  public void close() {
    scheduler.shutdownNow();
  }

  private void startInternal() {
    if (orchestratorConfig.isEnablePeriodicScan()) {
      Duration interval = orchestratorConfig.getPeriodicScanInterval();
      scheduler.scheduleAtFixedRate(
          this::safeEvaluateOnce,
          interval.toMillis(),
          interval.toMillis(),
          TimeUnit.MILLISECONDS
      );
    }
  }

  private void scheduleCoalescedEvaluation() {
    if (coalesceScheduled.compareAndSet(false, true)) {
      long delayMillis = orchestratorConfig.getMinInterEventInterval().toMillis();
      scheduler.schedule(this::flushCoalesced, delayMillis, TimeUnit.MILLISECONDS);
    }
  }

  private void flushCoalesced() {
    try {
      if (dirtyLinkIds.isEmpty()) return;
      dirtyLinkIds.clear();
      safeEvaluateOnce();
    } finally {
      coalesceScheduled.set(false);
      if (!dirtyLinkIds.isEmpty()) {
        scheduleCoalescedEvaluation();
      }
    }
  }

  private void safeEvaluateOnce() {
    try {
      linkSelector.evaluateOnce();
    } catch (Exception exception) {
      logger.log(EXCEPTION_DURING_EVALUATION, exception);
    }
  }
}
