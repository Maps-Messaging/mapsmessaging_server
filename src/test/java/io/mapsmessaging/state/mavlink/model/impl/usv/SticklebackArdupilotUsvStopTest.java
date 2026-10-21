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
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.mavlink.messages.MavlinkCommandInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandIntFactory;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SticklebackArdupilotUsvStopTest {

  @Test
  void stopUsesUnlimitedLoiterAtCurrentPosition() {
    SticklebackArdupilotUsvModel model = new SticklebackArdupilotUsvModel();
    UxvCommandContext context =
        new UxvCommandContext(
            UUID.fromString("a1eef2cf-7dc2-5655-9aed-9ed6cee64345"),
            10,
            1,
            255,
            190,
            23);

    UxvModelCommandSet commandSet = model.stop(context);

    assertEquals(UxvOperation.STOP, commandSet.operation());
    assertEquals(SticklebackArdupilotUsvModel.MODEL_NAME, commandSet.modelName());
    assertEquals(1, commandSet.messages().size());
    assertTrue(commandSet.messages().get(0) instanceof MavlinkCommandInt);

    MavlinkCommandInt command = (MavlinkCommandInt) commandSet.messages().get(0);
    assertEquals(MavlinkCommandInt.MESSAGE_ID_COMMAND_INT, command.getMessageId());
    assertEquals(MavlinkCommandIntFactory.MAV_CMD_NAV_LOITER_UNLIM, command.getCommand());
    assertEquals(MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, command.getFrame());
    assertEquals(10, command.getTargetSystem());
    assertEquals(1, command.getTargetComponent());
    assertEquals(23, command.getSequence());
    assertEquals(0, command.getLatitude());
    assertEquals(0, command.getLongitude());
    assertEquals(0.0f, command.getAltitude());
  }
}
