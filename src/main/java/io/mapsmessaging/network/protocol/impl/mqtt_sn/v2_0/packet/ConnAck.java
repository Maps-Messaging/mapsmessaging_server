/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import lombok.Getter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;

@ToString
public class ConnAck extends MQTT_SN_2_Packet {

  @Getter
  private final int reasonCode;
  @Getter
  private final long sessionExpiry;
  @Getter
  private final String clientId;

  private final boolean sessionPresent;

  public ConnAck(ReasonCodes reasonCode, long sessionExpiry, String clientId, boolean sessionPresent) {
    super(CONNACK);
    this.reasonCode = reasonCode.getValue();
    this.sessionExpiry = sessionExpiry;
    this.clientId = clientId;
    this.sessionPresent = sessionPresent;
  }

  @Override
  public int packFrame(Packet packet) {
    int len = 8;
    if (clientId != null) {
      len += clientId.length();
    }
    len = packLength(packet, len);
    packet.put((byte) CONNACK);
    packet.put((byte) reasonCode);
    packet.put((sessionPresent ? (byte) 1 : 0));
    MQTTPacket.writeInt(packet, sessionExpiry);
    if (clientId != null) {
      packet.put(clientId.getBytes(StandardCharsets.UTF_8));
    }
    return len;
  }

}
