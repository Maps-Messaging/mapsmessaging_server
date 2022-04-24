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

package io.mapsmessaging.network.protocol.impl.mqtt_sn2.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import lombok.Getter;
import lombok.ToString;

@ToString
public class Unsubscribe extends MQTT_SN_2_Packet {

  @Getter
  private final int msgId;
  @Getter
  private int topicId;
  @Getter
  private String topicName;

  public Unsubscribe(Packet packet) {
    super(UNSUBSCRIBE);
    byte flags = packet.get();
    msgId = MQTTPacket.readShort(packet);
    int topicNameType = flags & 0b11;

    //ToDo: implement the different topic name types
    if (topicNameType == TOPIC_NAME_ALIAS) {
      byte[] tmp = new byte[packet.available()];
      packet.get(tmp, 0, tmp.length);
      topicName = new String(tmp);
    } else {
      topicId = MQTTPacket.readShort(packet);
    }
  }
}
