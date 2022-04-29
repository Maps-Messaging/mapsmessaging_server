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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.BasePublish;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class Publish extends MQTT_SN_2_Packet implements BasePublish {

  @Getter
  private final int topicId;
  @Getter
  private final String topicName;
  @Getter
  private final int messageId;
  @Getter
  private final byte[] message;
  @Getter
  private boolean dup;
  @Getter @Setter
  private QualityOfService QoS;
  @Getter @Setter
  private boolean retain;

  public Publish(short topicId, int messageId, byte[] message) {
    super(PUBLISH);
    this.topicId = topicId;
    this.messageId = messageId;
    this.message = message;
    this.topicName = null;
  }

  public Publish(Packet packet, int length) throws IOException {
    super(PUBLISH);
    byte flags = packet.get();
    dup = (flags & 0b10000000) != 0;
    QoS = QualityOfService.getInstance((flags & 0b01100000) >> 5);
    retain = (flags & 0b00010000) != 0;
    int topicIdType = flags & 0b11;

    int topicLength = MQTTPacket.readShort(packet);
    messageId = MQTTPacket.readShort(packet);

    if (topicIdType == TOPIC_LONG_NAME) {
      byte[] tmp = new byte[topicLength];
      packet.get(tmp, 0, topicLength);
      topicName = new String(tmp);
      topicId = -1;
    } else {
      topicId = (short) MQTTPacket.readShort(packet);
      topicName = null;
    }
    message = new byte[packet.available()];
    packet.get(message, 0, message.length);
  }

  @Override
  public int packFrame(Packet packet) {
    int len = 9 + message.length;
    if (len < 256) {
      packet.put((byte) len);
    } else {
      packet.put((byte) 1);
      MQTTPacket.writeShort(packet, len+2);
    }
    packet.put((byte) PUBLISH);
    packet.put(packFlag());
    MQTTPacket.writeShort(packet, 2);
    MQTTPacket.writeShort(packet, messageId);
    MQTTPacket.writeShort(packet, topicId);
    packet.put(message);
    return len;
  }

  byte packFlag(){
      byte f = (byte) ( (byte) ((QoS.getLevel() & 0b11) << 5));
      int type = MQTT_SN_2_Packet.TOPIC_LONG_NAME;
      if(topicName == null){
        type = MQTT_SN_2_Packet.TOPIC_ALIAS;
      }
      f = (byte) (f | (type & 0b11));
      return f;
  }
}
