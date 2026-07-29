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
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MavlinkMissionCount implements MavlinkMessage {

  public static final int MESSAGE_ID_MISSION_COUNT = 44;

  private static final int TRANSPORT_SEQUENCE_PLACEHOLDER = 0;

  private String messageType = "MISSION_COUNT";
  private int messageId = MESSAGE_ID_MISSION_COUNT;
  private int targetSystem;
  private int targetComponent;
  private int count;
  private int missionType;

  @Override
  public int getCommand() {
    return 0;
  }

  @Override
  public JsonObject toMavlinkJsonObject() {
    JsonObject root = new JsonObject();
    JsonObject header = new JsonObject();
    JsonObject payload = new JsonObject();

    header.addProperty("version", "V2");
    header.addProperty("systemId", 0);
    header.addProperty("componentId", 0);
    header.addProperty("sequence", TRANSPORT_SEQUENCE_PLACEHOLDER);
    header.addProperty("messageId", messageId);
    header.addProperty("signed", false);
    header.addProperty("incompatibilityFlags", 0);
    header.addProperty("compatibilityFlags", 0);

    payload.addProperty("target_system", targetSystem);
    payload.addProperty("target_component", targetComponent);
    payload.addProperty("count", count);
    payload.addProperty("mission_type", missionType);

    root.add("header", header);
    root.add("payload", payload);

    return root;
  }
}
