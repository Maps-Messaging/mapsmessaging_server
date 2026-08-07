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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.mavlink.ProcessedFrame;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SysStatusPacketHealthTest {

  @Test
  void areEnabledSensorsHealthy_whenHealthContainsAdditionalDisabledSensors_returnsTrue() {
    SysStatusPacket packet = packet(Map.of("onboard_control_sensors_enabled", 0x03, "onboard_control_sensors_health", 0x07));

    assertTrue(packet.areEnabledSensorsHealthy());
    assertEquals(0L, packet.getUnhealthyEnabledSensorsMask());
  }

  @Test
  void areEnabledSensorsHealthy_whenEnabledSensorIsUnhealthy_returnsFalse() {
    SysStatusPacket packet = packet(Map.of("onboard_control_sensors_enabled", 0x07, "onboard_control_sensors_health", 0x03));

    assertFalse(packet.areEnabledSensorsHealthy());
    assertEquals(0x04L, packet.getUnhealthyEnabledSensorsMask());
  }

  @Test
  void areEnabledExtendedSensorsHealthy_usesExtendedHealthMask() {
    SysStatusPacket packet = packet(Map.of("onboard_control_sensors_enabled_extended", 0x03, "onboard_control_sensors_health_extended", 0x01));

    assertFalse(packet.areEnabledExtendedSensorsHealthy());
    assertEquals(0x02L, packet.getUnhealthyEnabledSensorsExtendedMask());
  }

  private SysStatusPacket packet(Map<String, Object> healthFields) {
    Map<String, Object> fields = new HashMap<>(healthFields);
    ProcessedFrame frame = mock(ProcessedFrame.class);
    when(frame.getFields()).thenReturn(fields);
    when(frame.isValid()).thenReturn(true);
    return new SysStatusPacket(frame);
  }
}
