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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionSubjectLookupTest {

  @Test
  void resolvesAbsoluteAndRelativeMapsDestinationsToTheSameAckOwner() {
    NamedConsumer consumer = mock(NamedConsumer.class);
    StreamSubscriptionInfo subscription = mock(StreamSubscriptionInfo.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    when(consumer.getStreamName()).thenReturn("EVENTS");
    when(consumer.getName()).thenReturn("reader");
    when(consumer.getStreams()).thenReturn(List.of(subscription));
    when(subscription.getDestination()).thenReturn(destination);
    when(destination.getFullyQualifiedNamespace()).thenReturn("EVENTS/telemetry/data");

    assertEquals(0, new TransactionSubject(consumer, "EVENTS/telemetry/data", 42).getDestinationIndex());
    assertEquals(0, new TransactionSubject(consumer, "telemetry.data", 42).getDestinationIndex());
    assertEquals(0, new TransactionSubject(consumer, "telemetry/data", 42).getDestinationIndex());
  }

  @Test
  void unresolvedDestinationDoesNotResolveToAnotherStream() {
    NamedConsumer consumer = mock(NamedConsumer.class);
    when(consumer.getStreamName()).thenReturn("EVENTS");
    when(consumer.getStreams()).thenReturn(List.of());

    assertEquals(Integer.MAX_VALUE, new TransactionSubject(consumer, "OTHER/telemetry", 42).getDestinationIndex());
  }
}
