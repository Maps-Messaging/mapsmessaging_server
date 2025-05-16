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
public class Subscribe extends MQTT_SNPacket {

  @Getter
  private final int msgId;
  @Getter
  private String topicName;
  @Getter
  private short topicId;
  @Getter
  private final boolean dup;
  @Getter
  private final int topicIdType;

  private final int QoS;

  private byte flags;

  public Subscribe(Packet packet) {
    super(SUBSCRIBE);
    flags = packet.get();

    dup = (flags & 0b10000000) != 0;
    QoS = (flags & 0b01100000) >> 5;
    topicIdType = (flags & 0b11);

    msgId = MQTTPacket.readShort(packet);
    if (topicIdType == TOPIC_NAME) {
      byte[] tmp = new byte[packet.available()];
      packet.get(tmp, 0, tmp.length);
      topicName = new String(tmp);
    } else {
      topicId = (short) MQTTPacket.readShort(packet);
    }
  }

  public boolean dup() {
    return (flags & 0b10000000) != 0;
  }

  public void setDup(boolean set) {
    if (set) {
      flags = (byte) (flags | 0b10000000);
    }
  }

  public QualityOfService getQoS() {
    return QualityOfService.getInstance((flags & 0b01100000) >> 5);
  }

  public void setQoS(QualityOfService qos) {
    flags = (byte) (flags | (byte) ((qos.getLevel() & 0b11) << 5));
  }
}
