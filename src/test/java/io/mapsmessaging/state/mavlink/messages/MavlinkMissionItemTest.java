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

class MavlinkMissionItemTest {

  @Test
  void defaultsDescribeMissionItemMessage() {
    MavlinkMissionItem item = new MavlinkMissionItem();

    assertEquals("MISSION_ITEM", item.getMessageType());
    assertEquals(MavlinkMissionItem.MESSAGE_ID_MISSION_ITEM, item.getMessageId());
  }

  @Test
  void jsonContainsConfiguredHeaderAndPayloadFields() {
    MavlinkMissionItem item = new MavlinkMissionItem();
    item.setTargetSystem(1);
    item.setTargetComponent(2);
    item.setMissionSequence(7);
    item.setFrame(3);
    item.setCommand(16);
    item.setCurrent(1);
    item.setAutocontinue(1);
    item.setSequence(42);
    item.setParam1(1.1f);
    item.setParam2(2.2f);
    item.setParam3(3.3f);
    item.setParam4(4.4f);
    item.setLatitude(38.4f);
    item.setLongitude(-9.1f);
    item.setAltitude(55.5f);
    item.setMissionType(0);

    JsonObject root = item.toMavlinkJsonObject();
    JsonObject header = root.getAsJsonObject("header");
    JsonObject payload = root.getAsJsonObject("payload");

    assertEquals("V2", header.get("version").getAsString());
    assertEquals(0, header.get("systemId").getAsInt());
    assertEquals(0, header.get("componentId").getAsInt());
    assertEquals(42, header.get("sequence").getAsInt());
    assertEquals(39, header.get("messageId").getAsInt());
    assertFalse(header.get("signed").getAsBoolean());

    assertEquals(1, payload.get("target_system").getAsInt());
    assertEquals(2, payload.get("target_component").getAsInt());
    assertEquals(7, payload.get("seq").getAsInt());
    assertEquals(3, payload.get("frame").getAsInt());
    assertEquals(16, payload.get("command").getAsInt());
    assertEquals(1, payload.get("current").getAsInt());
    assertEquals(1, payload.get("autocontinue").getAsInt());
    assertEquals(1.1f, payload.get("param1").getAsFloat(), 0.0001f);
    assertEquals(2.2f, payload.get("param2").getAsFloat(), 0.0001f);
    assertEquals(3.3f, payload.get("param3").getAsFloat(), 0.0001f);
    assertEquals(4.4f, payload.get("param4").getAsFloat(), 0.0001f);
    assertEquals(38.4f, payload.get("x").getAsFloat(), 0.0001f);
    assertEquals(-9.1f, payload.get("y").getAsFloat(), 0.0001f);
    assertEquals(55.5f, payload.get("z").getAsFloat(), 0.0001f);
    assertEquals(0, payload.get("mission_type").getAsInt());
  }
}
