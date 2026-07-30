/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.state.mavlink.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionCount;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionCountFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import java.util.List;
import org.junit.jupiter.api.Test;

class MavlinkCommandSetPreparerTest {

  @Test
  void prepareMissionPrependsCanonicalMissionCount() {
    MavlinkMissionItemInt first = waypoint(0, 59.4673d, 24.828353d);
    MavlinkMissionItemInt second = waypoint(1, 59.4680d, 24.8290d);
    UxvModelCommandSet commandSet = UxvModelCommandSet.of(UxvOperation.BUILD_MISSION, "test-model", List.of(first, second));

    PreparedMavlinkCommandSet prepared = MavlinkCommandSetPreparer.prepare(commandSet);

    assertEquals(3, prepared.commandSet().messages().size());
    MavlinkMissionCount count = assertInstanceOf(MavlinkMissionCount.class, prepared.commandSet().messages().get(0));
    assertEquals(10, count.getTargetSystem());
    assertEquals(1, count.getTargetComponent());
    assertEquals(2, count.getCount());
    assertSame(first, prepared.commandSet().messages().get(1));
    assertSame(second, prepared.commandSet().messages().get(2));
    assertInstanceOf(MavlinkMissionAcknowledgementHandler.class, prepared.acknowledgementHandler());
  }

  @Test
  void prepareMissionAcceptsAlreadyFramedMission() {
    MavlinkMissionCount count = MavlinkMissionCountFactory.mission(10, 1, 1);
    MavlinkMissionItemInt item = waypoint(0, 59.4673d, 24.828353d);
    UxvModelCommandSet commandSet = UxvModelCommandSet.of(UxvOperation.BUILD_MISSION, "test-model", List.of(count, item));

    PreparedMavlinkCommandSet prepared = MavlinkCommandSetPreparer.prepare(commandSet);

    assertSame(count, prepared.commandSet().messages().get(0));
    assertSame(item, prepared.commandSet().messages().get(1));
  }

  @Test
  void prepareMissionRejectsSequenceGaps() {
    MavlinkMissionItemInt item = waypoint(1, 59.4673d, 24.828353d);
    UxvModelCommandSet commandSet = UxvModelCommandSet.of(UxvOperation.BUILD_MISSION, "test-model", List.of(item));

    assertThrows(IllegalArgumentException.class, () -> MavlinkCommandSetPreparer.prepare(commandSet));
  }

  @Test
  void prepareCommandUsesCommandAcknowledgementHandler() {
    UxvModelCommandSet commandSet = UxvModelCommandSet.empty(UxvOperation.STOP, "test-model");

    PreparedMavlinkCommandSet prepared = MavlinkCommandSetPreparer.prepare(commandSet);

    assertSame(commandSet, prepared.commandSet());
    assertInstanceOf(MavlinkCommandAcknowledgementHandler.class, prepared.acknowledgementHandler());
  }

  private MavlinkMissionItemInt waypoint(int sequence, double latitude, double longitude) {
    return MavlinkMissionItemIntFactory.waypoint(10, 1, sequence, new GeoPosition(latitude, longitude, 20.0d, null));
  }
}
