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

public class PubAck extends MQTT_SNPacket {

  private final int status;
  private final int topicId;
  private final int messageId;

  public PubAck(int topicId, int messageId, int status) {
    super(PUBACK);
    this.topicId = topicId;
    this.messageId = messageId;
    this.status = status;
  }

  public PubAck(Packet packet) {
    super(PUBACK);
    topicId = MQTTPacket.readShort(packet);
    messageId = MQTTPacket.readShort(packet);
    status = packet.get();
  }

  public int getStatus() {
    return status;
  }

  public int getTopicId() {
    return topicId;
  }

  public int getMessageId() {
    return messageId;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 7);
    packet.put((byte) PUBACK);
    MQTTPacket.writeShort(packet, topicId);
    MQTTPacket.writeShort(packet, messageId);
    packet.put((byte) status);
    return 7;
  }

  @Override
  public String toString() {
    return "PubAck:TopicId:" + topicId + " MessageId:" + messageId + " Status:" + status;
  }

}
