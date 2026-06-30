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

package io.mapsmessaging.state.mavlink;

import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinRelationship;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinMissingState;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinReadinessEvaluator;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinReadinessResult;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapEvent;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapEventPublisher;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapStateEngine;

import java.util.List;
import java.util.Set;

/**
 * Observes MAVLink-backed drone twins and drives MAVLink bootstrap/readiness evaluation.
 *
 * <p>This class does not send MAVLink directly. It evaluates the current twin state,
 * updates readiness fields on the twin, and publishes bootstrap events for another
 * component to translate into MAVLink commands.</p>
 */
public class MavlinkDroneMonitor implements TwinObserver {

  private final TwinManager twinManager;
  private final DroneTwinReadinessEvaluator readinessEvaluator;
  private final MavlinkBootstrapStateEngine bootstrapStateEngine;
  private final MavlinkBootstrapEventPublisher bootstrapEventPublisher;

  public MavlinkDroneMonitor(
      TwinManager twinManager,
      DroneTwinReadinessEvaluator readinessEvaluator,
      MavlinkBootstrapStateEngine bootstrapStateEngine,
      MavlinkBootstrapEventPublisher bootstrapEventPublisher
  ) {
    this.twinManager = twinManager;
    this.readinessEvaluator = readinessEvaluator;
    this.bootstrapStateEngine = bootstrapStateEngine;
    this.bootstrapEventPublisher = bootstrapEventPublisher;
  }

  @Override
  public void onTwinAdded(EntityTwin twin, TwinUpdateContext context) {
    evaluateTwin(twin, context);
  }

  @Override
  public void onTwinUpdated(String twinId, EntityTwin current, TwinUpdateContext context) {
    evaluateTwin(current, context);
  }

  @Override
  public void onTwinRemoved(EntityTwin removed, TwinUpdateContext context) {
    if (removed != null && removed.getTwinId() != null) {
      bootstrapStateEngine.remove(removed.getTwinId());
    }
  }

  @Override
  public void onRelationshipUpdated(
      String twinId,
      TwinRelationship relationship,
      TwinUpdateContext context
  ) {
    // no-op
  }

  @Override
  public void onTwinStatusChanged(
      String twinId,
      TwinLifecycleStatus previousStatus,
      TwinLifecycleStatus currentStatus,
      EntityTwin twin,
      TwinUpdateContext context
  ) {
    evaluateTwin(twin, context);
  }

  @Override
  public void onRelationshipRemoved(
      String twinId,
      TwinRelationship relationship,
      TwinUpdateContext context
  ) {
    // no-op
  }

  private void evaluateTwin(EntityTwin twin, TwinUpdateContext context) {
    if (!(twin instanceof DroneTwin droneTwin)) {
      return;
    }

    if (!isMavlinkBacked(droneTwin)) {
      return;
    }

    DroneTwinReadinessResult readinessResult =
        readinessEvaluator.evaluate(droneTwin, context);

    updateReadinessIfChanged(droneTwin, readinessResult, context);

    List<MavlinkBootstrapEvent> events =
        bootstrapStateEngine.update(droneTwin, readinessResult, context);

    if(bootstrapEventPublisher != null) {
      for (MavlinkBootstrapEvent event : events) {
        bootstrapEventPublisher.publish(event);
      }
    }
  }

  private boolean isMavlinkBacked(DroneTwin droneTwin) {
    return droneTwin.getSystemId() != null
        && droneTwin.getComponentId() != null
        && droneTwin.getProtocolSourceId() != null
        && droneTwin.getProtocolSourceId().startsWith("mavlink:");
  }

  private void updateReadinessIfChanged(
      DroneTwin droneTwin,
      DroneTwinReadinessResult readinessResult,
      TwinUpdateContext context
  ) {
    if (!hasReadinessChanged(droneTwin, readinessResult)) {
      return;
    }

    String twinId = droneTwin.getTwinId();

    twinManager.updateTwin(twinId, twin -> {
      DroneTwin updatedDroneTwin = (DroneTwin) twin;

      updatedDroneTwin.setReadinessState(readinessResult.getReadinessState().name());
      updatedDroneTwin.setRegistrationReady(readinessResult.isRegistrationReady());
      updatedDroneTwin.setCommandReady(readinessResult.isCommandReady());
      updatedDroneTwin.setMissingReadinessItems(toNames(readinessResult.getMissingStates()));
      updatedDroneTwin.setDegradedReadinessItems(toNames(readinessResult.getDegradedStates()));
      updatedDroneTwin.setBlockingReadinessItems(toNames(readinessResult.getBlockingStates()));
      updatedDroneTwin.setReadinessUpdatedAt(readinessResult.getEvaluatedAt());

    }, context);
  }

  private boolean hasReadinessChanged(
      DroneTwin droneTwin,
      DroneTwinReadinessResult readinessResult
  ) {
    String readinessState = readinessResult.getReadinessState() != null
        ? readinessResult.getReadinessState().name()
        : null;

    if (!equalsNullable(droneTwin.getReadinessState(), readinessState)) {
      return true;
    }

    if (!equalsNullable(droneTwin.getRegistrationReady(), readinessResult.isRegistrationReady())) {
      return true;
    }

    if (!equalsNullable(droneTwin.getCommandReady(), readinessResult.isCommandReady())) {
      return true;
    }

    if (!equalsNullable(
        droneTwin.getMissingReadinessItems(),
        toNames(readinessResult.getMissingStates())
    )) {
      return true;
    }

    if (!equalsNullable(
        droneTwin.getDegradedReadinessItems(),
        toNames(readinessResult.getDegradedStates())
    )) {
      return true;
    }

    return !equalsNullable(
        droneTwin.getBlockingReadinessItems(),
        toNames(readinessResult.getBlockingStates())
    );
  }

  private List<String> toNames(Set<DroneTwinMissingState> missingStates) {
    if (missingStates == null || missingStates.isEmpty()) {
      return List.of();
    }

    return missingStates.stream()
        .map(Enum::name)
        .sorted()
        .toList();
  }

  private boolean equalsNullable(Object left, Object right) {
    if (left == null) {
      return right == null;
    }

    return left.equals(right);
  }
}