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

import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.bootstrap.checks.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public class DroneTwinReadinessEvaluator {

  private final List<DroneTwinReadinessCheck> readinessChecks;

  public DroneTwinReadinessEvaluator() {
    this(
        List.of(
            new IdentityReadinessCheck(),
            new AutopilotReadinessCheck(),
            new ConnectivityReadinessCheck(Duration.ofSeconds(10)),
            new GlobalPositionReadinessCheck(Duration.ofSeconds(10)),
            new GpsReadinessCheck(),
            new HomePositionReadinessCheck(),
            new BatteryReadinessCheck(Duration.ofSeconds(30)),
            new SystemStateReadinessCheck(),
            new CapabilitiesReadinessCheck(),
            new LifecycleReadinessCheck()
        )
    );
  }

  public DroneTwinReadinessEvaluator(List<DroneTwinReadinessCheck> readinessChecks) {
    this.readinessChecks = readinessChecks;
  }

  public DroneTwinReadinessResult evaluate(
      DroneTwin droneTwin,
      TwinUpdateContext context
  ) {
    Instant now = getNow(context);

    DroneTwinReadinessEvaluation evaluation =
        new DroneTwinReadinessEvaluation(now);

    if (droneTwin == null) {
      DroneTwinReadinessResult readinessResult = new DroneTwinReadinessResult();
      readinessResult.setReadinessState(DroneTwinReadinessState.UNKNOWN);
      readinessResult.setRegistrationReady(false);
      readinessResult.setCommandReady(false);
      readinessResult.setMissingStates(evaluation.getMissingStates());
      readinessResult.setDegradedStates(evaluation.getDegradedStates());
      readinessResult.setBlockingStates(evaluation.getBlockingStates());
      readinessResult.setEvaluatedAt(now);
      return readinessResult;
    }

    for (DroneTwinReadinessCheck readinessCheck : readinessChecks) {
      readinessCheck.evaluate(droneTwin, evaluation);
    }

    boolean registrationReady = isRegistrationReady(
        evaluation.getMissingStates(),
        evaluation.getBlockingStates()
    );

    boolean commandReady = isCommandReady(
        evaluation.getMissingStates(),
        evaluation.getBlockingStates()
    );

    DroneTwinReadinessResult readinessResult =
        new DroneTwinReadinessResult(droneTwin.getTwinId());

    readinessResult.setReadinessState(
        determineReadinessState(
            droneTwin,
            evaluation.getMissingStates(),
            evaluation.getBlockingStates(),
            registrationReady,
            commandReady
        )
    );

    readinessResult.setRegistrationReady(registrationReady);
    readinessResult.setCommandReady(commandReady);
    readinessResult.setMissingStates(evaluation.getMissingStates());
    readinessResult.setDegradedStates(evaluation.getDegradedStates());
    readinessResult.setBlockingStates(evaluation.getBlockingStates());
    readinessResult.setEvaluatedAt(now);

    return readinessResult;
  }

  private boolean isRegistrationReady(
      Set<DroneTwinMissingState> missingStates,
      Set<DroneTwinMissingState> blockingStates
  ) {
    if (blockingStates.contains(DroneTwinMissingState.MISSING_SYSTEM_ID)) {
      return false;
    }

    if (blockingStates.contains(DroneTwinMissingState.MISSING_COMPONENT_ID)) {
      return false;
    }

    if (blockingStates.contains(DroneTwinMissingState.MISSING_VEHICLE_CLASS)) {
      return false;
    }

    if (blockingStates.contains(DroneTwinMissingState.STALE_HEARTBEAT)) {
      return false;
    }

    return !missingStates.contains(DroneTwinMissingState.MISSING_AUTOPILOT_TYPE);
  }

  private boolean isCommandReady(
      Set<DroneTwinMissingState> missingStates,
      Set<DroneTwinMissingState> blockingStates
  ) {
    if (!isRegistrationReady(missingStates, blockingStates)) {
      return false;
    }

    if (blockingStates.contains(DroneTwinMissingState.MISSING_GLOBAL_POSITION)) {
      return false;
    }

    if (blockingStates.contains(DroneTwinMissingState.MISSING_GPS_FIX)) {
      return false;
    }

    if (blockingStates.contains(DroneTwinMissingState.STALE_POSITION)) {
      return false;
    }

    if (missingStates.contains(DroneTwinMissingState.MISSING_BATTERY_STATE)) {
      return false;
    }

    return !missingStates.contains(DroneTwinMissingState.MISSING_CAPABILITIES);
  }

  private DroneTwinReadinessState determineReadinessState(
      DroneTwin droneTwin,
      Set<DroneTwinMissingState> missingStates,
      Set<DroneTwinMissingState> blockingStates,
      boolean registrationReady,
      boolean commandReady
  ) {
    if (blockingStates.contains(DroneTwinMissingState.STALE_HEARTBEAT)
        || droneTwin.getLifecycleStatus() == TwinLifecycleStatus.STALE) {
      return DroneTwinReadinessState.STALE;
    }

    if (commandReady) {
      return DroneTwinReadinessState.COMMAND_READY;
    }

    if (registrationReady) {
      if (missingStates.contains(DroneTwinMissingState.MISSING_GLOBAL_POSITION)
          || missingStates.contains(DroneTwinMissingState.MISSING_GPS_FIX)
          || missingStates.contains(DroneTwinMissingState.STALE_POSITION)) {
        return DroneTwinReadinessState.POSITION_PARTIAL;
      }

      if (missingStates.contains(DroneTwinMissingState.MISSING_CAPABILITIES)) {
        return DroneTwinReadinessState.CAPABILITY_PARTIAL;
      }

      if (missingStates.contains(DroneTwinMissingState.MISSING_SYSTEM_STATE)
          || missingStates.contains(DroneTwinMissingState.MISSING_BATTERY_STATE)
          || missingStates.contains(DroneTwinMissingState.STALE_POWER)) {
        return DroneTwinReadinessState.HEALTH_PARTIAL;
      }

      return DroneTwinReadinessState.REGISTRATION_READY;
    }

    if (!missingStates.contains(DroneTwinMissingState.MISSING_GLOBAL_POSITION)
        && !missingStates.contains(DroneTwinMissingState.MISSING_GPS_FIX)) {
      return DroneTwinReadinessState.POSITIONED;
    }

    if (!missingStates.contains(DroneTwinMissingState.MISSING_SYSTEM_ID)
        && !missingStates.contains(DroneTwinMissingState.MISSING_COMPONENT_ID)
        && !missingStates.contains(DroneTwinMissingState.MISSING_VEHICLE_CLASS)) {
      return DroneTwinReadinessState.IDENTIFIED;
    }

    if (droneTwin.getTwinId() != null) {
      return DroneTwinReadinessState.DISCOVERED;
    }

    return DroneTwinReadinessState.UNKNOWN;
  }

  private Instant getNow(TwinUpdateContext context) {
    if (context != null && context.getReceivedTime() != null) {
      return context.getReceivedTime();
    }

    return Instant.now();
  }
}