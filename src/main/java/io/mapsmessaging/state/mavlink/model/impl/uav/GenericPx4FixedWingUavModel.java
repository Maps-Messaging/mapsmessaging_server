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
import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.FixedWingUavModel;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanItemType;
import io.mapsmessaging.state.mavlink.model.PlanValidationIssue;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import java.time.Duration;
import java.util.List;

public class GenericPx4FixedWingUavModel extends GenericPx4UavModel implements FixedWingUavModel {

  public static final String MODEL_NAME = "generic-px4-fixed-wing-uav";
  public static final double DEFAULT_LOITER_RADIUS_METERS = 50.0d;

  private final double defaultLoiterRadiusMeters;

  public GenericPx4FixedWingUavModel() {
    this(DEFAULT_LOITER_RADIUS_METERS);
  }

  public GenericPx4FixedWingUavModel(double defaultLoiterRadiusMeters) {
    super(MODEL_NAME);
    requirePositive(defaultLoiterRadiusMeters, "defaultLoiterRadiusMeters");
    this.defaultLoiterRadiusMeters = defaultLoiterRadiusMeters;
  }

  public double getDefaultLoiterRadiusMeters() {
    return defaultLoiterRadiusMeters;
  }

  @Override
  protected MavlinkMessage toMissionWaypoint(UxvCommandContext context, int sequence, PlanItem item) {
    Duration holdDuration = toDuration(item.holdDuration(), "holdDuration");

    if (holdDuration.isZero()) {
      return super.toMissionWaypoint(context, sequence, item);
    }

    rejectYaw(item.yawDegrees(), UxvOperation.BUILD_MISSION);

    GeoPosition position = withAltitude(item.position(), item.altitudeMeters());

    return MavlinkMissionItemIntFactory.loiterTime(
        context.targetSystem(),
        context.targetComponent(),
        sequence,
        position,
        holdDuration,
        resolveLoiterRadius(item));
  }

  @Override
  protected MavlinkMessage toMissionLoiter(UxvCommandContext context, int sequence, PlanItem item) {
    GeoPosition position = withAltitude(item.position(), item.altitudeMeters());

    double radiusMeters = resolveLoiterRadius(item);
    Duration holdDuration = toDuration(item.holdDuration(), "holdDuration");

    if (holdDuration.isZero()) {
      return MavlinkMissionItemIntFactory.loiterUnlimited(
          context.targetSystem(),
          context.targetComponent(),
          sequence,
          position,
          radiusMeters);
    }

    return MavlinkMissionItemIntFactory.loiterTime(
        context.targetSystem(),
        context.targetComponent(),
        sequence,
        position,
        holdDuration,
        radiusMeters);
  }

  @Override
  protected void validatePlanItem(int index, PlanItem item, List<PlanValidationIssue> issues) {
    super.validatePlanItem(index, item, issues);

    boolean timedWaypoint =
        item.type() == PlanItemType.WAYPOINT
            && item.holdDuration() != null
            && !item.holdDuration().isZero();

    if ((timedWaypoint || item.type() == PlanItemType.LOITER)
        && item.radiusMeters() != null
        && item.radiusMeters() <= 0.0d) {
      issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " radiusMeters must be greater than zero for fixed-wing loiter"));
    }

    if (timedWaypoint && item.yawDegrees() != null) {
      issues.add(
          new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " yawDegrees is not supported for a timed fixed-wing waypoint"));
    }

    if (item.type() == PlanItemType.LOITER && item.yawDegrees() != null) {
      issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " yawDegrees is not supported for fixed-wing loiter"));
    }
  }

  private double resolveLoiterRadius(PlanItem item) {
    double radiusMeters =
        item.radiusMeters() == null
            ? defaultLoiterRadiusMeters
            : item.radiusMeters();

    requirePositive(radiusMeters, "radiusMeters");
    return radiusMeters;
  }
}