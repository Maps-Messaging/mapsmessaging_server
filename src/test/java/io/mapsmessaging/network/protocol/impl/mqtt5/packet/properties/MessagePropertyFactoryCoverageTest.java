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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MessagePropertyFactoryCoverageTest {

  @ParameterizedTest
  @MethodSource("properties")
  void factoryReturnsExpectedPropertyType(int id, Class<? extends MessageProperty> type) {
    MessageProperty property = MessagePropertyFactory.getInstance().find(id);

    assertNotNull(property);
    assertEquals(id, property.getId());
    assertEquals(type, property.getClass());
  }

  @ParameterizedTest
  @MethodSource("properties")
  void factoryReturnsFreshInstances(int id, Class<? extends MessageProperty> type) {
    MessageProperty first = MessagePropertyFactory.getInstance().find(id);
    MessageProperty second = MessagePropertyFactory.getInstance().find(id);

    assertNotSame(first, second);
    assertEquals(type, first.getClass());
    assertEquals(type, second.getClass());
  }

  @Test
  void unknownPropertyIdentifierReturnsNull() {
    assertNull(MessagePropertyFactory.getInstance().find(0x7F));
  }

  private static Stream<Arguments> properties() {
    return Stream.of(
        Arguments.of(MessagePropertyFactory.MAXIMUM_QOS, MaximumQoS.class),
        Arguments.of(MessagePropertyFactory.RECEIVE_MAXIMUM, ReceiveMaximum.class),
        Arguments.of(MessagePropertyFactory.SESSION_EXPIRY_INTERVAL, SessionExpiryInterval.class),
        Arguments.of(MessagePropertyFactory.MAXIMUM_PACKET_SIZE, MaximumPacketSize.class),
        Arguments.of(MessagePropertyFactory.RETAIN_AVAILABLE, RetainAvailable.class),
        Arguments.of(MessagePropertyFactory.ASSIGNED_CLIENT_IDENTIFIER, AssignedClientIdentifier.class),
        Arguments.of(MessagePropertyFactory.TOPIC_ALIAS_MAXIMUM, TopicAliasMaximum.class),
        Arguments.of(MessagePropertyFactory.TOPIC_ALIAS, TopicAlias.class),
        Arguments.of(MessagePropertyFactory.USER_PROPERTY, UserProperty.class),
        Arguments.of(MessagePropertyFactory.WILDCARD_SUBSCRIPTION_AVAILABLE, WildcardSubscriptionsAvailable.class),
        Arguments.of(MessagePropertyFactory.SUBSCRIPTION_IDENTIFIERS_AVAILABLE, SubscriptionIdentifiersAvailable.class),
        Arguments.of(MessagePropertyFactory.SHARED_SUBSCRIPTION_AVAILABLE, SharedSubscriptionsAvailable.class),
        Arguments.of(MessagePropertyFactory.SERVER_KEEPALIVE, ServerKeepAlive.class),
        Arguments.of(MessagePropertyFactory.RESPONSE_INFORMATION, ResponseInformation.class),
        Arguments.of(MessagePropertyFactory.SERVER_REFERENCE, ServerReference.class),
        Arguments.of(MessagePropertyFactory.AUTHENTICATION_METHOD, AuthenticationMethod.class),
        Arguments.of(MessagePropertyFactory.AUTHENTICATION_DATA, AuthenticationData.class),
        Arguments.of(MessagePropertyFactory.REQUEST_RESPONSE_INFORMATION, RequestResponseInformation.class),
        Arguments.of(MessagePropertyFactory.REQUEST_PROBLEM_INFORMATION, RequestProblemInformation.class),
        Arguments.of(MessagePropertyFactory.WILL_DELAY_INTERVAL, WillDelayInterval.class),
        Arguments.of(MessagePropertyFactory.MESSAGE_EXPIRY_INTERVAL, MessageExpiryInterval.class),
        Arguments.of(MessagePropertyFactory.CONTENT_TYPE, ContentType.class),
        Arguments.of(MessagePropertyFactory.CORRELATION_DATA, CorrelationData.class),
        Arguments.of(MessagePropertyFactory.RESPONSE_TOPIC, ResponseTopic.class),
        Arguments.of(MessagePropertyFactory.SUBSCRIPTION_IDENTIFIER, SubscriptionIdentifier.class),
        Arguments.of(MessagePropertyFactory.PAYLOAD_FORMAT_INDICATOR, PayloadFormatIndicator.class),
        Arguments.of(MessagePropertyFactory.REASON_STRING, ReasonString.class)
    );
  }
}
