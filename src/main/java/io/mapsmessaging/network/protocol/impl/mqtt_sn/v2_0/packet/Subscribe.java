/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import java.io.IOException;
import lombok.Getter;
import lombok.ToString;

@ToString
public class Subscribe extends MQTT_SN_2_Packet {

  @Getter private final int msgId;
  @Getter private final String topicName;
  @Getter private final boolean noLocal;
  @Getter private final QualityOfService QoS;
  @Getter private final boolean retain;
  @Getter private final RetainHandler retainHandler;
  @Getter private final int topicIdType;
  @Getter private final int topicId;

  public Subscribe(Packet packet) throws IOException {
    super(SUBSCRIBE);
    byte flags = packet.get();
    noLocal = (flags & 0b10000000) != 0;
    QoS = QualityOfService.getInstance((flags & 0b01100000) >> 5);
    retain = (flags & 0b00010000) != 0;
    retainHandler = RetainHandler.getInstance((flags & 0b00001100) >> 2);
    topicIdType = (flags & 0b11);
    msgId = MQTTPacket.readShort(packet);
    if (topicIdType == LONG_TOPIC_NAME || topicIdType == TOPIC_NAME) {
      byte[] tmp = new byte[packet.available()];
      packet.get(tmp);
      topicName = new String(tmp);
      topicId = -1;
    } else {
      topicId = (short) MQTTPacket.readShort(packet);
      topicName = null;
    }
  }
}
