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

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.qpid.proton.amqp.messaging.Target;
import org.junit.jupiter.api.Test;

class AnonymousSenderAddressTest {

  @Test
  void fixedTargetAlwaysWinsOverMessageAddress() {
    Target link = mock(Target.class);
    when(link.getAddress()).thenReturn("queue/fixed");

    assertEquals("queue/fixed",
        DeliveryEventListener.resolveDestination(link, "queue/ignored"));
  }

  @Test
  void missingLinkTargetUsesMessageToField() {
    assertEquals("queue/per-transfer",
        DeliveryEventListener.resolveDestination(null, "queue/per-transfer"));
  }

  @Test
  void anonymousLinkUsesMessageToField() {
    Target link = mock(Target.class);
    assertEquals("topic/per-transfer",
        DeliveryEventListener.resolveDestination(link, "topic/per-transfer"));
  }

  @Test
  void missingDestinationRemainsRejected() {
    assertNull(DeliveryEventListener.resolveDestination(null, null));
    assertNull(DeliveryEventListener.resolveDestination(null, "  "));
  }
}
