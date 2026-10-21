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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class MavlinkMissionItemIntTest {

  @Test
  void missionSequenceIsWrittenOnlyToPayloadSequence() {
    MavlinkMissionItemInt missionItem = new MavlinkMissionItemInt();
    missionItem.setMissionSequence(7);

    JsonObject json = missionItem.toMavlinkJsonObject();

    assertEquals(0, json.getAsJsonObject("header").get("sequence").getAsInt());
    assertEquals(7, json.getAsJsonObject("payload").get("seq").getAsInt());
  }

  @Test
  void jsonContainsCompleteMissionItemWithoutConflatingTransportSequence() {
    MavlinkMissionItemInt missionItem = new MavlinkMissionItemInt();
    missionItem.setTargetSystem(10);
    missionItem.setTargetComponent(1);
    missionItem.setMissionSequence(4);
    missionItem.setFrame(MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT);
    missionItem.setCommand(MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT);
    missionItem.setCurrent(0);
    missionItem.setAutocontinue(1);
    missionItem.setParam1(1.5f);
    missionItem.setParam2(3.0f);
    missionItem.setParam3(0.0f);
    missionItem.setParam4(Float.NaN);
    missionItem.setLatitude(-338_688_000);
    missionItem.setLongitude(1_512_093_000);
    missionItem.setAltitude(120.0f);
    missionItem.setMissionType(MavlinkMissionItemIntFactory.MAV_MISSION_TYPE_MISSION);

    JsonObject json = missionItem.toMavlinkJsonObject();
    JsonObject header = json.getAsJsonObject("header");
    JsonObject payload = json.getAsJsonObject("payload");

    assertEquals("V2", header.get("version").getAsString());
    assertEquals(0, header.get("sequence").getAsInt());
    assertEquals(MavlinkMissionItemInt.MESSAGE_ID_MISSION_ITEM_INT, header.get("messageId").getAsInt());

    assertEquals(10, payload.get("target_system").getAsInt());
    assertEquals(1, payload.get("target_component").getAsInt());
    assertEquals(4, payload.get("seq").getAsInt());
    assertEquals(MavlinkMissionItemIntFactory.MAV_FRAME_GLOBAL_INT, payload.get("frame").getAsInt());
    assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT, payload.get("command").getAsInt());
    assertEquals(0, payload.get("current").getAsInt());
    assertEquals(1, payload.get("autocontinue").getAsInt());
    assertEquals(1.5f, payload.get("param1").getAsFloat());
    assertEquals(3.0f, payload.get("param2").getAsFloat());
    assertEquals(0.0f, payload.get("param3").getAsFloat());
    assertTrue(Float.isNaN(payload.get("param4").getAsFloat()));
    assertEquals(-338_688_000, payload.get("x").getAsInt());
    assertEquals(1_512_093_000, payload.get("y").getAsInt());
    assertEquals(120.0f, payload.get("z").getAsFloat());
    assertEquals(
        MavlinkMissionItemIntFactory.MAV_MISSION_TYPE_MISSION,
        payload.get("mission_type").getAsInt());
  }
}
