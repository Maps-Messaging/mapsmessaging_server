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
import lombok.Getter;
import lombok.ToString;

@ToString
public class Register extends MQTT_SN_2_Packet {

  @Getter
  private final int topicId;
  @Getter
  private final int messageId;
  @Getter
  private final String topic;

  public Register(short topicId, short messageId, String topic) {
    super(REGISTER);
    this.topicId = topicId;
    this.messageId = messageId;
    this.topic = topic;
  }

  public Register(Packet packet, int length) {
    super(REGISTER);
    topicId = MQTTPacket.readShort(packet);
    messageId = MQTTPacket.readShort(packet);
    byte[] topicBuffer = new byte[length - 6];
    packet.get(topicBuffer, 0, topicBuffer.length);
    topic = new String(topicBuffer);
  }

  @Override
  public int packFrame(Packet packet) {
    int len = packLength(packet, 6 + topic.length());
    packet.put((byte) REGISTER);
    MQTTPacket.writeShort(packet, topicId);
    MQTTPacket.writeShort(packet, messageId);
    packet.put(topic.getBytes());
    return len;
  }
}
