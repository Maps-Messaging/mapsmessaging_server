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

package io.mapsmessaging.state.mavlink.model.impl.usv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandIntFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItem;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemFactory;
import io.mapsmessaging.state.mavlink.model.LoiterRequest;
import io.mapsmessaging.state.mavlink.model.RepositionRequest;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SticklebackArdupilotUsvModelTest {

  private static final UxvCommandContext CONTEXT = new UxvCommandContext(UUID.randomUUID(), 10, 1, 255, 190, 42);

  @Test
  void repositionUsesFixedRelativeAltitudeWithoutMutatingSurfacePosition() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, 123.0d, null);

    UxvModelCommandSet commandSet = model.reposition(CONTEXT, new RepositionRequest(position, null, null));

    assertEquals(3, commandSet.messages().size());

    MavlinkCommandInt reposition = assertInstanceOf(MavlinkCommandInt.class, commandSet.messages().get(0));
    assertEquals(MavlinkCommandIntFactory.MAV_CMD_DO_REPOSITION, reposition.getCommand());
    assertEquals(MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, reposition.getFrame());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, reposition.getAltitude());

    MavlinkCommandLong guidedMode = assertInstanceOf(MavlinkCommandLong.class, commandSet.messages().get(1));
    assertEquals(MavlinkCommandLongFactory.MAV_CMD_DO_SET_MODE, guidedMode.getCommand());
    assertEquals(MavlinkCommandLongFactory.ARDUPLANE_MODE_GUIDED, guidedMode.getParam2());

    MavlinkMissionItem guidedWaypoint = assertInstanceOf(MavlinkMissionItem.class, commandSet.messages().get(2));
    assertEquals(MavlinkMissionItemFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT, guidedWaypoint.getFrame());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, guidedWaypoint.getAltitude());
    assertEquals(2, guidedWaypoint.getCurrent());

    assertEquals(123.0d, position.getAltitudeMslMeters());
    assertNull(position.getAltitudeAglMeters());
  }

  @Test
  void unlimitedLoiterUsesFixedRelativeAltitudeWithoutMutatingPosition() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, 87.0d, null);

    UxvModelCommandSet commandSet = model.loiter(CONTEXT, new LoiterRequest(position, 25.0d, Duration.ZERO, null, null, null));

    MavlinkCommandInt loiter = assertInstanceOf(MavlinkCommandInt.class, commandSet.messages().get(0));
    assertEquals(MavlinkCommandIntFactory.MAV_CMD_NAV_LOITER_UNLIM, loiter.getCommand());
    assertEquals(MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, loiter.getFrame());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, loiter.getAltitude());
    assertEquals(87.0d, position.getAltitudeMslMeters());
  }

  @Test
  void timedLoiterUsesFixedRelativeAltitude() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    GeoPosition position = new GeoPosition(59.4673d, 24.828353d, null, null);

    UxvModelCommandSet commandSet = model.loiter(CONTEXT, new LoiterRequest(position, 25.0d, Duration.ofSeconds(30), null, null, null));

    MavlinkCommandInt loiter = assertInstanceOf(MavlinkCommandInt.class, commandSet.messages().get(0));
    assertEquals(MavlinkCommandIntFactory.MAV_CMD_NAV_LOITER_TIME, loiter.getCommand());
    assertEquals(MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, loiter.getFrame());
    assertEquals((float) SticklebackArdupilotUsvModel.MAX_ALTITUDE_METERS, loiter.getAltitude());
    assertEquals(30.0f, loiter.getParam1());
  }
}
