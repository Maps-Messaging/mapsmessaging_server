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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MqttTopicFilterValidator;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.SubscriptionInfo;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessageProperty;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.SubscriptionIdentifier;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901161
 */

// Between MQTT 3/4 and 5 there is duplicate code base, yes this is by design
@java.lang.SuppressWarnings("common-java:DuplicatedBlocks") // MQTT 3/4 and MQTT5 share a lot in common, however, the changes don't make it easy to extend.
@Getter
public class Subscribe5 extends MQTTPacket5 {

  private final List<SubscriptionInfo> subscriptionList;
  @Setter
  private int messageId;

  public Subscribe5(){
    super(MQTTPacket.SUBSCRIBE);
    subscriptionList = new ArrayList<>();
    messageId = 0;
  }

  public Subscribe5(byte fixedHeader, long remainingLen, Packet packet) throws MalformedException, EndOfBufferException {
    super(MQTTPacket.SUBSCRIBE);
    if ((fixedHeader & 0xf) != 2) {
      throw new MalformedException("Subscribe Fixed Header bits 3,2,1,0 must be set as 0,0,1,0  as per the specification : [MQTT-3.8.1-1]");
    }
    messageId = readShort(packet);
    if (messageId == 0) {
      throw new MalformedException("SUBSCRIBE Packet Identifier must not be zero [MQTT-2.2.1-3]");
    }
    int start = packet.position();
    loadProperties(packet);
    long consumed = (long) packet.position() - start + 2;
    subscriptionList = new ArrayList<>();

    SubscriptionIdentifier subscriptionIdValue = validatedSubscriptionIdentifier();

    while (consumed < remainingLen) {
      subscriptionList.add(readSubscription(packet, subscriptionIdValue));
      consumed = (long) packet.position() - start + 2;
    }
    if (subscriptionList.isEmpty() || consumed != remainingLen) {
      throw new MalformedException("Invalid MQTT 5 SUBSCRIBE payload length");
    }
  }

  private SubscriptionIdentifier validatedSubscriptionIdentifier() throws MalformedException {
    SubscriptionIdentifier subscriptionIdValue = null;
    for (MessageProperty property : getProperties().values()) {
      if (property.getId() == MessagePropertyFactory.SUBSCRIPTION_IDENTIFIER) {
        if (subscriptionIdValue != null) {
          throw new MalformedException("Duplicate MQTT 5 Subscription Identifier in SUBSCRIBE");
        }
        subscriptionIdValue = (SubscriptionIdentifier) property;
        if (subscriptionIdValue.getSubscriptionIdentifier() == 0) {
          throw new MalformedException("Subscription Identifier must not be zero [MQTT-3.8.2.1.2]");
        }
      }
    }

    return subscriptionIdValue;
  }

  private SubscriptionInfo readSubscription(Packet packet, SubscriptionIdentifier subscriptionIdValue)
      throws MalformedException, EndOfBufferException {
    String topicFilter = readUTF8(packet);
    MqttTopicFilterValidator.validate(topicFilter);
    int options = packet.get() & 0xff;
    int qosValue = options & 0x03;
    int retainValue = (options >> 4) & 0x03;
    if ((options & 0xc0) != 0 || qosValue == 3 || retainValue == 3) {
      throw new MalformedException("Invalid MQTT 5 SUBSCRIBE options [MQTT-3.8.3.1]");
    }
    boolean noLocal = (options & 0x04) != 0;
    if (noLocal && MqttTopicFilterValidator.isShared(topicFilter)) {
      throw new MalformedException("No Local forbidden on shared subscriptions [MQTT-3.8.3-4]");
    }
    QualityOfService qos = QualityOfService.getInstance(qosValue);
    boolean retainAsPublish = (options & 0x08) != 0;
    RetainHandler retainHandling = RetainHandler.getInstance(retainValue);
    return new SubscriptionInfo(topicFilter, qos, retainHandling,
        noLocal, retainAsPublish, subscriptionIdValue);
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
    sb.append("MQTTv5 Subscribe[");
    for (SubscriptionInfo info : subscriptionList) {
      sb.append(info.toString());
    }

    sb.append("]");
    return sb.toString();
  }

  @Override
  public int packFrame(Packet packet) {
    int remainingLength = 2; // Packet Identifier

    int propertiesLength = propertiesSize();
    remainingLength += lengthSize(propertiesLength) + propertiesLength;

    // Payload: each subscription = UTF-8 topic (2 + bytes) + 1 options byte
    for (SubscriptionInfo info : subscriptionList) {
      int utf8Len = info.getTopicFilter().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
      remainingLength += 2 + utf8Len + 1;
    }

    // Fixed header (control packet type + required flags 0010)
    packControlByte(packet, 2);
    writeVariableInt(packet, remainingLength);

    // Variable header
    writeShort(packet, messageId);
    packProperties(packet, propertiesLength);

    // Payload
    for (SubscriptionInfo info : subscriptionList) {
      writeUTF8(packet, info.getTopicFilter());

      int qos = info.getQualityOfService().getLevel() & 0b11;
      int noLocal = info.noLocalMessages() ? 0b100 : 0;
      int rap = info.isRetainAsPublished() ? 0b1000 : 0;
      int rh = (info.getRetainHandling().getHandler() & 0b11) << 4;

      packet.put((byte) (qos | noLocal | rap | rh));
    }

    return remainingLength;
  }


}
