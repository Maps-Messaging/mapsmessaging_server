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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionSubjectLookupTest {

  @Test
  void standardMetadataRoundTripsWithoutInternalMapsIdentifiers() {
    TransactionSubject original = new TransactionSubject("EVENTS", "reader", 3, 14, 9, 1730000000000000000L, 7);
    TransactionSubject parsed = TransactionSubject.parse(original.toSubject());

    assertEquals("$JS.ACK.EVENTS.reader.3.14.9.1730000000000000000.7", original.toSubject());
    assertEquals("EVENTS", parsed.getStream());
    assertEquals("reader", parsed.getConsumer());
    assertEquals(3, parsed.getDeliverySequence());
    assertEquals(14, parsed.getStreamSequence());
    assertEquals(9, parsed.getConsumerSequence());
    assertEquals(1730000000000000000L, parsed.getTimestamp());
    assertEquals(7, parsed.getPending());
  }

  @Test
  void invalidSubjectsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> TransactionSubject.parse("$JS.ACK.ORDERS.reader.1.2"));
    assertThrows(IllegalArgumentException.class, () -> TransactionSubject.parse("$JS.ACK.ORDERS.reader.1.bad.1.0.0"));
    assertThrows(IllegalArgumentException.class, () -> TransactionSubject.parse("$JS.ACK.ORDERS.reader.1.0.1.0.0"));
    assertThrows(IllegalArgumentException.class, () -> TransactionSubject.parse("$JS.ACK.ORDERS.reader.1.-2.1.0.0"));
    assertThrows(IllegalArgumentException.class, () -> TransactionSubject.parse(null));
  }
}
