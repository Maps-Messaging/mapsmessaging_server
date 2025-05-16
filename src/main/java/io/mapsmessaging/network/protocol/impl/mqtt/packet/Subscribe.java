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

package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718063
 */
@java.lang.SuppressWarnings("common-java:DuplicatedBlocks") // MQTT 3/4 and MQTT5 share a lot in common, however, the changes don't make it easy to extend.
@Getter
public class Subscribe extends MQTTPacket {

  private final List<SubscriptionInfo> subscriptionList;
  @Setter
  private int messageId;

  public Subscribe() {
    super(SUBSCRIBE);
    subscriptionList = new ArrayList<>();
    messageId = 0;
  }

  public Subscribe(byte fixedHeader, long remainingLen, Packet packet) throws MalformedException {
    super(MQTTPacket.SUBSCRIBE);
    if ((fixedHeader & 0xf) != 2) {
      throw new MalformedException("Subscribe Fixed Header bits 3,2,1,0 must be set as 0,0,1,0  as per the specification : [MQTT-3.8.1-1]");
    }
    messageId = readShort(packet);
    subscriptionList = new ArrayList<>();
    int position = 2; // Include the short we read for the message ID
    while (position < remainingLen) {
      String topicFilter = readUTF8(packet);
      QualityOfService qos = QualityOfService.getInstance(packet.get() & 0b11);
      if (qos.equals(QualityOfService.MQTT_SN_REGISTERED)) {
        throw new MalformedException("QoS valid values are 0,1 or 2 any other values are considered a Malformed Packet : [MQTT-3-8.3-4]");
      }
      SubscriptionInfo info = new SubscriptionInfo(topicFilter, qos);
      subscriptionList.add(info);
      position += info.length() + 2; // 2 for the length of the string
    }
    if (subscriptionList.isEmpty()) {
      throw new MalformedException("Subscription request must have at least 1 topic / qos entry as per [MQTT-3.8.3-3]");
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MQTT Subscribe[");
    for (SubscriptionInfo info : subscriptionList) {
      sb.append(" TopicName:")
          .append(info.getTopicName())
          .append(" QoS:")
          .append(info.getQualityOfService());
    }

    sb.append("]");
    return sb.toString();
  }

  public int packFrame(Packet packet) {
    int length = 2;
    for (SubscriptionInfo info : subscriptionList) {
      length += info.length() + 2;
    }
    packControlByte(packet, 2);
    writeVariableInt(packet, length);
    writeShort(packet, messageId);
    for (SubscriptionInfo info : subscriptionList) {
      writeUTF8(packet, info.getTopicName());
      packet.put((byte) info.getQualityOfService().getLevel());
    }

    return 0;
  }

}
