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

package io.mapsmessaging.state.mavlink.model;

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public interface UxvModel {

  String getModelName();

  UxvVehicleType getVehicleType();

  Set<UxvOperation> getSupportedOperations();

  default boolean supports(UxvOperation operation) {
    return getSupportedOperations().contains(Objects.requireNonNull(operation, "operation must not be null"));
  }

  default void requireSupported(UxvOperation operation) {
    if (!supports(operation)) {
      throw unsupported(operation);
    }
  }

  default boolean supportsPassiveDetection() {
    return false;
  }

  default int firstMissionItemSequence() {
    return 0;
  }

  default Optional<DetectionEvent> interpretDetection(DroneTwin droneTwin, MavlinkPacket event) {
    return Optional.empty();
  }

  default UxvModelCommandSet arm(UxvCommandContext context) {
    throw unsupported(UxvOperation.ARM);
  }

  default UxvModelCommandSet disarm(UxvCommandContext context) {
    throw unsupported(UxvOperation.DISARM);
  }

  default UxvModelCommandSet setHome(UxvCommandContext context, HomeRequest request) {
    throw unsupported(UxvOperation.SET_HOME);
  }

  default UxvModelCommandSet returnToHome(UxvCommandContext context) {
    throw unsupported(UxvOperation.RETURN_TO_HOME);
  }

  default UxvModelCommandSet reposition(UxvCommandContext context, RepositionRequest request) {
    throw unsupported(UxvOperation.REPOSITION);
  }

  default UxvModelCommandSet orbit(UxvCommandContext context, OrbitRequest request) {
    throw unsupported(UxvOperation.ORBIT);
  }

  default UxvModelCommandSet loiter(UxvCommandContext context, LoiterRequest request) {
    throw unsupported(UxvOperation.LOITER);
  }

  default UxvModelCommandSet holdPosition(UxvCommandContext context) {
    throw unsupported(UxvOperation.HOLD_POSITION);
  }

  default UxvModelCommandSet stop(UxvCommandContext context) {
    throw unsupported(UxvOperation.STOP);
  }

  default UxvModelCommandSet pauseVehicle(UxvCommandContext context) {
    throw unsupported(UxvOperation.PAUSE_VEHICLE);
  }

  default UxvModelCommandSet resumeVehicle(UxvCommandContext context) {
    throw unsupported(UxvOperation.RESUME_VEHICLE);
  }

  default PlanValidation validateMission(MissionPlan missionPlan) {
    return PlanValidation.success();
  }

  default UxvModelCommandSet buildMission(UxvCommandContext context, MissionPlan missionPlan) {
    throw unsupported(UxvOperation.BUILD_MISSION);
  }

  default UxvModelCommandSet startMission(UxvCommandContext context) {
    throw unsupported(UxvOperation.START_MISSION);
  }

  default UxvModelCommandSet setCurrentMission(
      UxvCommandContext context, MissionCurrentRequest request) {
    throw unsupported(UxvOperation.SET_CURRENT_MISSION);
  }

  default UxvModelCommandSet clearMission(UxvCommandContext context) {
    throw unsupported(UxvOperation.CLEAR_MISSION);
  }

  default UxvNavigationPlan navigate(UxvCommandContext context, List<GeoPosition> waypoints, Duration duration) {
    throw unsupported(UxvOperation.NAVIGATE);
  }

  default UxvNavigationPlan navigatePlan(UxvCommandContext context, MissionPlan missionPlan, Duration duration) {
    throw unsupported(UxvOperation.NAVIGATE);
  }

  default UnsupportedUxvOperationException unsupported(UxvOperation operation) {
    return new UnsupportedUxvOperationException(getModelName(), operation);
  }
}
