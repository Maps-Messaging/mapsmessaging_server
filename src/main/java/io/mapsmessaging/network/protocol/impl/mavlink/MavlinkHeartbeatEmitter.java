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

package io.mapsmessaging.network.protocol.impl.mavlink;

import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_FAILED_SENDING_HEARTBEAT;

import com.google.gson.JsonObject;
import io.mapsmessaging.config.protocol.impl.MavlinkConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.schemas.config.impl.MavlinkSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class MavlinkHeartbeatEmitter implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(MavlinkHeartbeatEmitter.class);

  private static final String MAVLINK_V2 = "V2";

  private static final int HEARTBEAT_MESSAGE_ID = 0;
  private static final int MAV_TYPE_GCS = 6;
  private static final int MAV_AUTOPILOT_INVALID = 8;
  private static final int MAV_MODE_FLAG_CUSTOM_MODE_ENABLED = 128;
  private static final int MAV_MODE_FLAG_MANUAL_INPUT_ENABLED = 64;
  private static final int MAV_STATE_ACTIVE = 4;
  private static final int MAVLINK_VERSION = 3;

  private final EndPoint endPoint;
  private final MessageFormatter formatter;
  private final JsonObject input;
  private final JsonObject header;
  private final AtomicInteger sequence;

  public MavlinkHeartbeatEmitter( AtomicInteger sequenceCounter, EndPoint endPoint, MavlinkConfig mavlinkConfig) throws IOException {
    this.endPoint = Objects.requireNonNull(endPoint, "endPoint");
    Objects.requireNonNull(mavlinkConfig, "mavlinkConfig");

    MavlinkSchemaConfig config = new MavlinkSchemaConfig();
    config.setDialect(mavlinkConfig.getDialectName());
    formatter = MessageFormatterFactory.getInstance().getFormatter(config);

    sequence = sequenceCounter;
    header = createHeader(mavlinkConfig);
    input = new JsonObject();
    input.add("header", header);
    input.add("payload", createPayload());
  }

  @Override
  public void run() {
    try {
      updateSequence();
      byte[] frame = formatter.parseFromJson(input);
      Packet packet = new Packet(ByteBuffer.wrap(frame));
      endPoint.sendPacket(packet);
    } catch (IOException e) {
      logger.log(MAVLINK_FAILED_SENDING_HEARTBEAT, endPoint.getName(), e);
    }
  }

  private JsonObject createHeader(MavlinkConfig mavlinkConfig) {
    JsonObject json = new JsonObject();
    json.addProperty("version", MAVLINK_V2);
    json.addProperty("systemId", mavlinkConfig.getSystemId());
    json.addProperty("componentId", mavlinkConfig.getComponentId());
    json.addProperty("sequence", 0);
    json.addProperty("messageId", HEARTBEAT_MESSAGE_ID);
    json.addProperty("signed", false);
    json.addProperty("incompatibilityFlags", 0);
    json.addProperty("compatibilityFlags", 0);
    return json;
  }

  private JsonObject createPayload() {
    JsonObject json = new JsonObject();
    json.addProperty("custom_mode", 0);
    json.addProperty("type", MAV_TYPE_GCS);
    json.addProperty("autopilot", MAV_AUTOPILOT_INVALID);
    json.addProperty("base_mode", MAV_MODE_FLAG_CUSTOM_MODE_ENABLED | MAV_MODE_FLAG_MANUAL_INPUT_ENABLED);
    json.addProperty("system_status", MAV_STATE_ACTIVE);
    json.addProperty("mavlink_version", MAVLINK_VERSION);
    return json;
  }

  private void updateSequence() {
    header.addProperty("sequence", sequence.getAndUpdate(value -> (value + 1) & 0xff));
  }
}