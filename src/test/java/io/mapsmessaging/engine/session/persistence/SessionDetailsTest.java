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

package io.mapsmessaging.engine.session.persistence;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SessionDetailsTest {

  @Test
  void repeated_subscription_key_is_not_persisted_twice() {
    SessionDetails details = new SessionDetails("session", "unique", 1, 60);
    SubscriptionContext first = new SubscriptionContext("topic/#");
    SubscriptionContext duplicate = new SubscriptionContext("topic/#");

    Assertions.assertTrue(details.addSubscriptionContextIfAbsent(first));
    Assertions.assertFalse(details.addSubscriptionContextIfAbsent(duplicate));
    Assertions.assertEquals(1, details.getSubscriptionContextList().size());
    Assertions.assertSame(first, details.getSubscriptionContextList().get(0));
  }

  @Test
  void overlapping_subscription_keys_are_still_persisted() {
    SessionDetails details = new SessionDetails("session", "unique", 1, 60);
    SubscriptionContext wildcard = new SubscriptionContext("topic/#");
    SubscriptionContext exact = new SubscriptionContext("topic/value");

    Assertions.assertTrue(details.addSubscriptionContextIfAbsent(wildcard));
    Assertions.assertTrue(details.addSubscriptionContextIfAbsent(exact));
    Assertions.assertEquals(2, details.getSubscriptionContextList().size());
  }

  @Test
  void same_destination_with_different_protocol_aliases_is_not_deduplicated() {
    SessionDetails details = new SessionDetails("session", "unique", 1, 60);
    SubscriptionContext first = new SubscriptionContext("topic/value");
    first.setAlias("subscription-1");
    SubscriptionContext second = new SubscriptionContext("topic/value");
    second.setAlias("subscription-2");

    Assertions.assertTrue(details.addSubscriptionContextIfAbsent(first));
    Assertions.assertTrue(details.addSubscriptionContextIfAbsent(second));
    Assertions.assertEquals(2, details.getSubscriptionContextList().size());
  }

  @Test
  void same_destination_in_different_shared_groups_is_not_deduplicated() {
    SessionDetails details = new SessionDetails("session", "unique", 1, 60);
    SubscriptionContext first = new SubscriptionContext("topic/value");
    first.setSharedName("group-1");
    SubscriptionContext second = new SubscriptionContext("topic/value");
    second.setSharedName("group-2");

    Assertions.assertTrue(details.addSubscriptionContextIfAbsent(first));
    Assertions.assertTrue(details.addSubscriptionContextIfAbsent(second));
    Assertions.assertEquals(2, details.getSubscriptionContextList().size());
  }
}
