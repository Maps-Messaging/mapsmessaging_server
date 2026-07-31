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

package io.mapsmessaging.state.mavlink.model.impl.ardupilot;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.MissionPlan;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanValidation;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.model.UxvVehicleType;
import io.mapsmessaging.state.mavlink.model.impl.AbstractMissionUxvModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class GenericArduPilotUxvModel extends AbstractMissionUxvModel {

  private static final int HOME_MISSION_SEQUENCE = 0;
  private static final int FIRST_REAL_MISSION_SEQUENCE = 1;

  protected GenericArduPilotUxvModel(String modelName, UxvVehicleType vehicleType, Set<UxvOperation> supportedOperations) {
    super(modelName, vehicleType, supportedOperations);
  }

  @Override
  public int firstMissionItemSequence() {
    return FIRST_REAL_MISSION_SEQUENCE;
  }

  @Override
  public UxvModelCommandSet buildMission(UxvCommandContext context, MissionPlan missionPlan) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(missionPlan, "missionPlan must not be null");

    PlanValidation validation = validateMission(missionPlan);
    if (!validation.valid()) {
      throw new IllegalArgumentException("Invalid mission plan: " + validation.issues());
    }

    int additionalItems = FIRST_REAL_MISSION_SEQUENCE + (missionPlan.repeats() ? 1 : 0);
    List<MavlinkMessage> messages = new ArrayList<>(missionPlan.items().size() + additionalItems);
    messages.add(homeMissionItem(context, missionPlan));

    for (int index = 0; index < missionPlan.items().size(); index++) {
      messages.add(toMissionMessage(context, index + FIRST_REAL_MISSION_SEQUENCE, missionPlan.items().get(index)));
    }

    if (missionPlan.repeats()) {
      messages.add(
          MavlinkMissionItemIntFactory.jump(
              context.targetSystem(),
              context.targetComponent(),
              missionPlan.items().size() + FIRST_REAL_MISSION_SEQUENCE,
              FIRST_REAL_MISSION_SEQUENCE,
              missionPlan.jumpRepeatCount()));
    }

    return UxvModelCommandSet.of(UxvOperation.BUILD_MISSION, getModelName(), messages);
  }

  @Override
  public UxvModelCommandSet startMission(UxvCommandContext context) {
    Objects.requireNonNull(context, "context must not be null");
    List<MavlinkMessage> messages =
        List.of(
            MavlinkCommandLongFactory.setMissionCurrent(
                context.targetSystem(),
                context.targetComponent(),
                context.sequence(),
                FIRST_REAL_MISSION_SEQUENCE,
                true),
            MavlinkCommandLongFactory.missionStart(
                context.targetSystem(),
                context.targetComponent(),
                context.sequence()));
    return UxvModelCommandSet.of(UxvOperation.START_MISSION, getModelName(), messages);
  }

  @Override
  public UxvModelCommandSet returnToHome(UxvCommandContext context) {
    Objects.requireNonNull(context, "context must not be null");
    return UxvModelCommandSet.of(
        UxvOperation.RETURN_TO_HOME,
        getModelName(),
        MavlinkCommandLongFactory.returnToLaunch(context.targetSystem(), context.targetComponent(), context.sequence()));
  }

  private MavlinkMessage homeMissionItem(UxvCommandContext context, MissionPlan missionPlan) {
    GeoPosition placeholderPosition =
        missionPlan.items().stream()
            .map(PlanItem::position)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseGet(() -> new GeoPosition(0.0d, 0.0d, 0.0d, null));

    return MavlinkMissionItemIntFactory.waypoint(
        context.targetSystem(),
        context.targetComponent(),
        HOME_MISSION_SEQUENCE,
        placeholderPosition);
  }
}
