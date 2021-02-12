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

package org.maps.network.protocol.impl.mqtt5.packet;

import java.util.ArrayList;
import java.util.List;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.features.RetainHandler;
import org.maps.network.io.Packet;
import org.maps.network.protocol.EndOfBufferException;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;
import org.maps.network.protocol.impl.mqtt.packet.SubscriptionInfo;
import org.maps.network.protocol.impl.mqtt5.packet.properties.MessageProperty;
import org.maps.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;
import org.maps.network.protocol.impl.mqtt5.packet.properties.SubscriptionIdentifier;

/**
 * https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901161
 */

// Between MQTT 3/4 and 5 there is duplicate code base, yes this is by design
@java.lang.SuppressWarnings("common-java:DuplicatedBlocks")
public class Subscribe5 extends MQTTPacket5 {

  private final List<SubscriptionInfo> subscriptionList;
  private final int messageId;

  public Subscribe5(byte fixedHeader, long remainingLen, Packet packet) throws MalformedException, EndOfBufferException {
    super(MQTTPacket.SUBSCRIBE);
    if ((fixedHeader & 0xf) != 2) {
      throw new MalformedException("Subscribe Fixed Header bits 3,2,1,0 must be set as 0,0,1,0  as per the specification : [MQTT-3.8.1-1]");
    }
    messageId = readShort(packet);
    long loaded = loadProperties(packet);
    loaded += 2;
    subscriptionList = new ArrayList<>();

    SubscriptionIdentifier subscriptionIdValue = null;
    for (MessageProperty property : getProperties().values()) {
      if (property.getId() == MessagePropertyFactory.SUBSCRIPTION_IDENTIFIER) {
        subscriptionIdValue = (SubscriptionIdentifier) property;
        break;
      }
    }

    while (loaded < remainingLen) {
      String topicFilter = readUTF8(packet);
      byte subscriptionOptions = packet.get();
      QualityOfService qos = QualityOfService.getInstance((subscriptionOptions & 0b11));
      boolean noLocal = (subscriptionOptions & 0b100) != 0;
      boolean retainAsPublish = (subscriptionOptions & 0b1000) != 0;
      RetainHandler retainHandling = RetainHandler.getInstance((subscriptionOptions & 0b110000) >> 4);
      if ((subscriptionOptions & 0b11000000) != 0) {
        throw new MalformedException("The Server MUST treat a SUBSCRIBE packet as malformed if any of Reserved bits in the Payload are non-zero [MQTT-3.8.3-5].");
      }
      if (qos.equals(QualityOfService.MQTT_SN_REGISTERED)) {
        throw new MalformedException("QoS valid values are 0,1 or 2 any other values are considered a Malformed Packet : [MQTT-3-8.3-4]");
      }
      SubscriptionInfo info = new SubscriptionInfo(topicFilter, qos, retainHandling, noLocal, retainAsPublish, subscriptionIdValue);
      subscriptionList.add(info);
      loaded += 1 + topicFilter.length() + 2;
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
    sb.append("MQTTv5 Subscribe[");
    for (SubscriptionInfo info : subscriptionList) {
      sb.append(info.toString());
    }

    sb.append("]");
    return sb.toString();
  }

  public int packFrame(Packet packet) {
    return 0;
  }
}
