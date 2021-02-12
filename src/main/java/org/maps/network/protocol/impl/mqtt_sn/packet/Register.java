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

package org.maps.network.protocol.impl.mqtt_sn.packet;

import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;

public class Register extends MQTT_SNPacket {

  private final int topicId;
  private final int messageId;
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

  public short getTopicId() {
    return (short) topicId;
  }

  public int getMessageId() {
    return messageId;
  }

  public String getTopic() {
    return topic;
  }

  @Override
  public int packFrame(Packet packet) {
    int len = 6 + topic.length();
    packet.put((byte) len);
    packet.put((byte) REGISTER);
    MQTTPacket.writeShort(packet, topicId);
    MQTTPacket.writeShort(packet, messageId);
    packet.put(topic.getBytes());
    return len;
  }

  @Override
  public String toString() {
    return "Register:TopicId:" + topicId + " MessageId:" + messageId + " Topic:" + topic;
  }
}
