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

import java.io.IOException;

@ToString
public class Publish extends MQTT_SNPacket implements BasePublish {

  @Getter
  private final int topicId;
  @Getter
  private final int messageId;
  @Getter
  private final byte[] message;
  private byte flags;

  public Publish(short topicId, int messageId, byte[] message) {
    super(PUBLISH);
    this.topicId = topicId;
    this.messageId = messageId;
    this.message = message;
  }

  public Publish(Packet packet, int length) throws IOException {
    super(PUBLISH);
    flags = packet.get();
    topicId = MQTTPacket.readShort(packet);
    messageId = MQTTPacket.readShort(packet);
    message = new byte[packet.available()];

    packet.get(message, 0, message.length);
  }

  @Override
  public int packFrame(Packet packet) {
    int len = packLength(packet, 7 + message.length);
    packet.put((byte) PUBLISH);
    packet.put(flags);
    MQTTPacket.writeShort(packet, topicId);
    MQTTPacket.writeShort(packet, messageId);
    packet.put(message);
    return len;
  }

  public boolean dup() {
    return (flags & 0b10000000) != 0;
  }

  public QualityOfService getQoS() {
    return QualityOfService.getInstance((flags & 0b01100000) >> 5);
  }

  public void setQoS(QualityOfService qos) {
    flags = (byte) (flags | (byte) ((qos.getLevel() & 0b11) << 5));
  }

  public boolean retain() {
    return (flags & 0b00010000) != 0;
  }

  public int getTopicIdType() {
    return (flags & 0b00000011);
  }

  public void setTopicIdType(int type) {
    flags = (byte) (flags | (type & 0b11));
  }
}
