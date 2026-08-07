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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.packet.SysStatusPacket;
import org.junit.jupiter.api.Test;

class SysStatusListenerHealthTest {

  @Test
  void handle_whenAllEnabledSensorsAreHealthy_marksSystemHealthy() {
    DroneTwin droneTwin = new DroneTwin("test-drone");
    TwinManager twinManager = new TwinManager();
    twinManager.registerTwin(droneTwin, null);

    SysStatusPacket packet = packet(true, true, 0, 0);
    new SysStatusListener(twinManager).handle(droneTwin.getTwinId(), packet, null);

    assertTrue(droneTwin.getSystemState().getHealthy());
    assertEquals("System health nominal", droneTwin.getSystemState().getStatusMessage());
  }

  @Test
  void handle_whenEnabledSensorIsUnhealthy_marksSystemDegradedWithMask() {
    DroneTwin droneTwin = new DroneTwin("test-drone");
    TwinManager twinManager = new TwinManager();
    twinManager.registerTwin(droneTwin, null);

    SysStatusPacket packet = packet(false, true, 0x20, 0);
    new SysStatusListener(twinManager).handle(droneTwin.getTwinId(), packet, null);

    assertFalse(droneTwin.getSystemState().getHealthy());
    assertEquals("System health degraded: unhealthy enabled sensors 0x20", droneTwin.getSystemState().getStatusMessage());
  }

  @Test
  void handle_whenEnabledExtendedSensorIsUnhealthy_marksSystemDegradedWithExtendedMask() {
    DroneTwin droneTwin = new DroneTwin("test-drone");
    TwinManager twinManager = new TwinManager();
    twinManager.registerTwin(droneTwin, null);

    SysStatusPacket packet = packet(true, false, 0, 0x02);
    new SysStatusListener(twinManager).handle(droneTwin.getTwinId(), packet, null);

    assertFalse(droneTwin.getSystemState().getHealthy());
    assertEquals("System health degraded: unhealthy enabled extended sensors 0x2", droneTwin.getSystemState().getStatusMessage());
  }

  private SysStatusPacket packet(boolean basicHealthy, boolean extendedHealthy, long basicMask, long extendedMask) {
    SysStatusPacket packet = mock(SysStatusPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.getLoadPercent()).thenReturn(Double.NaN);
    when(packet.getVoltageVolts()).thenReturn(Double.NaN);
    when(packet.getCurrentAmps()).thenReturn(Double.NaN);
    when(packet.getRemainingPercent()).thenReturn(Double.NaN);
    when(packet.areEnabledSensorsHealthy()).thenReturn(basicHealthy);
    when(packet.areEnabledExtendedSensorsHealthy()).thenReturn(extendedHealthy);
    when(packet.getUnhealthyEnabledSensorsMask()).thenReturn(basicMask);
    when(packet.getUnhealthyEnabledSensorsExtendedMask()).thenReturn(extendedMask);
    return packet;
  }
}
