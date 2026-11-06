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

package io.mapsmessaging.state.mavlink.messages;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MavlinkCommandIntTest {

  @Test
  void defaultsDescribeCommandIntMessage() {
    MavlinkCommandInt command = new MavlinkCommandInt();

    assertEquals("COMMAND_INT", command.getMessageType());
    assertEquals(MavlinkCommandInt.MESSAGE_ID_COMMAND_INT, command.getMessageId());
  }

  @Test
  void jsonContainsConfiguredHeaderAndPayloadFields() {
    MavlinkCommandInt command = new MavlinkCommandInt();
    command.setTargetSystem(3);
    command.setTargetComponent(4);
    command.setFrame(6);
    command.setCommand(400);
    command.setCurrent(0);
    command.setAutocontinue(1);
    command.setSequence(17);
    command.setParam1(1.5f);
    command.setParam2(2.5f);
    command.setParam3(3.5f);
    command.setParam4(4.5f);
    command.setLatitude(384261947);
    command.setLongitude(-90737520);
    command.setAltitude(12.75f);

    JsonObject root = command.toMavlinkJsonObject();
    JsonObject header = root.getAsJsonObject("header");
    JsonObject payload = root.getAsJsonObject("payload");

    assertEquals("V2", header.get("version").getAsString());
    assertEquals(17, header.get("sequence").getAsInt());
    assertEquals(75, header.get("messageId").getAsInt());
    assertFalse(header.get("signed").getAsBoolean());

    assertEquals(3, payload.get("target_system").getAsInt());
    assertEquals(4, payload.get("target_component").getAsInt());
    assertEquals(6, payload.get("frame").getAsInt());
    assertEquals(400, payload.get("command").getAsInt());
    assertEquals(0, payload.get("current").getAsInt());
    assertEquals(1, payload.get("autocontinue").getAsInt());
    assertEquals(1.5f, payload.get("param1").getAsFloat(), 0.0001f);
    assertEquals(2.5f, payload.get("param2").getAsFloat(), 0.0001f);
    assertEquals(3.5f, payload.get("param3").getAsFloat(), 0.0001f);
    assertEquals(4.5f, payload.get("param4").getAsFloat(), 0.0001f);
    assertEquals(384261947, payload.get("x").getAsInt());
    assertEquals(-90737520, payload.get("y").getAsInt());
    assertEquals(12.75f, payload.get("z").getAsFloat(), 0.0001f);
  }
}
