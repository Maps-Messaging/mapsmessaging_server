/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.mqtt.packet;

import java.util.ArrayList;
import java.util.List;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.network.io.Packet;

/**
 * http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718063
 */
public class Subscribe extends MQTTPacket {

  private final List<SubscriptionInfo> subscriptionList;
  private final int messageId;

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

  public int getMessageId() {
    return messageId;
  }

  public List<SubscriptionInfo> getSubscriptionList() {
    return subscriptionList;
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
    return 0;
  }
}
