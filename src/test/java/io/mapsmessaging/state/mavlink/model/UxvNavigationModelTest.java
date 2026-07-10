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

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModel;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvNavigationPlan;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.model.impl.AbstractMissionUxvModel;
import io.mapsmessaging.state.mavlink.model.impl.uav.GenericPx4UavModel;
import io.mapsmessaging.state.mavlink.model.impl.ugv.GenericPx4UgvModel;
import io.mapsmessaging.state.mavlink.model.impl.usv.SticklebackArdupilotUsvModel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UxvNavigationModelTest {

  private static final int TARGET_SYSTEM = 10;
  private static final int TARGET_COMPONENT = 1;
  private static final int SEQUENCE = 42;

  @Test
  void ugvBuildsNavigationPlanWithoutAltitude() {
    GenericPx4UgvModel model = new GenericPx4UgvModel();
    List<GeoPosition> waypoints = List.of(
        position(-33.8688, 151.2093),
        position(-33.8695, 151.2102));
    Duration duration = Duration.ofMinutes(3);

    UxvNavigationPlan plan = model.navigate(context(), waypoints, duration);

    assertNavigationPlan(model, plan, waypoints.size(), duration);
  }

  @Test
  void usvBuildsNavigationPlanWithoutAltitude() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    List<GeoPosition> waypoints = List.of(
        position(59.434079, 24.747487),
        position(59.434550, 24.748200));

    UxvNavigationPlan plan = model.navigate(context(), waypoints, null);

    assertNavigationPlan(model, plan, waypoints.size(), Duration.ZERO);
    assertFalse(plan.hasTimeout());
  }

  @Test
  void uavBuildsNavigationPlanWithMslAltitude() {
    GenericPx4UavModel model = new GenericPx4UavModel();
    List<GeoPosition> waypoints = List.of(
        new GeoPosition(-33.8688, 151.2093, 120.0, null),
        new GeoPosition(-33.8695, 151.2102, 125.0, null));

    UxvNavigationPlan plan = model.navigate(context(), waypoints, Duration.ofSeconds(90));

    assertNavigationPlan(model, plan, waypoints.size(), Duration.ofSeconds(90));
  }

  @Test
  void uavBuildsNavigationPlanWithAglAltitude() {
    GenericPx4UavModel model = new GenericPx4UavModel();
    List<GeoPosition> waypoints = List.of(
        new GeoPosition(-33.8688, 151.2093, null, 40.0),
        new GeoPosition(-33.8695, 151.2102, null, 45.0));

    UxvNavigationPlan plan = model.navigate(context(), waypoints, Duration.ZERO);

    assertNavigationPlan(model, plan, waypoints.size(), Duration.ZERO);
    assertFalse(plan.hasTimeout());
  }

  @Test
  void navigateRejectsNullContext() {
    assertThrows(
        NullPointerException.class,
        () -> new GenericPx4UgvModel().navigate(
            null,
            List.of(position(-33.8688, 151.2093)),
            Duration.ZERO));
  }

  @Test
  void navigateRejectsNullWaypointList() {
    assertThrows(
        NullPointerException.class,
        () -> new GenericPx4UgvModel().navigate(context(), null, Duration.ZERO));
  }

  @Test
  void navigateRejectsEmptyWaypointList() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GenericPx4UgvModel().navigate(context(), List.of(), Duration.ZERO));
  }

  @Test
  void navigateRejectsNullWaypoint() {
    List<GeoPosition> waypoints = new ArrayList<>();
    waypoints.add(position(-33.8688, 151.2093));
    waypoints.add(null);

    assertThrows(
        NullPointerException.class,
        () -> new GenericPx4UgvModel().navigate(context(), waypoints, Duration.ZERO));
  }

  @Test
  void navigateRejectsNegativeDuration() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GenericPx4UgvModel().navigate(
            context(),
            List.of(position(-33.8688, 151.2093)),
            Duration.ofMillis(-1)));
  }

  @Test
  void navigateRejectsMissingLatitude() {
    assertInvalidPosition(new GeoPosition(null, 151.2093, null, null));
  }

  @Test
  void navigateRejectsMissingLongitude() {
    assertInvalidPosition(new GeoPosition(-33.8688, null, null, null));
  }

  @Test
  void navigateRejectsLatitudeBelowMinimum() {
    assertInvalidPosition(position(-90.0001, 151.2093));
  }

  @Test
  void navigateRejectsLatitudeAboveMaximum() {
    assertInvalidPosition(position(90.0001, 151.2093));
  }

  @Test
  void navigateRejectsLongitudeBelowMinimum() {
    assertInvalidPosition(position(-33.8688, -180.0001));
  }

  @Test
  void navigateRejectsLongitudeAboveMaximum() {
    assertInvalidPosition(position(-33.8688, 180.0001));
  }

  @Test
  void navigateRejectsNonFiniteLatitude() {
    assertInvalidPosition(position(Double.NaN, 151.2093));
    assertInvalidPosition(position(Double.POSITIVE_INFINITY, 151.2093));
  }

  @Test
  void navigateRejectsNonFiniteLongitude() {
    assertInvalidPosition(position(-33.8688, Double.NaN));
    assertInvalidPosition(position(-33.8688, Double.NEGATIVE_INFINITY));
  }

  @Test
  void uavNavigateRejectsMissingAltitude() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GenericPx4UavModel().navigate(
            context(),
            List.of(position(-33.8688, 151.2093)),
            Duration.ZERO));
  }

  @Test
  void uavNavigateRejectsNonFiniteMslAltitude() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GenericPx4UavModel().navigate(
            context(),
            List.of(new GeoPosition(-33.8688, 151.2093, Double.NaN, null)),
            Duration.ZERO));
  }

  @Test
  void uavNavigateRejectsNonFiniteAglAltitude() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GenericPx4UavModel().navigate(
            context(),
            List.of(new GeoPosition(-33.8688, 151.2093, null, Double.POSITIVE_INFINITY)),
            Duration.ZERO));
  }

  private void assertNavigationPlan(
      AbstractMissionUxvModel model,
      UxvNavigationPlan plan,
      int expectedWaypointCount,
      Duration expectedDuration) {

    assertNotNull(plan);
    assertEquals(expectedDuration, plan.duration());

    assertEquals(1, plan.missionPhase().size());

    UxvModelCommandSet missionPhase = plan.missionPhase().getFirst();
    assertEquals(UxvOperation.BUILD_MISSION, missionPhase.operation());
    assertEquals(model.getModelName(), missionPhase.modelName());
    assertEquals(expectedWaypointCount, missionPhase.messages().size());

    assertEquals(1, plan.postMissionUploadPhase().size());

    UxvModelCommandSet postMissionUploadPhase =
        plan.postMissionUploadPhase().getFirst();

    assertEquals(UxvOperation.START_MISSION, postMissionUploadPhase.operation());

    assertEquals(model.getModelName(), postMissionUploadPhase.modelName());
    assertEquals(1, postMissionUploadPhase.messages().size());

    UxvModelCommandSet terminalAction = plan.terminalAction();

    assertEquals(UxvOperation.PAUSE_VEHICLE, terminalAction.operation());

    assertEquals(model.getModelName(), terminalAction.modelName());
    assertEquals(1, terminalAction.messages().size());
  }

  private static void assertInvalidPosition(GeoPosition position) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GenericPx4UgvModel().navigate(
            context(),
            List.of(position),
            Duration.ZERO));
  }

  private static GeoPosition position(Double latitude, Double longitude) {
    return new GeoPosition(latitude, longitude, null, null);
  }

  private static UxvCommandContext context() {
    UxvCommandContext context = mock(UxvCommandContext.class);
    when(context.targetSystem()).thenReturn(TARGET_SYSTEM);
    when(context.targetComponent()).thenReturn(TARGET_COMPONENT);
    when(context.sequence()).thenReturn(SEQUENCE);
    return context;
  }
}