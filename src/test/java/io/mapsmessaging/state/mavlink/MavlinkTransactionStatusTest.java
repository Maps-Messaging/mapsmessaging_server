/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.state.mavlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MavlinkTransactionStatusTest {

  @Test
  void capturesProtocolStatusWithoutMissionPayloadOrSenderReference() {
    Instant at = Instant.parse("2026-11-08T00:00:00Z");
    JsonObject event = event();
    event.addProperty("reason", "MAVLink response timeout");

    MavlinkTransactionStatus status = MavlinkTransactionStatus.fromJson(event, at);

    assertEquals("transaction-1", status.transactionId());
    assertEquals(17, status.systemId());
    assertEquals(42, status.componentId());
    assertEquals("TIMED_OUT", status.status());
    assertEquals(3, status.attempts());
    assertEquals(2, status.retryCount());
    assertEquals(1, status.requestedItems());
    assertEquals(2, status.totalItems());
    assertEquals("MAVLink response timeout", status.reason());
    assertEquals(at, status.observedAt());
  }

  @Test
  void reasonMayBeAbsent() {
    assertNull(MavlinkTransactionStatus.fromJson(event(), Instant.now()).reason());
  }

  @Test
  void missingIdentityOrInvalidCountsAreRejected() {
    JsonObject missing = event();
    missing.remove("transactionId");
    assertThrows(IllegalArgumentException.class, () -> MavlinkTransactionStatus.fromJson(missing, Instant.now()));

    JsonObject negative = event();
    negative.addProperty("retryCount", -1);
    assertThrows(IllegalArgumentException.class, () -> MavlinkTransactionStatus.fromJson(negative, Instant.now()));

    JsonObject excessive = event();
    excessive.addProperty("requestedItems", 3);
    assertThrows(IllegalArgumentException.class, () -> MavlinkTransactionStatus.fromJson(excessive, Instant.now()));

    JsonObject wrongType = event();
    wrongType.addProperty("systemId", "seventeen");
    assertThrows(IllegalArgumentException.class, () -> MavlinkTransactionStatus.fromJson(wrongType, Instant.now()));
  }

  static JsonObject event() {
    JsonObject event = new JsonObject();
    event.addProperty("transactionId", "transaction-1");
    event.addProperty("systemId", 17);
    event.addProperty("componentId", 42);
    event.addProperty("status", "TIMED_OUT");
    event.addProperty("attempts", 3);
    event.addProperty("retryCount", 2);
    event.addProperty("requestedItems", 1);
    event.addProperty("totalItems", 2);
    return event;
  }
}
