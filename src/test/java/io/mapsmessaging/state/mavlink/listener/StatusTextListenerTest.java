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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.packet.StatusTextPacket;
import org.junit.jupiter.api.Test;

class StatusTextListenerTest {

  @Test
  void handle_updatesDroneTwinWithLatestStatusText() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("test-drone");
    twinManager.registerTwin(droneTwin, null);

    StatusTextPacket packet = mock(StatusTextPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.getText()).thenReturn("  Ready to fly  ");

    new StatusTextListener(twinManager).handle(droneTwin.getTwinId(), packet, null);

    assertEquals("Ready to fly", droneTwin.getLastStatusText());
    assertNotNull(droneTwin.getOperationalUpdatedAt());
  }

  @Test
  void handle_ignoresBlankStatusText() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("test-drone");
    droneTwin.setLastStatusText("Existing status");
    twinManager.registerTwin(droneTwin, null);

    StatusTextPacket packet = mock(StatusTextPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.getText()).thenReturn("   ");

    new StatusTextListener(twinManager).handle(droneTwin.getTwinId(), packet, null);

    assertEquals("Existing status", droneTwin.getLastStatusText());
  }
}
