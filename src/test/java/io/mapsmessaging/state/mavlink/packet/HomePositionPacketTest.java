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

import io.mapsmessaging.mavlink.ProcessedFrame;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HomePositionPacketTest {

  @Test
  void rawCoordinatesAreConvertedToDegreesAndMetres() {
    HomePositionPacket packet = new HomePositionPacket(frame(Map.of(
        "latitude", 384261947,
        "longitude", -90737520,
        "altitude", 12345
    ), true));

    assertEquals(MavlinkMessageIds.HOME_POSITION, packet.getMessageId());
    assertEquals(38.4261947, packet.getLatitude(), 0.0000001);
    assertEquals(-9.073752, packet.getLongitude(), 0.0000001);
    assertEquals(12.345, packet.getAltitudeMeters(), 0.000001);
    assertTrue(packet.isValid());
  }

  @Test
  void sentinelCoordinatesAndAltitudeBecomeNaN() {
    HomePositionPacket minusOne = new HomePositionPacket(frame(Map.of(
        "latitude", -1,
        "longitude", -1,
        "altitude", -1
    ), true));

    assertTrue(Double.isNaN(minusOne.getLatitude()));
    assertTrue(Double.isNaN(minusOne.getLongitude()));
    assertTrue(Double.isNaN(minusOne.getAltitudeMeters()));

    HomePositionPacket minValue = new HomePositionPacket(frame(Map.of(
        "latitude", Integer.MIN_VALUE,
        "longitude", Integer.MIN_VALUE,
        "altitude", Integer.MIN_VALUE
    ), false));

    assertTrue(Double.isNaN(minValue.getLatitude()));
    assertTrue(Double.isNaN(minValue.getLongitude()));
    assertTrue(Double.isNaN(minValue.getAltitudeMeters()));
    assertFalse(minValue.isValid());
  }

  private static ProcessedFrame frame(Map<String, Object> fields, boolean valid) {
    return new ProcessedFrame("HOME_POSITION", null, fields, valid, List.of(), null);
  }
}
