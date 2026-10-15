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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DurableConsumerSessionIdTest {

  @Test
  void durableSessionIdentityIsStableAndIsolatedByAuthenticatedUserStreamAndConsumer() {
    String first = DurableConsumerSession.sessionId("user-a", "EVENTS", "worker");
    String second = DurableConsumerSession.sessionId("user-a", "EVENTS", "worker");

    assertEquals(first, second);
    assertNotEquals(first, DurableConsumerSession.sessionId("user-b", "EVENTS", "worker"));
    assertNotEquals(first, DurableConsumerSession.sessionId("user-a", "OTHER", "worker"));
    assertNotEquals(first, DurableConsumerSession.sessionId("user-a", "EVENTS", "reader"));
    assertTrue(first.startsWith("nats-jetstream-durable-"));
    assertFalse(first.contains("user-a"));
  }

  @Test
  void rejectsIncompleteOrEmptyDurableSessionIdentity() {
    assertThrows(IllegalArgumentException.class, () -> DurableConsumerSession.sessionId(null, "STREAM", "consumer"));
    assertThrows(IllegalArgumentException.class, () -> DurableConsumerSession.sessionId("user", "", "consumer"));
    assertThrows(IllegalArgumentException.class, () -> DurableConsumerSession.sessionId("user", "STREAM", null));
  }
}
