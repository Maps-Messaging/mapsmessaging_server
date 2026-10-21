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

package io.mapsmessaging.state.mavlink.model.impl.ardupilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemInt;
import io.mapsmessaging.state.mavlink.model.MissionPlan;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanItemType;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.impl.usv.SticklebackArdupilotUsvModel;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GenericArduPilotMissionPlaceholderTest {

  @Test
  void missionSequenceZeroDoesNotCopyFirstTaskWaypoint() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    UxvCommandContext context = new UxvCommandContext(UUID.randomUUID(), 1, 1, 103, 190, 0);
    GeoPosition firstWaypoint = new GeoPosition(38.42328682771597, -9.151574667936899, 100.0, null);
    PlanItem firstItem =
        new PlanItem(
            PlanItemType.WAYPOINT,
            firstWaypoint,
            Duration.ZERO,
            2.0,
            null,
            null,
            null,
            null);

    UxvModelCommandSet commandSet = model.buildMission(context, new MissionPlan(List.of(firstItem)));

    assertEquals(2, commandSet.messages().size());
    MavlinkMissionItemInt placeholder =
        assertInstanceOf(MavlinkMissionItemInt.class, commandSet.messages().get(0));
    MavlinkMissionItemInt realWaypoint =
        assertInstanceOf(MavlinkMissionItemInt.class, commandSet.messages().get(1));

    assertEquals(0, placeholder.getMissionSequence());
    assertEquals(0, placeholder.getLatitude());
    assertEquals(0, placeholder.getLongitude());
    assertEquals(0.0f, placeholder.getAltitude());

    assertEquals(1, realWaypoint.getMissionSequence());
    assertEquals(384232868, realWaypoint.getLatitude());
    assertEquals(-91515747, realWaypoint.getLongitude());
  }
}
