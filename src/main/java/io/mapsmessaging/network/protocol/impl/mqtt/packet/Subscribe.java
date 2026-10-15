/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
    if (messageId == 0) {
      throw new MalformedException("SUBSCRIBE Packet Identifier must not be zero [MQTT-2.3.1-1]");
    }
    subscriptionList = new ArrayList<>();
    int start = packet.position();
    while (packet.position() - start < remainingLen - 2) {
      String topicFilter = readUTF8(packet);
      MqttTopicFilterValidator.validate(topicFilter);
      int options = packet.get() & 0xff;
      if ((options & 0xfc) != 0 || (options & 0x03) == 3) {
        throw new MalformedException("Invalid MQTT 3.1.1 SUBSCRIBE options");
      }
      QualityOfService qos = QualityOfService.getInstance(options);
      subscriptionList.add(new SubscriptionInfo(topicFilter, qos));
    }
    if (subscriptionList.isEmpty() || packet.position() - start != remainingLen - 2) {
      throw new MalformedException("Invalid MQTT 3.1.1 SUBSCRIBE payload length");
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
      writeUTF8(packet, info.getTopicFilter());
      packet.put((byte) info.getQualityOfService().getLevel());
    }

    return 0;
  }

}
