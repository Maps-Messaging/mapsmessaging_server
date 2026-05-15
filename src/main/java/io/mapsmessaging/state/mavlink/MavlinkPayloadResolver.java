/*
 *
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

package io.mapsmessaging.state.mavlink;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_STATE_JSON_PACKET_DETECTED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_STATE_JSON_PARSE_FAILED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_STATE_RAW_PACKET_DETECTED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_STATE_UNKNOWN_PACKET_IGNORED;

public class MavlinkPayloadResolver {

  private static final int MAVLINK_V1_MAGIC = 0xFE;
  private static final int MAVLINK_V2_MAGIC = 0xFD;

  private final Logger logger = LoggerFactory.getLogger(MavlinkPayloadResolver.class);

  private final MessageFormatter mavlinkFormatter;

  public MavlinkPayloadResolver(@NonNull @NotNull MessageFormatter mavlinkFormatter) {
    this.mavlinkFormatter = mavlinkFormatter;
  }

  public byte[] resolve(String sourceName, byte[] opaqueData) {
    if (opaqueData == null || opaqueData.length == 0) {
      logger.log(MAVLINK_STATE_UNKNOWN_PACKET_IGNORED, sourceName);
      return null;
    }

    if (isMavlinkFrame(opaqueData)) {
      logger.log(MAVLINK_STATE_RAW_PACKET_DETECTED, sourceName);
      return opaqueData;
    }

    logger.log(MAVLINK_STATE_JSON_PACKET_DETECTED, sourceName);

    return parseJsonMavlinkPayload(sourceName, opaqueData);
  }

  private byte[] parseJsonMavlinkPayload(String sourceName, byte[] opaqueData) {
    try {
      String jsonText = new String(opaqueData, StandardCharsets.UTF_8);
      JsonElement element = JsonParser.parseString(jsonText);

      if (!element.isJsonObject()) {
        logger.log(MAVLINK_STATE_UNKNOWN_PACKET_IGNORED, sourceName);
        return null;
      }

      JsonObject json = element.getAsJsonObject();

      if (!json.has("mavlink") || !json.get("mavlink").isJsonObject()) {
        logger.log(MAVLINK_STATE_UNKNOWN_PACKET_IGNORED, sourceName);
        return null;
      }

      JsonObject mavlink = json.getAsJsonObject("mavlink");

      return mavlinkFormatter.parseFromJson(mavlink);
    } catch (RuntimeException | IOException exception) {
      logger.log(MAVLINK_STATE_JSON_PARSE_FAILED, sourceName);
      return null;
    }
  }

  private boolean isMavlinkFrame(byte[] payload) {
    int firstByte = payload[0] & 0xFF;

    return firstByte == MAVLINK_V1_MAGIC || firstByte == MAVLINK_V2_MAGIC;
  }
}