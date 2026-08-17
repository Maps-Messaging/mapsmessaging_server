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

import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds;
import io.mapsmessaging.state.mavlink.packet.MissionCurrentPacket;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MissionCurrentListenerTest {

  @Test
  void handle_whenMissionCurrentArrives_updatesCorrelatedMissionObservation() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("mavlink:test:1");
    twinManager.registerTwin(droneTwin, new TwinUpdateContext());

    Instant receivedAt = Instant.parse("2026-08-17T04:10:00Z");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(receivedAt);

    MissionCurrentPacket packet =
        packet(
            Map.of(
                "seq", 2,
                "total", 4,
                "mission_state", 3,
                "mission_id", 14523L),
            true);
    new MissionCurrentListener(twinManager)
        .handle("mavlink:test:1", packet, context);

    DroneTwin updated =
        (DroneTwin) twinManager.getTwin("mavlink:test:1").orElseThrow();
    assertEquals(MavlinkMessageIds.MISSION_CURRENT, packet.getMessageId());
    assertEquals(2, updated.getCurrentMissionSequence());
    assertEquals(4, updated.getCurrentMissionTotal());
    assertEquals(3, updated.getCurrentMissionStateCode());
    assertEquals(14523L, updated.getCurrentMissionId());
    assertEquals("MISSION_ACTIVE", updated.getMissionState());
    assertEquals(receivedAt, updated.getCurrentMissionUpdatedAt());
    assertEquals(receivedAt, updated.getOperationalUpdatedAt());
  }

  @Test
  void handle_whenMavlinkOneOmitsExtensionFields_retainsActiveSequenceObservation() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("mavlink:test:1");
    twinManager.registerTwin(droneTwin, new TwinUpdateContext());

    Instant receivedAt = Instant.parse("2026-08-17T04:10:00Z");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(receivedAt);

    new MissionCurrentListener(twinManager)
        .handle("mavlink:test:1", packet(Map.of("seq", 1), true), context);

    DroneTwin updated =
        (DroneTwin) twinManager.getTwin("mavlink:test:1").orElseThrow();
    assertEquals(1, updated.getCurrentMissionSequence());
    assertEquals("MISSION_ACTIVE", updated.getMissionState());
    assertEquals(receivedAt, updated.getCurrentMissionUpdatedAt());
    assertNull(updated.getCurrentMissionTotal());
    assertNull(updated.getCurrentMissionStateCode());
    assertNull(updated.getCurrentMissionId());
  }

  private MissionCurrentPacket packet(Map<String, Object> fields, boolean valid) {
    ProcessedFrame frame =
        new ProcessedFrame(
            "MISSION_CURRENT",
            null,
            fields,
            valid,
            List.of(),
            null);
    return new MissionCurrentPacket(frame);
  }
}
