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

package io.mapsmessaging.state.mavlink.model.impl;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandIntFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.HomeRequest;
import io.mapsmessaging.state.mavlink.model.MissionPlan;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanValidation;
import io.mapsmessaging.state.mavlink.model.PlanValidationIssue;
import io.mapsmessaging.state.mavlink.model.RepositionRequest;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvNavigationPlan;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.model.UxvVehicleType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractMissionUxvModel extends AbstractUxvModel {

  protected static final float DEFAULT_ACCEPTANCE_RADIUS_METERS = 2.0f;
  protected static final float DEFAULT_PASS_RADIUS_METERS = 0.0f;
  protected static final float DEFAULT_HOLD_SECONDS = 0.0f;

  private static final int MAV_CMD_DO_SET_HOME = 179;
  private static final int MAV_CMD_DO_CHANGE_SPEED = 178;
  private static final int MAV_CMD_CONDITION_YAW = 115;

  private static final float USE_CURRENT_POSITION = 1.0f;
  private static final float USE_SPECIFIED_POSITION = 0.0f;
  private static final float GROUND_SPEED = 1.0f;
  private static final float UNCHANGED_THROTTLE = -1.0f;
  private static final float CLOCKWISE_YAW_DIRECTION = 1.0f;
  private static final float RELATIVE_YAW_OFFSET = 0.0f;

  protected AbstractMissionUxvModel(String modelName, UxvVehicleType vehicleType, Set<UxvOperation> supportedOperations) {
    super(modelName, vehicleType, supportedOperations);
  }

  @Override
  public final UxvNavigationPlan navigate(UxvCommandContext context, List<GeoPosition> waypoints, Duration duration) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(waypoints, "waypoints must not be null");

    if (waypoints.isEmpty()) {
      throw new IllegalArgumentException("waypoints must contain at least one position");
    }

    Duration validatedDuration = toDuration(duration, "duration");
    List<MavlinkMessage> missionMessages = new ArrayList<>(waypoints.size());

    for (int index = 0; index < waypoints.size(); index++) {
      GeoPosition position = Objects.requireNonNull(waypoints.get(index), "waypoints must not contain null positions");
      validateNavigationWaypoint(position, index);
      missionMessages.add(toNavigationMessage(context, index, position));
    }

    UxvModelCommandSet missionPhase = UxvModelCommandSet.of(
        UxvOperation.BUILD_MISSION,
        getModelName(),
        missionMessages);

    return new UxvNavigationPlan(
        List.of(missionPhase),
        List.of(startMission(context)),
        validatedDuration,
        stop(context));
  }

  public UxvModelCommandSet arm(UxvCommandContext context) {
    Objects.requireNonNull(context, "context must not be null");
    return UxvModelCommandSet.of(UxvOperation.ARM, getModelName(), MavlinkCommandLongFactory.arm(context.targetSystem(), context.targetComponent(), context.sequence()));
  }

  public UxvModelCommandSet disarm(UxvCommandContext context) {
    Objects.requireNonNull(context, "context must not be null");
    return UxvModelCommandSet.of(UxvOperation.DISARM, getModelName(), MavlinkCommandLongFactory.disarm(context.targetSystem(), context.targetComponent(), context.sequence()));
  }

  public UxvModelCommandSet setHome(UxvCommandContext context, HomeRequest request) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(request, "request must not be null");

    MavlinkCommandLong commandLong =
        MavlinkCommandLongFactory.command(
            context.targetSystem(),
            context.targetComponent(),
            MAV_CMD_DO_SET_HOME,
            context.sequence());
    commandLong.setParam1(request.useCurrentPosition() ? USE_CURRENT_POSITION : USE_SPECIFIED_POSITION);

    if (!request.useCurrentPosition()) {
      GeoPosition position = Objects.requireNonNull(request.position(), "position must not be null when useCurrentPosition is false");
      validateCoordinates(position, "position");
      commandLong.setParam5(toFloatCoordinate(position.getLatitude(), "latitude"));
      commandLong.setParam6(toFloatCoordinate(position.getLongitude(), "longitude"));
      commandLong.setParam7(toAltitude(position));
    }

    return UxvModelCommandSet.of(UxvOperation.SET_HOME, getModelName(), commandLong);
  }

  public UxvModelCommandSet reposition(UxvCommandContext context, RepositionRequest request) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(request, "request must not be null");
    rejectSpeed(request.speedMetersPerSecond(), UxvOperation.REPOSITION);
    validateCoordinates(request.position(), "position");

    float yawDegrees = request.yawDegrees() == null ? Float.NaN : normaliseDegrees(request.yawDegrees());
    return UxvModelCommandSet.of(
        UxvOperation.REPOSITION,
        getModelName(),
        MavlinkCommandIntFactory.reposition(context.targetSystem(), context.targetComponent(), request.position(), yawDegrees, context.sequence())
    );
  }

  public UxvModelCommandSet holdPosition(UxvCommandContext context) {
    Objects.requireNonNull(context, "context must not be null");
    return UxvModelCommandSet.of(
        UxvOperation.HOLD_POSITION,
        getModelName(),
        pauseCommand(context)
    );
  }

  @Override
  public UxvModelCommandSet stop(UxvCommandContext context) {
    java.util.Objects.requireNonNull(context, "context must not be null");
    MavlinkMessage message = MavlinkCommandIntFactory.stop(context.targetSystem(), context.targetComponent(), context.sequence());
    return UxvModelCommandSet.of(UxvOperation.STOP, getModelName(), message);
  }

  public UxvModelCommandSet pauseVehicle(UxvCommandContext context) {
    Objects.requireNonNull(context, "context must not be null");
    return UxvModelCommandSet.of(UxvOperation.PAUSE_VEHICLE, getModelName(), pauseCommand(context));
  }

  public UxvModelCommandSet resumeVehicle(UxvCommandContext context) {
    Objects.requireNonNull(context, "context must not be null");
    return UxvModelCommandSet.of(
        UxvOperation.RESUME_VEHICLE,
        getModelName(),
        MavlinkCommandLongFactory.resume(context.targetSystem(), context.targetComponent(), context.sequence()));
  }

  public PlanValidation validateMission(MissionPlan missionPlan) {
    Objects.requireNonNull(missionPlan, "missionPlan must not be null");

    List<PlanValidationIssue> issues = new ArrayList<>();
    if (missionPlan.items().isEmpty()) {
      issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission plan must contain at least one item"));
    }

    for (int index = 0; index < missionPlan.items().size(); index++) {
      PlanItem item = missionPlan.items().get(index);
      if (item == null) {
        issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " must not be null"));
      } else {
        validatePlanItem(index, item, issues);
      }
    }

    return issues.isEmpty() ? PlanValidation.success() : PlanValidation.failure(issues);
  }

  public UxvModelCommandSet buildMission(UxvCommandContext context, MissionPlan missionPlan) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(missionPlan, "missionPlan must not be null");

    PlanValidation validation = validateMission(missionPlan);
    if (!validation.valid()) {
      throw new IllegalArgumentException("Invalid mission plan: " + validation.issues());
    }

    List<MavlinkMessage> messages = new ArrayList<>(missionPlan.items().size());
    for (int index = 0; index < missionPlan.items().size(); index++) {
      messages.add(toMissionMessage(context, index, missionPlan.items().get(index)));
    }

    return UxvModelCommandSet.of(UxvOperation.BUILD_MISSION, getModelName(), messages);
  }

  public UxvModelCommandSet startMission(UxvCommandContext context) {
    Objects.requireNonNull(context, "context must not be null");
    return UxvModelCommandSet.of(
        UxvOperation.START_MISSION,
        getModelName(),
        MavlinkCommandLongFactory.missionStart(context.targetSystem(), context.targetComponent(), context.sequence()));
  }

  public UxvModelCommandSet setSpeed(UxvCommandContext context, double speedMetersPerSecond) {
    Objects.requireNonNull(context, "context must not be null");
    requirePositiveOrZero(speedMetersPerSecond, "speedMetersPerSecond");

    MavlinkCommandLong commandLong =
        MavlinkCommandLongFactory.command(
            context.targetSystem(),
            context.targetComponent(),
            MAV_CMD_DO_CHANGE_SPEED,
            context.sequence());
    commandLong.setParam1(GROUND_SPEED);
    commandLong.setParam2((float) speedMetersPerSecond);
    commandLong.setParam3(UNCHANGED_THROTTLE);

    return UxvModelCommandSet.of(UxvOperation.SET_SPEED, getModelName(), commandLong);
  }

  public UxvModelCommandSet setHeading(UxvCommandContext context, float headingDegrees) {
    Objects.requireNonNull(context, "context must not be null");

    MavlinkCommandLong commandLong =
        MavlinkCommandLongFactory.command(
            context.targetSystem(),
            context.targetComponent(),
            MAV_CMD_CONDITION_YAW,
            context.sequence());
    commandLong.setParam1(normaliseDegrees(headingDegrees));
    commandLong.setParam2(0.0f);
    commandLong.setParam3(CLOCKWISE_YAW_DIRECTION);
    commandLong.setParam4(RELATIVE_YAW_OFFSET);

    return UxvModelCommandSet.of(UxvOperation.SET_HEADING, getModelName(), commandLong);
  }

  protected abstract MavlinkMessage toMissionMessage(UxvCommandContext context, int sequence, PlanItem item);

  protected abstract void validatePlanItem(int index, PlanItem item, List<PlanValidationIssue> issues);

  protected MavlinkMessage toNavigationMessage(UxvCommandContext context, int sequence, GeoPosition position) {
    return MavlinkMissionItemIntFactory.waypoint(
        context.targetSystem(),
        context.targetComponent(),
        sequence,
        position,
        DEFAULT_HOLD_SECONDS,
        DEFAULT_ACCEPTANCE_RADIUS_METERS,
        DEFAULT_PASS_RADIUS_METERS,
        Float.NaN);
  }

  protected void validateNavigationPosition(GeoPosition position, int index) {
  }

  protected final void validateCoordinates(GeoPosition position, String name) {
    Objects.requireNonNull(position, name + " must not be null");

    Double latitude = position.getLatitude();
    Double longitude = position.getLongitude();
    if (latitude == null) {
      throw new IllegalArgumentException(name + ".latitude must not be null");
    }
    if (longitude == null) {
      throw new IllegalArgumentException(name + ".longitude must not be null");
    }

    requireFinite(latitude, name + ".latitude");
    requireFinite(longitude, name + ".longitude");

    if (latitude < -90.0d || latitude > 90.0d) {
      throw new IllegalArgumentException(name + ".latitude must be between -90 and 90 degrees");
    }
    if (longitude < -180.0d || longitude > 180.0d) {
      throw new IllegalArgumentException(name + ".longitude must be between -180 and 180 degrees");
    }
  }

  protected final MavlinkCommandLong pauseCommand(UxvCommandContext context) {
    return MavlinkCommandLongFactory.pause(context.targetSystem(), context.targetComponent(), context.sequence());
  }

  protected final GeoPosition withAltitude(GeoPosition position, Double altitudeMeters) {
    Objects.requireNonNull(position, "position must not be null");

    if (altitudeMeters == null) {
      return position;
    }

    requireFinite(altitudeMeters, "altitudeMeters");
    return new GeoPosition(position.getLatitude(), position.getLongitude(), altitudeMeters, null);
  }

  protected final float toAltitude(GeoPosition position) {
    Double altitudeMeters = position.getPreferredAltitudeMeters();
    if (altitudeMeters == null) {
      return 0.0f;
    }
    requireFinite(altitudeMeters, "altitudeMeters");
    return altitudeMeters.floatValue();
  }

  protected final float toFloatCoordinate(Double coordinate, String name) {
    if (coordinate == null) {
      throw new IllegalArgumentException(name + " must not be null");
    }
    requireFinite(coordinate, name);
    return coordinate.floatValue();
  }

  protected final float toSeconds(Duration duration) {
    Duration validatedDuration = toDuration(duration, "duration");
    return validatedDuration.isZero() ? DEFAULT_HOLD_SECONDS : (float) validatedDuration.toSeconds();
  }

  protected final Duration toDuration(Duration duration, String name) {
    if (duration == null) {
      return Duration.ZERO;
    }
    if (duration.isNegative()) {
      throw new IllegalArgumentException(name + " must not be negative");
    }
    return duration;
  }

  protected final void rejectSpeed(Double speedMetersPerSecond, UxvOperation operation) {
    if (speedMetersPerSecond != null) {
      throw new IllegalArgumentException(operation + " does not currently map speedMetersPerSecond for model " + getModelName());
    }
  }

  protected final void rejectAltitude(Double altitudeMeters, UxvOperation operation) {
    if (altitudeMeters != null) {
      throw new IllegalArgumentException(operation + " does not currently map altitudeMeters for model " + getModelName());
    }
  }

  protected final void rejectDepth(Double depthMeters, UxvOperation operation) {
    if (depthMeters != null) {
      throw new IllegalArgumentException(operation + " does not support depthMeters for model " + getModelName());
    }
  }

  protected void rejectYaw(Float yawDegrees, UxvOperation operation) {
    if (yawDegrees != null) {
      throw new IllegalArgumentException(operation + " does not currently map yawDegrees for model " + getModelName());
    }
  }

  protected final void rejectDuration(Duration duration, UxvOperation operation) {
    Duration validatedDuration = toDuration(duration, "duration");
    if (!validatedDuration.isZero()) {
      throw new IllegalArgumentException(operation + " does not currently map duration for model " + getModelName());
    }
  }

  protected final void requireFinite(double value, String name) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be finite");
    }
  }

  protected final void requireFinite(float value, String name) {
    if (!Float.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be finite");
    }
  }

  protected final void requirePositive(double value, String name) {
    requireFinite(value, name);
    if (value <= 0.0d) {
      throw new IllegalArgumentException(name + " must be greater than zero");
    }
  }

  protected final void requirePositiveOrZero(double value, String name) {
    requireFinite(value, name);
    if (value < 0.0d) {
      throw new IllegalArgumentException(name + " must not be negative");
    }
  }

  protected final boolean isPositiveOrZero(double value) {
    return Double.isFinite(value) && value >= 0.0d;
  }

  protected final float normaliseDegrees(float degrees) {
    requireFinite(degrees, "degrees");

    float normalised = degrees % 360.0f;
    if (normalised < 0.0f) {
      normalised += 360.0f;
    }
    return normalised;
  }

  private void validateNavigationWaypoint(GeoPosition position, int index) {
    validateCoordinates(position, "waypoints[" + index + "]");
    validateNavigationPosition(position, index);
  }
}
