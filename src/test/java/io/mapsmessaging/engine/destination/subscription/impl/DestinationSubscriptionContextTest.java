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

package io.mapsmessaging.engine.destination.subscription.impl;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManager;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.session.SessionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class DestinationSubscriptionContextTest {

  @Test
  void reconnect_does_not_add_the_same_subscription_context_twice() {
    SubscriptionContext original = new SubscriptionContext("topic/#");
    DestinationSubscription subscription = createSubscription(original);

    subscription.addContext(new SubscriptionContext("topic/#"));

    Assertions.assertEquals(1, subscription.getContexts().size());
    Assertions.assertSame(original, subscription.getContext());
  }

  @Test
  void protocol_subscriptions_with_different_aliases_are_preserved() {
    SubscriptionContext first = new SubscriptionContext("topic/value");
    first.setAlias("subscription-1");
    SubscriptionContext second = new SubscriptionContext("topic/value");
    second.setAlias("subscription-2");
    DestinationSubscription subscription = createSubscription(first);

    subscription.addContext(second);

    Assertions.assertEquals(2, subscription.getContexts().size());
    Assertions.assertSame(first, subscription.getContexts().get(0));
    Assertions.assertSame(second, subscription.getContexts().get(1));
  }

  @Test
  void overlapping_filters_for_the_same_destination_are_preserved() {
    SubscriptionContext wildcard = new SubscriptionContext("topic/#");
    SubscriptionContext exact = new SubscriptionContext("topic/value");
    DestinationSubscription subscription = createSubscription(wildcard);

    subscription.addContext(exact);

    Assertions.assertEquals(2, subscription.getContexts().size());
  }

  private DestinationSubscription createSubscription(SubscriptionContext context) {
    return new DestinationSubscription(
        mock(DestinationImpl.class),
        context,
        mock(SessionImpl.class),
        "session",
        mock(AcknowledgementController.class),
        mock(MessageStateManager.class),
        false);
  }
}
