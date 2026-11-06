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

package io.mapsmessaging.network.protocol.impl.nats.streams;

import io.mapsmessaging.engine.destination.DestinationImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NamespaceManagerSubjectRoutingTest {

  @Test
  void exactPublicSubjectMapsToExistingStreamDestination() {
    StreamInfoList stream = stream("ACK", "ack.order", "ACK/ack/order");
    assertEquals(List.of("ACK/ack/order"), NamespaceManager.matchingStreamDestinations("ack.order", List.of(stream)));
    assertTrue(NamespaceManager.matchingStreamDestinations("ack.other", List.of(stream)).isEmpty());
  }

  @Test
  void wildcardTokensMatchNatsRulesWithoutCrossingBoundaries() {
    assertTrue(NamespaceManager.matchesSubject("sensors.*", "sensors.kitchen"));
    assertFalse(NamespaceManager.matchesSubject("sensors.*", "sensors.kitchen.humidity"));
    assertTrue(NamespaceManager.matchesSubject("sensors.>", "sensors.kitchen.humidity"));
    assertFalse(NamespaceManager.matchesSubject("sensors.>", "sensors"));
    assertFalse(NamespaceManager.matchesSubject("sensors.*", "sensors"));
    assertFalse(NamespaceManager.matchesSubject("orders.*", "orders.status.extra"));
    assertFalse(NamespaceManager.matchesSubject("orders.created", "orders.created.extra"));
  }

  @Test
  void overlappingStreamFiltersDoNotWriteTwiceToSameDestination() {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("EVENTS/ack/order");
    StreamInfoList stream = new StreamInfoList("EVENTS");
    stream.addSubject("ack.order", destination);
    stream.addSubject("ack.*", destination);

    assertEquals(List.of("EVENTS/ack/order"),
        NamespaceManager.matchingStreamDestinations("ack.order", List.of(stream)));
  }

  @Test
  void separateStreamsReceiveOwnCopyWithoutInventingDestinations() {
    StreamInfoList first = stream("ALPHA", "ack.>", "ALPHA/ack/one");
    StreamInfoList second = stream("BETA", "ack.one", "BETA/ack/one");
    assertEquals(2, NamespaceManager.matchingStreamDestinations("ack.one", List.of(first, second)).size());
    assertTrue(NamespaceManager.matchingStreamDestinations("sensor.one", List.of(first, second)).isEmpty());
  }

  private StreamInfoList stream(String name, String filter, String namespace) {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn(namespace);
    StreamInfoList stream = new StreamInfoList(name);
    stream.addSubject(filter, destination);
    return stream;
  }
}
