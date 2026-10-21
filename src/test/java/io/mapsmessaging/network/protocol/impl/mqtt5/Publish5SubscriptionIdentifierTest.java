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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Publish5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.SubscriptionIdentifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Publish5SubscriptionIdentifierTest {

  @Test
  void nonPositiveSubscriptionIdentifiersAreOmitted() {
    Publish5 publish = new Publish5(new byte[0], QualityOfService.AT_MOST_ONCE, 0, "test/topic", false);

    publish.add(new SubscriptionIdentifier(0));
    publish.add(new SubscriptionIdentifier(-1));

    Assertions.assertNull(publish.getProperties().get(MessagePropertyFactory.SUBSCRIPTION_IDENTIFIER));
  }

  @Test
  void positiveSubscriptionIdentifierIsPreserved() {
    Publish5 publish = new Publish5(new byte[0], QualityOfService.AT_MOST_ONCE, 0, "test/topic", false);

    publish.add(new SubscriptionIdentifier(1));

    SubscriptionIdentifier property =
        (SubscriptionIdentifier) publish.getProperties().get(MessagePropertyFactory.SUBSCRIPTION_IDENTIFIER);
    Assertions.assertNotNull(property);
    Assertions.assertEquals(1L, property.getSubscriptionIdentifier());
  }
}
