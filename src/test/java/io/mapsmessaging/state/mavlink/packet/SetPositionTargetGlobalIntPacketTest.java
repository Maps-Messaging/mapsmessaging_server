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

package io.mapsmessaging.state.mavlink.packet;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SetPositionTargetGlobalIntPacketTest {

  @Test
  void positionOnlyConstructorProducesExpectedFields() {
    SetPositionTargetGlobalIntPacket packet = new SetPositionTargetGlobalIntPacket(
        1,
        0,
        38.4261947,
        -9.0737520,
        42.5,
        MavlinkVerticalReference.RELATIVE_TO_HOME
    );

    assertEquals(SetPositionTargetGlobalIntPacket.MESSAGE_NAME, packet.getMessageName());
    assertEquals(6, packet.getCoordinateFrame());
    assertEquals(384261947, packet.getLatitudeInt());
    assertEquals(-90737520, packet.getLongitudeInt());
    assertEquals(SetPositionTargetGlobalIntPacket.POSITION_ONLY_TYPE_MASK, packet.getTypeMask());

    Map<String, Object> fields = packet.toFields();
    assertEquals(0L, fields.get("time_boot_ms"));
    assertEquals(1, fields.get("target_system"));
    assertEquals(0, fields.get("target_component"));
    assertEquals(6, fields.get("coordinate_frame"));
    assertEquals(384261947, fields.get("lat_int"));
    assertEquals(-90737520, fields.get("lon_int"));
    assertEquals(42.5, fields.get("alt"));
    assertEquals(0.0, fields.get("vx"));
    assertEquals(0.0, fields.get("yaw_rate"));
  }

  @Test
  void supportedVerticalReferencesMapToExpectedFrames() {
    assertEquals(5, packetFor(MavlinkVerticalReference.MEAN_SEA_LEVEL).getCoordinateFrame());
    assertEquals(6, packetFor(MavlinkVerticalReference.RELATIVE_TO_HOME).getCoordinateFrame());
    assertEquals(11, packetFor(MavlinkVerticalReference.TERRAIN).getCoordinateFrame());
  }

  @Test
  void targetIdentifiersEnforceMavlinkRanges() {
    assertDoesNotThrow(() -> packet(1, 0, 0.0, 0.0));
    assertDoesNotThrow(() -> packet(255, 255, 0.0, 0.0));

    assertThrows(IllegalArgumentException.class, () -> packet(0, 0, 0.0, 0.0));
    assertThrows(IllegalArgumentException.class, () -> packet(256, 0, 0.0, 0.0));
    assertThrows(IllegalArgumentException.class, () -> packet(1, -1, 0.0, 0.0));
    assertThrows(IllegalArgumentException.class, () -> packet(1, 256, 0.0, 0.0));
  }

  @Test
  void latitudeAndLongitudeEnforceGeodeticRanges() {
    assertDoesNotThrow(() -> packet(1, 1, -90.0, -180.0));
    assertDoesNotThrow(() -> packet(1, 1, 90.0, 180.0));

    assertThrows(IllegalArgumentException.class, () -> packet(1, 1, -90.000001, 0.0));
    assertThrows(IllegalArgumentException.class, () -> packet(1, 1, 90.000001, 0.0));
    assertThrows(IllegalArgumentException.class, () -> packet(1, 1, 0.0, -180.000001));
    assertThrows(IllegalArgumentException.class, () -> packet(1, 1, 0.0, 180.000001));
    assertThrows(IllegalArgumentException.class, () -> packet(1, 1, Double.NaN, 0.0));
    assertThrows(IllegalArgumentException.class, () -> packet(1, 1, 0.0, Double.POSITIVE_INFINITY));
  }

  @Test
  void verticalReferenceMustBeExplicitAndSupported() {
    assertThrows(IllegalArgumentException.class, () ->
        new SetPositionTargetGlobalIntPacket(1, 1, 0, 0, 10, null));
    assertThrows(IllegalArgumentException.class, () ->
        new SetPositionTargetGlobalIntPacket(1, 1, 0, 0, 10, MavlinkVerticalReference.WGS84_ELLIPSOID));
    assertThrows(IllegalArgumentException.class, () ->
        new SetPositionTargetGlobalIntPacket(1, 1, 0, 0, 10, MavlinkVerticalReference.DEPTH_BELOW_SURFACE));
  }

  @Test
  void allFloatingPointCommandValuesMustBeFinite() {
    assertThrows(IllegalArgumentException.class, () ->
        new SetPositionTargetGlobalIntPacket(1, 1, 0, 0, Double.NaN, MavlinkVerticalReference.MEAN_SEA_LEVEL));

    assertThrows(IllegalArgumentException.class, () -> fullPacket(Double.NaN, 0, 0, 0, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> fullPacket(0, Double.POSITIVE_INFINITY, 0, 0, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> fullPacket(0, 0, Double.NEGATIVE_INFINITY, 0, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> fullPacket(0, 0, 0, Double.NaN, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> fullPacket(0, 0, 0, 0, Double.NaN, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> fullPacket(0, 0, 0, 0, 0, Double.NaN, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> fullPacket(0, 0, 0, 0, 0, 0, Double.NaN, 0));
    assertThrows(IllegalArgumentException.class, () -> fullPacket(0, 0, 0, 0, 0, 0, 0, Double.NaN));
  }

  private static SetPositionTargetGlobalIntPacket packetFor(MavlinkVerticalReference reference) {
    return new SetPositionTargetGlobalIntPacket(1, 1, 0, 0, 10, reference);
  }

  private static SetPositionTargetGlobalIntPacket packet(
      int systemId,
      int componentId,
      double latitude,
      double longitude
  ) {
    return new SetPositionTargetGlobalIntPacket(
        systemId,
        componentId,
        latitude,
        longitude,
        10,
        MavlinkVerticalReference.MEAN_SEA_LEVEL
    );
  }

  private static SetPositionTargetGlobalIntPacket fullPacket(
      double vx,
      double vy,
      double vz,
      double ax,
      double ay,
      double az,
      double yaw,
      double yawRate
  ) {
    return new SetPositionTargetGlobalIntPacket(
        123L,
        1,
        1,
        MavlinkVerticalReference.MEAN_SEA_LEVEL,
        0,
        0,
        0,
        10,
        vx,
        vy,
        vz,
        ax,
        ay,
        az,
        yaw,
        yawRate
    );
  }
}
