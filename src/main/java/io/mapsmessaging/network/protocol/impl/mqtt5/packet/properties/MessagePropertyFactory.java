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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties;

import java.util.Map;
import java.util.TreeMap;

public class MessagePropertyFactory {

  // 0x0 - 0x0F
  public static final int PAYLOAD_FORMAT_INDICATOR = 0x01;
  public static final int MESSAGE_EXPIRY_INTERVAL = 0x02;
  public static final int CONTENT_TYPE = 0x03;
  public static final int RESPONSE_TOPIC = 0x08;
  public static final int CORRELATION_DATA = 0x09;
  public static final int SUBSCRIPTION_IDENTIFIER = 0x0B;

  // 0x10 - 0x1F
  public static final int SESSION_EXPIRY_INTERVAL = 0x11;
  public static final int ASSIGNED_CLIENT_IDENTIFIER = 0x12;
  public static final int SERVER_KEEPALIVE = 0x13;
  public static final int AUTHENTICATION_METHOD = 0x15;
  public static final int AUTHENTICATION_DATA = 0x16;
  public static final int REQUEST_PROBLEM_INFORMATION = 0x17;
  public static final int WILL_DELAY_INTERVAL = 0x18;
  public static final int REQUEST_RESPONSE_INFORMATION = 0x19;
  public static final int RESPONSE_INFORMATION = 0x1A;
  public static final int SERVER_REFERENCE = 0x1C;
  public static final int REASON_STRING = 0x1F;

  // 0x20 - 0x2F
  public static final int RECEIVE_MAXIMUM = 0x21;
  public static final int TOPIC_ALIAS_MAXIMUM = 0x22;
  public static final int TOPIC_ALIAS = 0x23;
  public static final int MAXIMUM_QOS = 0x24;
  public static final int USER_PROPERTY = 0x26;
  public static final int RETAIN_AVAILABLE = 0x25;
  public static final int MAXIMUM_PACKET_SIZE = 0x27;
  public static final int WILDCARD_SUBSCRIPTION_AVAILABLE = 0x28;
  public static final int SUBSCRIPTION_IDENTIFIERS_AVAILABLE = 0x29;
  public static final int SHARED_SUBSCRIPTION_AVAILABLE = 0x2A;

  private static final MessagePropertyFactory instance = new MessagePropertyFactory();
  private final Map<Integer, MessageProperty> properties;

  private MessagePropertyFactory() {
    properties = new TreeMap<>();
    load(new MaximumQoS());
    load(new ReceiveMaximum());
    load(new SessionExpiryInterval());
    load(new MaximumPacketSize());
    load(new RetainAvailable());
    load(new AssignedClientIdentifier());
    load(new TopicAliasMaximum());
    load(new TopicAlias());
    load(new UserProperty());
    load(new WildcardSubscriptionsAvailable());
    load(new SubscriptionIdentifiersAvailable());
    load(new SharedSubscriptionsAvailable());
    load(new ServerKeepAlive());
    load(new ResponseInformation());
    load(new ServerReference());
    load(new AuthenticationMethod());
    load(new AuthenticationData());
    load(new RequestResponseInformation());
    load(new RequestProblemInformation());
    load(new WillDelayInterval());
    load(new MessageExpiryInterval());
    load(new ContentType());
    load(new CorrelationData());
    load(new ResponseTopic());
    load(new SubscriptionIdentifier());
    load(new PayloadFormatIndicator());
    load(new ReasonString());
  }

  public static MessagePropertyFactory getInstance() {
    return instance;
  }

  private void load(MessageProperty property) {
    properties.put(property.getId(), property);
  }

  public MessageProperty find(int id) {
    MessageProperty property = properties.get(id);
    if (property != null) {
      return property.instance();
    }
    return null;
  }

  Map<Integer, MessageProperty> getProperties() {
    return properties;
  }
}
