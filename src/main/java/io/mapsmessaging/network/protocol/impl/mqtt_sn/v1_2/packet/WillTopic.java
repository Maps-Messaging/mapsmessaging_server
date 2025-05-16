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
import lombok.Getter;
import lombok.ToString;

@ToString
public class WillTopic extends MQTT_SNPacket {

  @Getter
  private final String topic;
  private byte flags;

  public WillTopic(Packet packet, int length) {
    super(WILLTOPIC);
    flags = packet.get();
    byte[] topicBuffer = new byte[length - 3];
    packet.get(topicBuffer, 0, topicBuffer.length);
    topic = new String(topicBuffer);
  }

  @Override
  public int packFrame(Packet packet) {
    int len = packLength(packet, 3 + topic.length());
    packet.put((byte) WILLTOPIC);
    packet.put(flags);
    packet.put(topic.getBytes());
    return len;
  }

  public boolean getRetain() {
    return (flags & 0b00010000) != 0;
  }

  public QualityOfService getQoS() {
    return QualityOfService.getInstance((flags & 0b01100000) >> 5);
  }

  public void setQoS(QualityOfService qos) {
    flags = (byte) (flags | (byte) ((qos.getLevel() & 0b11) << 5));
  }
}
