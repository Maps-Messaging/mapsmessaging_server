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

package io.mapsmessaging.state.mavlink.model.impl.uav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandIntFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItem;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemFactory;
import io.mapsmessaging.state.mavlink.model.RepositionRequest;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GenericPx4UavModelTest {

  private static final UxvCommandContext CONTEXT = new UxvCommandContext(UUID.randomUUID(), 2, 1, 255, 190, 17);

  @Test
  void repositionKeepsThreeMessageQgcCompatibleSequenceWithMslAltitude() {
    GenericPx4UavModel model = new GenericPx4UavModel();
    GeoPosition position = new GeoPosition(-33.8688d, 151.2093d, 120.0d, null);

    UxvModelCommandSet commandSet = model.reposition(CONTEXT, new RepositionRequest(position, null, null));

    assertEquals(3, commandSet.messages().size());

    MavlinkCommandInt reposition = assertInstanceOf(MavlinkCommandInt.class, commandSet.messages().get(0));
    assertEquals(MavlinkCommandIntFactory.MAV_CMD_DO_REPOSITION, reposition.getCommand());
    assertEquals(MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_INT, reposition.getFrame());
    assertEquals(120.0f, reposition.getAltitude());

    MavlinkCommandLong guidedMode = assertInstanceOf(MavlinkCommandLong.class, commandSet.messages().get(1));
    assertEquals(MavlinkCommandLongFactory.MAV_CMD_DO_SET_MODE, guidedMode.getCommand());
    assertEquals(MavlinkCommandLongFactory.ARDUPLANE_MODE_GUIDED, guidedMode.getParam2());

    MavlinkMissionItem guidedWaypoint = assertInstanceOf(MavlinkMissionItem.class, commandSet.messages().get(2));
    assertEquals(MavlinkMissionItemFactory.MAV_CMD_NAV_WAYPOINT, guidedWaypoint.getCommand());
    assertEquals(MavlinkMissionItemFactory.MAV_FRAME_GLOBAL, guidedWaypoint.getFrame());
    assertEquals(2, guidedWaypoint.getCurrent());
    assertEquals(120.0f, guidedWaypoint.getAltitude());
  }

  @Test
  void repositionKeepsTerrainAltitudeCoherentAcrossBothPositionMessages() {
    GenericPx4UavModel model = new GenericPx4UavModel();
    GeoPosition position = new GeoPosition(-33.8688d, 151.2093d, null, 35.0d);

    UxvModelCommandSet commandSet = model.reposition(CONTEXT, new RepositionRequest(position, null, null));

    MavlinkCommandInt reposition = assertInstanceOf(MavlinkCommandInt.class, commandSet.messages().get(0));
    assertEquals(MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_TERRAIN_ALT_INT, reposition.getFrame());
    assertEquals(35.0f, reposition.getAltitude());

    MavlinkMissionItem guidedWaypoint = assertInstanceOf(MavlinkMissionItem.class, commandSet.messages().get(2));
    assertEquals(MavlinkMissionItemFactory.MAV_FRAME_GLOBAL_TERRAIN_ALT, guidedWaypoint.getFrame());
    assertEquals(35.0f, guidedWaypoint.getAltitude());
  }
}
