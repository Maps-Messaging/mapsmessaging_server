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

package io.mapsmessaging.state.mavlink.bootstrap;

import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MavlinkBootstrapStateEngine {

  private final MavlinkBootstrapProfile bootstrapProfile;
  private final Map<String, MavlinkBootstrapState> bootstrapStates;

  public MavlinkBootstrapStateEngine(MavlinkBootstrapProfile bootstrapProfile) {
    this.bootstrapProfile = bootstrapProfile;
    this.bootstrapStates = new ConcurrentHashMap<>();
  }

  public synchronized List<MavlinkBootstrapEvent> update(DroneTwin droneTwin, DroneTwinReadinessResult readinessResult, TwinUpdateContext context) {
    List<MavlinkBootstrapEvent> events = new ArrayList<>();

    if (droneTwin == null || readinessResult == null) {
      return events;
    }

    String twinId = droneTwin.getTwinId();
    if (twinId == null) {
      return events;
    }
    MavlinkBootstrapState bootstrapState = bootstrapStates.computeIfAbsent(twinId, MavlinkBootstrapState::new);
    Instant now = getNow(context);

    resetResolvedTrackers(bootstrapState, readinessResult.getMissingStates());
    emitReadinessChangeIfRequired(bootstrapState, readinessResult, events);
    bootstrapState.setUpdatedAt(now);

    boolean actionableMissingState = hasActionableMissingState(readinessResult.getMissingStates());
    if (readinessResult.isCommandReady() && !actionableMissingState) {
      markCompletedIfRequired(bootstrapState, readinessResult, events);
      return events;
    }

    if (actionableMissingState && bootstrapState.isCompleted()) {
      bootstrapState.setCompleted(false);
    }

    if (bootstrapState.isFailed()) {
      return events;
    }

    if (droneTwin.getSystemId() == null || droneTwin.getComponentId() == null) {
      return events;
    }

    handleMissingStates(droneTwin, readinessResult, bootstrapState, now, events);
    return events;
  }

  public void remove(String twinId) {
    if (twinId != null) {
      bootstrapStates.remove(twinId);
    }
  }

  private boolean hasActionableMissingState(Set<DroneTwinMissingState> missingStates) {
    if (missingStates == null || missingStates.isEmpty()) {
      return false;
    }

    for (DroneTwinMissingState missingState : missingStates) {
      if (bootstrapProfile.getRequestDefinitions().containsKey(missingState)) {
        return true;
      }
    }
    return false;
  }

  private void resetResolvedTrackers(
      MavlinkBootstrapState bootstrapState,
      Set<DroneTwinMissingState> missingStates
  ) {
    Map<DroneTwinMissingState, MavlinkBootstrapRequestTracker> requestTrackers = bootstrapState.getRequestTrackers();
    if (requestTrackers == null || requestTrackers.isEmpty() || missingStates == null) {
      return;
    }

    requestTrackers.keySet().removeIf(missingState -> !missingStates.contains(missingState));
  }

  private void emitReadinessChangeIfRequired(
      MavlinkBootstrapState bootstrapState,
      DroneTwinReadinessResult readinessResult,
      List<MavlinkBootstrapEvent> events
  ) {
    DroneTwinReadinessState previousReadinessState = bootstrapState.getReadinessState();
    DroneTwinReadinessState currentReadinessState = readinessResult.getReadinessState();

    if (currentReadinessState == null) {
      currentReadinessState = DroneTwinReadinessState.UNKNOWN;
    }

    if (previousReadinessState != currentReadinessState) {
      events.add(
          MavlinkBootstrapEvent.readinessChanged(
              bootstrapState.getTwinId(),
              previousReadinessState,
              currentReadinessState
          )
      );
      bootstrapState.setReadinessState(currentReadinessState);
    }
  }

  private void markCompletedIfRequired(
      MavlinkBootstrapState bootstrapState,
      DroneTwinReadinessResult readinessResult,
      List<MavlinkBootstrapEvent> events
  ) {
    if (bootstrapState.isCompleted()) {
      return;
    }

    bootstrapState.setCompleted(true);

    events.add(
        MavlinkBootstrapEvent.completed(
            bootstrapState.getTwinId(),
            readinessResult.getReadinessState()
        )
    );
  }

  private void handleMissingStates(
      DroneTwin droneTwin,
      DroneTwinReadinessResult readinessResult,
      MavlinkBootstrapState bootstrapState,
      Instant now,
      List<MavlinkBootstrapEvent> events
  ) {
    if (readinessResult.getMissingStates() == null) {
      return;
    }

    Set<MavlinkBootstrapRequestDefinition> emittedRequests = new HashSet<>();
    for (DroneTwinMissingState missingState : readinessResult.getMissingStates()) {
      MavlinkBootstrapRequestDefinition requestDefinition =
          bootstrapProfile.getRequestDefinitions().get(missingState);

      if (requestDefinition == null) {
        continue;
      }

      MavlinkBootstrapRequestTracker requestTracker =
          bootstrapState.getOrCreateTracker(missingState);
      boolean taskBlocking = isTaskBlocking(readinessResult, missingState);

      if (!canRetry(requestTracker, taskBlocking, bootstrapState, now, events)) {
        continue;
      }

      if (emittedRequests.add(requestDefinition)) {
        events.add(
            createRequestEvent(
                droneTwin,
                missingState,
                requestDefinition
            )
        );
      }

      requestTracker.markRequested(now);
    }
  }

  private boolean canRetry(
      MavlinkBootstrapRequestTracker requestTracker,
      boolean taskBlocking,
      MavlinkBootstrapState bootstrapState,
      Instant now,
      List<MavlinkBootstrapEvent> events
  ) {
    if (!taskBlocking) {
      return retryDue(requestTracker, now, bootstrapProfile.getBackgroundRetryInterval());
    }

    if (requestTracker.getRequestCount() < bootstrapProfile.getMaximumRetries()) {
      return retryDue(requestTracker, now, bootstrapProfile.getRetryInterval());
    }

    if (!requestTracker.hasTimedOut(now, bootstrapProfile.getTimeout())) {
      return false;
    }

    if (!requestTracker.isTimedOut()) {
      requestTracker.setTimedOut(true);
      events.add(
          MavlinkBootstrapEvent.timedOut(
              bootstrapState.getTwinId(),
              requestTracker.getMissingState(),
              "Timed out waiting for " + requestTracker.getMissingState()
          )
      );
    }

    return retryDue(requestTracker, now, bootstrapProfile.getRecoveryRetryInterval());
  }

  private boolean retryDue(
      MavlinkBootstrapRequestTracker requestTracker,
      Instant now,
      Duration interval
  ) {
    Instant lastRequestedAt = requestTracker.getLastRequestedAt();
    if (lastRequestedAt == null) {
      return true;
    }
    return Duration.between(lastRequestedAt, now).compareTo(interval) >= 0;
  }

  private boolean isTaskBlocking(
      DroneTwinReadinessResult readinessResult,
      DroneTwinMissingState missingState
  ) {
    if (readinessResult.getBlockingStates() != null
        && readinessResult.getBlockingStates().contains(missingState)) {
      return true;
    }

    return missingState == DroneTwinMissingState.MISSING_BATTERY_STATE
        || missingState == DroneTwinMissingState.STALE_POWER;
  }

  private MavlinkBootstrapEvent createRequestEvent(
      DroneTwin droneTwin,
      DroneTwinMissingState missingState,
      MavlinkBootstrapRequestDefinition requestDefinition
  ) {
    int targetSystem = droneTwin.getSystemId();
    int targetComponent = droneTwin.getComponentId();

    if (requestDefinition.getRequestType() == MavlinkBootstrapRequestType.REQUEST_MESSAGE) {
      return MavlinkBootstrapEvent.requestMessage(
          droneTwin.getTwinId(),
          targetSystem,
          targetComponent,
          missingState,
          requestDefinition.getMavlinkMessageId()
      );
    }

    return MavlinkBootstrapEvent.setMessageInterval(
        droneTwin.getTwinId(),
        targetSystem,
        targetComponent,
        missingState,
        requestDefinition.getMavlinkMessageId(),
        requestDefinition.getIntervalMicroseconds()
    );
  }

  private Instant getNow(TwinUpdateContext context) {
    if (context != null && context.getReceivedTime() != null) {
      return context.getReceivedTime();
    }
    return Instant.now();
  }
}
