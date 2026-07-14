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

package io.mapsmessaging.state.mavlink.model.impl.uav;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandIntFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.LoiterRequest;
import io.mapsmessaging.state.mavlink.model.OrbitRequest;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanItemType;
import io.mapsmessaging.state.mavlink.model.PlanValidationIssue;
import io.mapsmessaging.state.mavlink.model.UavModel;
import io.mapsmessaging.state.mavlink.model.UnsupportedUxvOperationException;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.model.UxvVehicleType;
import io.mapsmessaging.state.mavlink.model.impl.px4.GenericPx4UxvModel;
import java.util.List;
import java.util.Objects;

public class GenericPx4UavModel extends GenericPx4UxvModel implements UavModel {

  public static final String MODEL_NAME = "generic-px4-uav";

  private static final int MAV_CMD_NAV_LAND = 21;
  private static final int MAV_CMD_NAV_TAKEOFF = 22;

  public GenericPx4UavModel() {
    this(MODEL_NAME);
  }

  protected GenericPx4UavModel(String modelName) {
    super(
        modelName,
        UxvVehicleType.UAV,
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
            UxvOperation.TAKE_OFF,
            UxvOperation.LAND,
            UxvOperation.SET_SPEED,
            UxvOperation.SET_HEADING,
            UxvOperation.ORBIT,
            UxvOperation.LOITER));
  }

  @Override
  public UxvModelCommandSet takeOff(UxvCommandContext context, double altitudeMeters) {
    Objects.requireNonNull(context, "context must not be null");
    requirePositive(altitudeMeters, "altitudeMeters");

    MavlinkCommandLong commandLong =
        MavlinkCommandLongFactory.command(
            context.targetSystem(),
            context.targetComponent(),
            MAV_CMD_NAV_TAKEOFF,
            context.sequence());
    commandLong.setParam7((float) altitudeMeters);

    return UxvModelCommandSet.of(UxvOperation.TAKE_OFF, getModelName(), commandLong);
  }

  @Override
  public UxvModelCommandSet land(UxvCommandContext context) {
    Objects.requireNonNull(context, "context must not be null");

    return UxvModelCommandSet.of(
        UxvOperation.LAND,
        getModelName(),
        MavlinkCommandLongFactory.command(
            context.targetSystem(),
            context.targetComponent(),
            MAV_CMD_NAV_LAND,
            context.sequence()));
  }

  @Override
  public UxvModelCommandSet orbit(UxvCommandContext context, OrbitRequest request) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(request, "request must not be null");

    requirePositive(request.radiusMeters(), "radiusMeters");
    rejectDepth(request.depthMeters(), UxvOperation.ORBIT);
    rejectSpeed(request.speedMetersPerSecond(), UxvOperation.ORBIT);
    rejectDuration(request.duration(), UxvOperation.ORBIT);

    double radiusMeters =
        switch (request.direction()) {
          case COUNTER_CLOCKWISE -> -request.radiusMeters();
          case CLOCKWISE, UNSPECIFIED -> request.radiusMeters();
          case null -> request.radiusMeters();
        };

    return UxvModelCommandSet.of(
        UxvOperation.ORBIT,
        getModelName(),
        MavlinkCommandIntFactory.orbit(
            context.targetSystem(),
            context.targetComponent(),
            withAltitude(request.center(), request.altitudeMeters()),
            radiusMeters,
            context.sequence()));
  }

  @Override
  public UxvModelCommandSet loiter(UxvCommandContext context, LoiterRequest request) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(request, "request must not be null");

    requirePositiveOrZero(request.radiusMeters(), "radiusMeters");
    rejectDepth(request.depthMeters(), UxvOperation.LOITER);

    GeoPosition position = withAltitude(request.position(), request.altitudeMeters());
    validateCoordinates(position, "position");

    MavlinkMessage message;
    if (toDuration(request.duration(), "duration").isZero()) {
      float yawDegrees =
          request.yawDegrees() == null
              ? Float.NaN
              : normaliseDegrees(request.yawDegrees());

      message =
          MavlinkCommandIntFactory.loiterUnlimited(
              context.targetSystem(),
              context.targetComponent(),
              position,
              request.radiusMeters(),
              yawDegrees,
              context.sequence());
    } else {
      rejectYaw(request.yawDegrees(), UxvOperation.LOITER);

      message =
          MavlinkCommandIntFactory.loiterTime(
              context.targetSystem(),
              context.targetComponent(),
              position,
              request.radiusMeters(),
              request.duration(),
              context.sequence());
    }

    return UxvModelCommandSet.of(UxvOperation.LOITER, getModelName(), message);
  }

  @Override
  protected MavlinkMessage toMissionMessage(
      UxvCommandContext context,
      int sequence,
      PlanItem item) {
    return switch (item.type()) {
      case WAYPOINT -> toMissionWaypoint(context, sequence, item);
      case LOITER -> toMissionLoiter(context, sequence, item);
      case RETURN_TO_HOME ->
          MavlinkMissionItemIntFactory.returnToLaunch(
              context.targetSystem(),
              context.targetComponent(),
              sequence);
      case ORBIT, HOLD_POSITION ->
          throw new UnsupportedUxvOperationException(
              getModelName(),
              UxvOperation.BUILD_MISSION,
              "Mission item type "
                  + item.type()
                  + " is not supported by this PX4 UAV model");
    };
  }

  protected MavlinkMessage toMissionWaypoint(
      UxvCommandContext context,
      int sequence,
      PlanItem item) {
    return MavlinkMissionItemIntFactory.waypoint(
        context.targetSystem(),
        context.targetComponent(),
        sequence,
        withAltitude(item.position(), item.altitudeMeters()),
        toSeconds(item.holdDuration()),
        item.radiusMeters() == null
            ? DEFAULT_ACCEPTANCE_RADIUS_METERS
            : item.radiusMeters().floatValue(),
        DEFAULT_PASS_RADIUS_METERS,
        item.yawDegrees() == null
            ? Float.NaN
            : normaliseDegrees(item.yawDegrees()));
  }

  protected MavlinkMessage toMissionLoiter(
      UxvCommandContext context,
      int sequence,
      PlanItem item) {
    GeoPosition position =
        withAltitude(item.position(), item.altitudeMeters());

    double radiusMeters =
        item.radiusMeters() == null
            ? DEFAULT_ACCEPTANCE_RADIUS_METERS
            : item.radiusMeters();

    requirePositiveOrZero(radiusMeters, "radiusMeters");

    if (toDuration(item.holdDuration(), "holdDuration").isZero()) {
      return MavlinkMissionItemIntFactory.loiterUnlimited(
          context.targetSystem(),
          context.targetComponent(),
          sequence,
          position,
          radiusMeters);
    }

    return MavlinkMissionItemIntFactory.loiterTime(context.targetSystem(), context.targetComponent(), sequence, position, item.holdDuration(), radiusMeters);
  }

  @Override
  protected void validatePlanItem(
      int index,
      PlanItem item,
      List<PlanValidationIssue> issues) {
    if (item.depthMeters() != null) {
      issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " contains depthMeters, which is not valid for a UAV model"));
    }

    if ((item.type() == PlanItemType.WAYPOINT
        || item.type() == PlanItemType.LOITER)
        && item.position() != null
        && item.altitudeMeters() == null
        && item.position().getPreferredAltitudeMeters() == null) {
      issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " requires an MSL or AGL altitude"));
    }

    if (item.speedMetersPerSecond() != null) {
      issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " speedMetersPerSecond is not currently mapped by this PX4 UAV model"));
    }

    if (item.type() == PlanItemType.ORBIT
        || item.type() == PlanItemType.HOLD_POSITION) {
      issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " type " + item.type() + " is not supported by this PX4 UAV model"));
    }
  }
}