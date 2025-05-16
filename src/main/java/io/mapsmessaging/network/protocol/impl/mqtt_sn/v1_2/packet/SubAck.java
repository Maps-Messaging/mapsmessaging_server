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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import lombok.Getter;
import lombok.ToString;

@ToString
public class SubAck extends MQTT_SNPacket {

  @Getter
  private final int msgId;
  @Getter
  private final int topicId;
  @Getter
  private final ReasonCodes status;

  private byte flags;

  public SubAck(short topicId, int msgId, ReasonCodes status) {
    super(SUBACK);
    this.msgId = msgId;
    this.topicId = topicId;
    this.status = status;
    this.flags = 0;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 8);
    packet.put((byte) SUBACK);
    packet.put(flags); // Only QOS counts
    MQTTPacket.writeShort(packet, topicId);
    MQTTPacket.writeShort(packet, msgId);
    packet.put((byte) status.getValue());
    return 8;
  }

  public QualityOfService getQoS() {
    return QualityOfService.getInstance((flags & 0b01100000) >> 5);
  }

  public void setQoS(QualityOfService qos) {
    flags = (byte) (flags | (byte) ((qos.getLevel() & 0b11) << 5));
  }

}
