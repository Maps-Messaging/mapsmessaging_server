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
import io.mapsmessaging.state.drone.model.autopilot.GenericAutopilotState;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinMissingState;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinReadinessEvaluator;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinReadinessResult;
import io.mapsmessaging.state.mavlink.packet.AutopilotVersionPacket;
import org.junit.jupiter.api.Test;

class AutopilotVersionListenerTest {

  @Test
  void autopilot_version_response_populates_version_and_capabilities() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("drone-1");
    GenericAutopilotState autopilotState = new GenericAutopilotState();
    autopilotState.setAutopilotType("ARDUPILOTMEGA");
    droneTwin.setAutopilotState(autopilotState);
    twinManager.registerTwin(droneTwin, null);

    DroneTwinReadinessResult before = new DroneTwinReadinessEvaluator().evaluate(droneTwin, null);
    assertTrue(before.getMissingStates().contains(DroneTwinMissingState.MISSING_AUTOPILOT_VERSION));
    assertTrue(before.getMissingStates().contains(DroneTwinMissingState.MISSING_CAPABILITIES));

    AutopilotVersionPacket packet = mock(AutopilotVersionPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.getUid()).thenReturn(0x1234L);
    when(packet.getFlightSoftwareVersion()).thenReturn(0x040500FFL);
    when(packet.getMiddlewareSoftwareVersion()).thenReturn(0x010200FFL);
    when(packet.getOsSoftwareVersion()).thenReturn(0x060100FFL);
    when(packet.getCapabilities()).thenReturn(59_647L);

    new AutopilotVersionListener(twinManager).handle(droneTwin.getTwinId(), packet, null);

    DroneTwinReadinessResult after = new DroneTwinReadinessEvaluator().evaluate(droneTwin, null);
    assertEquals(0x1234L, droneTwin.getAutopilotState().getUid());
    assertEquals(59_647L, droneTwin.getAutopilotState().getCapabilities());
    assertFalse(after.getMissingStates().contains(DroneTwinMissingState.MISSING_AUTOPILOT_VERSION));
    assertFalse(after.getMissingStates().contains(DroneTwinMissingState.MISSING_CAPABILITIES));
  }
}
