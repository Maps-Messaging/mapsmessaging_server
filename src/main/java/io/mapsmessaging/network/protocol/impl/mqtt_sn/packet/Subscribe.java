/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

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

  public Subscribe(Packet packet) {
    super(SUBSCRIBE);
    flags = packet.get();
    msgId = MQTTPacket.readShort(packet);
    if (topicIdType() == TOPIC_NAME) {
      byte[] tmp = new byte[packet.available()];
      packet.get(tmp, 0, tmp.length);
      topicName = new String(tmp);
    } else {
      topicId = (short) MQTTPacket.readShort(packet);
    }
  }
}
