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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.model.MissionPlan;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanItemType;
import io.mapsmessaging.state.mavlink.model.PlanValidation;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class GenericPx4UavModelMissionValidationTest {


  private final GenericPx4UavModel model = new GenericPx4UavModel();

  @Test
  void acceptsMslWaypoint() {
    assertTrue(validate(waypoint(new GeoPosition(-33.8688d, 151.2093d, 120.0d, null))).valid());
  }

  @Test
  void acceptsAglWaypoint() {
    assertTrue(validate(waypoint(new GeoPosition(-33.8688d, 151.2093d, null, 35.0d))).valid());
  }

  @Test
  void acceptsPlanItemMslAltitudeOverride() {
    PlanItem item =
        new PlanItem(
            PlanItemType.WAYPOINT,
            new GeoPosition(-33.8688d, 151.2093d, null, null),
            null,
            null,
            null,
            null,
            150.0d,
            null);

    assertTrue(validate(item).valid());
  }

  @Test
  void acceptsBoundaryCoordinates() {
    assertTrue(validate(waypoint(new GeoPosition(-90.0d, -180.0d, 120.0d, null))).valid());
    assertTrue(validate(waypoint(new GeoPosition(90.0d, 180.0d, 120.0d, null))).valid());
  }

  @Test
  void acceptsReturnToHomeWithoutPosition() {
    PlanItem item =
        new PlanItem(
            PlanItemType.RETURN_TO_HOME,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    assertTrue(validate(item).valid());
  }

  @Test
  void rejectsWaypointWithoutAltitude() {
    assertFalse(validate(waypoint(new GeoPosition(-33.8688d, 151.2093d, null, null))).valid());
  }

  @Test
  void rejectsInvalidCoordinatesBeforeMissionCompilation() {
    assertFalse(validate(waypoint(new GeoPosition(null, 151.2093d, 120.0d, null))).valid());
    assertFalse(validate(waypoint(new GeoPosition(-33.8688d, null, 120.0d, null))).valid());
    assertFalse(validate(waypoint(new GeoPosition(91.0d, 151.2093d, 120.0d, null))).valid());
    assertFalse(validate(waypoint(new GeoPosition(-33.8688d, 181.0d, 120.0d, null))).valid());
    assertFalse(validate(waypoint(new GeoPosition(Double.NaN, 151.2093d, 120.0d, null))).valid());
    assertFalse(validate(waypoint(new GeoPosition(-33.8688d, Double.POSITIVE_INFINITY, 120.0d, null))).valid());
  }

  @Test
  void rejectsNonFiniteAltitudeValues() {
    assertFalse(validate(waypoint(new GeoPosition(-33.8688d, 151.2093d, Double.NaN, null))).valid());
    assertFalse(validate(waypoint(new GeoPosition(-33.8688d, 151.2093d, null, Double.POSITIVE_INFINITY))).valid());

    PlanItem override =
        new PlanItem(
            PlanItemType.WAYPOINT,
            new GeoPosition(-33.8688d, 151.2093d, null, null),
            null,
            null,
            null,
            null,
            Double.NaN,
            null);

    assertFalse(validate(override).valid());
  }

  @Test
  void rejectsNegativeRadiusAndDuration() {
    assertFalse(
        validate(
                new PlanItem(
                    PlanItemType.WAYPOINT,
                    new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
                    Duration.ZERO,
                    -1.0d,
                    null,
                    null,
                    null,
                    null))
            .valid());

    assertFalse(
        validate(
                new PlanItem(
                    PlanItemType.WAYPOINT,
                    new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
                    Duration.ofMillis(-1),
                    null,
                    null,
                    null,
                    null,
                    null))
            .valid());
  }

  @Test
  void rejectsNonFiniteYawSpeedAndDepth() {
    assertFalse(validate(itemWith(Float.NaN, null, null)).valid());
    assertFalse(validate(itemWith(null, Double.POSITIVE_INFINITY, null)).valid());
    assertFalse(validate(itemWith(null, null, Double.NaN)).valid());
  }

  @Test
  void rejectsUnsupportedSpeedDepthOrbitAndHoldItems() {
    assertFalse(validate(itemWith(null, 10.0d, null)).valid());
    assertFalse(validate(itemWith(null, null, 2.0d)).valid());
    assertFalse(validate(positionItem(PlanItemType.ORBIT)).valid());
    assertFalse(validate(positionItem(PlanItemType.HOLD_POSITION)).valid());
  }

  @Test
  void invalidPlanCannotBeCompiled() {
    MissionPlan missionPlan =
        new MissionPlan(
            List.of(
                waypoint(
                    new GeoPosition(
                        -33.8688d,
                        151.2093d,
                        null,
                        null))));

    assertThrows(
        IllegalArgumentException.class,
        () -> model.buildMission(context(), missionPlan));
  }

  private PlanValidation validate(PlanItem item) {
    return model.validateMission(new MissionPlan(List.of(item)));
  }

  private static PlanItem waypoint(GeoPosition position) {
    return new PlanItem(
        PlanItemType.WAYPOINT,
        position,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static PlanItem positionItem(PlanItemType type) {
    return new PlanItem(
        type,
        new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
        null,
        50.0d,
        null,
        null,
        null,
        null);
  }

  private static PlanItem itemWith(
      Float yawDegrees,
      Double speedMetersPerSecond,
      Double depthMeters) {
    return new PlanItem(
        PlanItemType.WAYPOINT,
        new GeoPosition(-33.8688d, 151.2093d, 120.0d, null),
        null,
        null,
        yawDegrees,
        speedMetersPerSecond,
        null,
        depthMeters);
  }
  private static UxvCommandContext context() {
    UxvCommandContext context = mock(UxvCommandContext.class);
    when(context.targetSystem()).thenReturn(2);
    when(context.targetComponent()).thenReturn(1);
    when(context.sequence()).thenReturn(7);
    return context;
  }

}
