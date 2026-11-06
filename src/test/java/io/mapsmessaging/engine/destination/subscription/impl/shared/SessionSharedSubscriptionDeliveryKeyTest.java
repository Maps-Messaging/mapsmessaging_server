/*
 *
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

package io.mapsmessaging.engine.destination.subscription.impl.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import org.junit.jupiter.api.Test;

class SessionSharedSubscriptionDeliveryKeyTest {

  @Test
  void deliveryKeyRemainsAvailableAfterContextRemoval() {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("queue/shared-delivery");
    SharedSubscription shared = mock(SharedSubscription.class);
    when(shared.getDestinationImpl()).thenReturn(destination);

    SubscriptionContext context = new SubscriptionContextBuilder(
        "queue/shared-delivery", ClientAcknowledgement.AUTO).build();
    String expected = SubscribedEventManager.deliveryKey(context, "queue/shared-delivery");
    SessionSharedSubscription subscription = new SessionSharedSubscription(
        shared, null, "session", context, mock(AcknowledgementController.class));

    subscription.removeContext(context);

    assertNull(subscription.getContext());
    assertEquals(expected, subscription.getDeliveryKey());
  }
}
