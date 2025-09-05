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

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.BasePublish;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;

@ToString
public class Publish extends MQTT_SN_2_Packet implements BasePublish {

  @Getter
  private final int topicId;
  @Getter
  private final String topicName;
  @Getter
  private final int messageId;
  @Getter
  @Setter
  private int topicIdType;
  @Getter
  private final byte[] message;
  @Getter
  private boolean dup;
  @Getter
  @Setter
  private QualityOfService QoS;
  @Getter
  @Setter
  private boolean retain;

  public Publish(short topicId, int messageId, byte[] message) {
    super(PUBLISH);
    this.topicId = topicId;
    this.messageId = messageId;
    this.message = message;
    this.topicName = null;
    topicIdType = TOPIC_NAME;
  }

  public Publish(Packet packet) throws IOException {
    super(PUBLISH);
    byte flags = packet.get();
    dup = (flags & 0b10000000) != 0;
    int qos = (flags & 0b01100000) >> 5;
    QoS = QualityOfService.getInstance(qos);
    boolean receiveMessageId = qos == 1 || qos == 2;

    retain = (flags & 0b00010000) != 0;
    topicIdType = flags & 0b11;
    if(receiveMessageId) {
      messageId = MQTTPacket.readShort(packet);
    }
    else{
      messageId =0;
    }
    if (topicIdType == LONG_TOPIC_NAME) {
      try {
        topicName = MQTTPacket.readUTF8(packet);
      } catch (MalformedException e) {
        throw new IOException("Invalid UTF-8 encoding for topic name", e);
      }
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
    boolean sendMessageId = getQoS().getLevel() == 1 || getQoS().getLevel() == 2;
    boolean sendTopicNameLength = topicIdType == LONG_TOPIC_NAME;
    int len = 5 + message.length;
    if(sendMessageId) len += 2;
    if(sendTopicNameLength) len += 2;

    len = packLength(packet, len);
    packet.put((byte) PUBLISH);
    packet.put(packFlag());
    if(sendMessageId) MQTTPacket.writeShort(packet, messageId);
    if(topicIdType == LONG_TOPIC_NAME){
      MQTTPacket.writeUTF8(packet, topicName);
    }
    else{
      MQTTPacket.writeShort(packet, topicId);
    }
    packet.put(message);
    return len;
  }

  byte packFlag() {
    byte f = ((byte) ((QoS.getLevel() & 0b11) << 5));
    f = (byte) (f | (topicIdType & 0b11));
    return f;
  }
}
