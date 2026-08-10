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
import io.mapsmessaging.state.mavlink.packet.MissionItemReachedPacket;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MissionItemReachedListenerTest {

  @Test
  void handle_whenMissionItemIsReached_updatesDedicatedTwinState() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("mavlink:test:1");
    twinManager.registerTwin(droneTwin, new TwinUpdateContext());

    Instant receivedAt = Instant.parse("2026-07-30T03:05:00Z");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(receivedAt);

    MissionItemReachedPacket packet = packet(0, true);
    new MissionItemReachedListener(twinManager).handle("mavlink:test:1", packet, context);

    DroneTwin updated = (DroneTwin) twinManager.getTwin("mavlink:test:1").orElseThrow();
    assertEquals(MavlinkMessageIds.MISSION_ITEM_REACHED, packet.getMessageId());
    assertEquals(0, updated.getLastMissionItemReachedSequence());
    assertEquals(receivedAt, updated.getLastMissionItemReachedAt());
    assertEquals(receivedAt, updated.getOperationalUpdatedAt());
    assertNull(updated.getCurrentMissionSequence());
  }

  @Test
  void handle_whenFrameIsInvalid_doesNotUpdateTwin() {
    TwinManager twinManager = new TwinManager();
    DroneTwin droneTwin = new DroneTwin("mavlink:test:1");
    twinManager.registerTwin(droneTwin, new TwinUpdateContext());

    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-07-30T03:05:00Z"));

    new MissionItemReachedListener(twinManager)
        .handle("mavlink:test:1", packet(2, false), context);

    DroneTwin updated = (DroneTwin) twinManager.getTwin("mavlink:test:1").orElseThrow();
    assertNull(updated.getLastMissionItemReachedSequence());
    assertNull(updated.getLastMissionItemReachedAt());
  }

  private MissionItemReachedPacket packet(int sequence, boolean valid) {
    ProcessedFrame frame =
        new ProcessedFrame(
            "MISSION_ITEM_REACHED",
            null,
            Map.of("seq", sequence),
            valid,
            List.of(),
            null);
    return new MissionItemReachedPacket(frame);
  }
}
