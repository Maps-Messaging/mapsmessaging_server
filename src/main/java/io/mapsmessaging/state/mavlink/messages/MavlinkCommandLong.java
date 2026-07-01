/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License.
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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MavlinkCommandLong implements MavlinkMessage {

  public static final int MESSAGE_ID_COMMAND_LONG = 76;

  private String messageType = "COMMAND_LONG";
  private int messageId = MESSAGE_ID_COMMAND_LONG;
  private int targetSystem;
  private int targetComponent;
  private int command;
  private int confirmation;
  private int sequence;
  private float param1;
  private float param2;
  private float param3;
  private float param4;
  private float param5;
  private float param6;
  private float param7;

  public JsonObject toMavlinkJsonObject(int systemId, int componentId) {
    JsonObject root = new JsonObject();
    JsonObject header = new JsonObject();
    JsonObject payload = new JsonObject();

    header.addProperty("version", "V2");
    header.addProperty("systemId", systemId);
    header.addProperty("componentId", componentId);
    header.addProperty("sequence", sequence);
    header.addProperty("messageId", messageId);
    header.addProperty("signed", false);
    header.addProperty("incompatibilityFlags", 0);
    header.addProperty("compatibilityFlags", 0);

    payload.addProperty("target_system", targetSystem);
    payload.addProperty("target_component", targetComponent);
    payload.addProperty("command", command);
    payload.addProperty("confirmation", confirmation);
    payload.addProperty("param1", param1);
    payload.addProperty("param2", param2);
    payload.addProperty("param3", param3);
    payload.addProperty("param4", param4);
    payload.addProperty("param5", param5);
    payload.addProperty("param6", param6);
    payload.addProperty("param7", param7);

    root.add("header", header);
    root.add("payload", payload);

    return root;
  }
}