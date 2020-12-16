/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.protocol.impl.mqtt_sn.packet;

import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;

public class Unsubscribe extends MQTT_SNPacket {

  private final int msgId;
  private int topicId;
  private String topicName;

  public Unsubscribe(Packet packet) {
    super(UNSUBSCRIBE);
    flags = packet.get();
    msgId = MQTTPacket.readShort(packet);
    if (topicIdType() == TOPIC_NAME) {
      byte[] tmp = new byte[packet.available()];
      packet.get(tmp, 0, tmp.length);
      topicName = new String(tmp);
    } else {
      topicId = MQTTPacket.readShort(packet);
    }
  }

  public int getMsgId() {
    return msgId;
  }

  public int getTopicId() {
    return topicId;
  }

  public String getTopicName() {
    return topicName;
  }

  @Override
  public String toString() {
    return "Unsubscribe:TopicName:" + topicName + " TopicId:" + topicId + " MessageId:" + msgId + " " + super.toString();
  }
}
