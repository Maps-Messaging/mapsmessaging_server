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

package io.mapsmessaging.state.mavlink.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.model.VelocityVector;
import io.mapsmessaging.state.mavlink.packet.GlobalPositionPacket;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GlobalPositionListenerTest {

  private static final Instant NOW = Instant.parse("2026-09-06T12:00:00Z");

  @Test
  void incompleteGlobalPositionClearsUnavailableNumericState() {
    TwinManager twinManager = new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
    TwinUpdateContext context = context();
    DroneTwin twin = new DroneTwin("drone-1");
    twin.setHeadingDegrees(123.0d);
    twin.setCourseOverGroundDegrees(120.0d);
    twin.setGroundSpeedMetersPerSecond(8.0d);
    twin.setVerticalSpeedMetersPerSecond(1.5d);
    twin.setVelocityVector(new VelocityVector(7.0d, 3.0d, -1.5d));
    twinManager.registerTwin(twin, context);

    ProcessedFrame frame = mock(ProcessedFrame.class);
    when(frame.getFields()).thenReturn(Map.of(
        "lat", 384321000,
        "lon", -91034000,
        "alt", 12500
    ));
    when(frame.isValid()).thenReturn(true);

    GlobalPositionPacket packet = new GlobalPositionPacket(frame);
    new GlobalPositionListener(twinManager).handle("drone-1", packet, context);

    assertEquals(38.4321d, twin.getGeoPosition().getLatitude(), 0.0000001d);
    assertEquals(-9.1034d, twin.getGeoPosition().getLongitude(), 0.0000001d);
    assertEquals(12.5d, twin.getGeoPosition().getAltitudeMslMeters(), 0.0000001d);
    assertNull(twin.getHeadingDegrees());
    assertNull(twin.getCourseOverGroundDegrees());
    assertNull(twin.getGroundSpeedMetersPerSecond());
    assertNull(twin.getVerticalSpeedMetersPerSecond());
    assertNull(twin.getVelocityVector().getNorthMetersPerSecond());
    assertNull(twin.getVelocityVector().getEastMetersPerSecond());
    assertNull(twin.getVelocityVector().getDownMetersPerSecond());
    assertEquals(NOW, twin.getNavigationUpdatedAt());
  }

  @Test
  void finiteGlobalPositionPopulatesDerivedNavigationState() {
    DroneTwin twin = processVelocity(300, 400);

    assertEquals(90.0d, twin.getHeadingDegrees(), 0.0000001d);
    assertEquals(53.1301024d, twin.getCourseOverGroundDegrees(), 0.0000001d);
    assertEquals(5.0d, twin.getGroundSpeedMetersPerSecond(), 0.0000001d);
    assertEquals(0.5d, twin.getVerticalSpeedMetersPerSecond(), 0.0000001d);
    assertEquals(3.0d, twin.getVelocityVector().getNorthMetersPerSecond(), 0.0000001d);
    assertEquals(4.0d, twin.getVelocityVector().getEastMetersPerSecond(), 0.0000001d);
    assertEquals(-0.5d, twin.getVelocityVector().getDownMetersPerSecond(), 0.0000001d);
  }

  @Test
  void fusedVelocityDeterminesNormalisedCourseOverGround() {
    assertEquals(0.0d, processVelocity(300, 0).getCourseOverGroundDegrees(), 0.0000001d);
    assertEquals(90.0d, processVelocity(0, 300).getCourseOverGroundDegrees(), 0.0000001d);
    assertEquals(180.0d, processVelocity(-300, 0).getCourseOverGroundDegrees(), 0.0000001d);
    assertEquals(270.0d, processVelocity(0, -300).getCourseOverGroundDegrees(), 0.0000001d);
    assertEquals(45.0d, processVelocity(300, 300).getCourseOverGroundDegrees(), 0.0000001d);
  }

  @Test
  void zeroHorizontalVelocityHasNoCourseOverGround() {
    DroneTwin twin = processVelocity(0, 0);

    assertNull(twin.getCourseOverGroundDegrees());
    assertEquals(90.0d, twin.getHeadingDegrees(), 0.0000001d);
    assertEquals(0.0d, twin.getGroundSpeedMetersPerSecond(), 0.0000001d);
  }

  private DroneTwin processVelocity(int northCentimetresPerSecond, int eastCentimetresPerSecond) {
    TwinManager twinManager = new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
    TwinUpdateContext context = context();
    DroneTwin twin = new DroneTwin("drone-1");
    twinManager.registerTwin(twin, context);

    ProcessedFrame frame = mock(ProcessedFrame.class);
    when(frame.getFields()).thenReturn(Map.of(
        "lat", 384321000,
        "lon", -91034000,
        "alt", 12500,
        "vx", northCentimetresPerSecond,
        "vy", eastCentimetresPerSecond,
        "vz", -50,
        "hdg", 9000
    ));
    when(frame.isValid()).thenReturn(true);

    new GlobalPositionListener(twinManager).handle("drone-1", new GlobalPositionPacket(frame), context);
    return twin;
  }

  private TwinUpdateContext context() {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(NOW);
    return context;
  }
}
