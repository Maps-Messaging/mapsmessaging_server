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

package io.mapsmessaging.state.mavlink.model.impl.ugv;

import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanItemType;
import io.mapsmessaging.state.mavlink.model.PlanValidationIssue;
import io.mapsmessaging.state.mavlink.model.UgvModel;
import io.mapsmessaging.state.mavlink.model.UnsupportedUxvOperationException;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.model.UxvVehicleType;
import io.mapsmessaging.state.mavlink.model.impl.px4.GenericPx4UxvModel;
import java.util.List;

public class GenericPx4UgvModel extends GenericPx4UxvModel implements UgvModel {

  public static final String MODEL_NAME = "generic-px4-ugv";

  public GenericPx4UgvModel() {
    super(
        MODEL_NAME,
        UxvVehicleType.UGV,
        operations(
            UxvOperation.ARM,
            UxvOperation.DISARM,
            UxvOperation.SET_HOME,
            UxvOperation.RETURN_TO_HOME,
            UxvOperation.REPOSITION,
            UxvOperation.HOLD_POSITION,
            UxvOperation.STOP,
            UxvOperation.PAUSE_VEHICLE,
            UxvOperation.RESUME_VEHICLE,
            UxvOperation.BUILD_MISSION,
            UxvOperation.START_MISSION,
            UxvOperation.NAVIGATE,
            UxvOperation.SET_SPEED,
            UxvOperation.SET_HEADING));
  }

  @Override
  protected MavlinkMessage toMissionMessage(UxvCommandContext context, int sequence, PlanItem item) {
    return switch (item.type()) {
      case WAYPOINT ->
          MavlinkMissionItemIntFactory.waypoint(
              context.targetSystem(),
              context.targetComponent(),
              sequence,
              item.position(),
              toSeconds(item.holdDuration()),
              item.radiusMeters() == null ? DEFAULT_ACCEPTANCE_RADIUS_METERS : item.radiusMeters().floatValue(),
              DEFAULT_PASS_RADIUS_METERS,
              item.yawDegrees() == null ? Float.NaN : normaliseDegrees(item.yawDegrees()));
      case RETURN_TO_HOME ->
          MavlinkMissionItemIntFactory.returnToLaunch(context.targetSystem(), context.targetComponent(), sequence);
      case LOITER, ORBIT, HOLD_POSITION ->
          throw new UnsupportedUxvOperationException(
              getModelName(),
              UxvOperation.BUILD_MISSION,
              "Mission item type " + item.type() + " is not supported by this PX4 UGV model");
    };
  }

  @Override
  protected void validatePlanItem(int index, PlanItem item, List<PlanValidationIssue> issues) {
    if (item.type() == PlanItemType.LOITER || item.type() == PlanItemType.ORBIT || item.type() == PlanItemType.HOLD_POSITION) {
      issues.add(
          new PlanValidationIssue(
              UxvOperation.BUILD_MISSION,
              "Mission item " + index + " type " + item.type() + " is not supported by this PX4 UGV model"));
    }

    if (item.speedMetersPerSecond() != null) {
      issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " speedMetersPerSecond is not currently mapped by this PX4 UGV model"));
    }

    if (item.altitudeMeters() != null) {
      issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " altitudeMeters is not currently mapped by this PX4 UGV model"));
    }

    if (item.depthMeters() != null) {
      issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " depthMeters is not valid for a UGV model"));
    }
  }

}