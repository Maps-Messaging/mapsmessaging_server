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

package io.mapsmessaging.state.adapter.mti;

import io.mapsmessaging.state.metrics.FeedActivityRegistry;
import io.mapsmessaging.state.metrics.MessageOutcomeStats;
import org.junit.jupiter.api.Test;

import static io.mapsmessaging.state.metrics.MessageOutcomeStats.Category.FAILURE;
import static io.mapsmessaging.state.metrics.MessageOutcomeStats.Category.FILTERED;
import static io.mapsmessaging.state.metrics.MessageOutcomeStats.Source.MTI;
import static org.junit.jupiter.api.Assertions.*;

class MtiStatusAdapterOutcomeTest {

  private static String status(String uid, String observedAt, String validUntil) {
    String validity = validUntil == null ? "null" : "\"" + validUntil + "\"";
    return """
        {"schema":"mti.asset.health/v1","op":"update","uid":"%s","state":"go","observed_at":"%s","valid_until":%s}
        """.formatted(uid, observedAt, validity);
  }

  @Test
  void malformedMessages_countAsFailures_perReason() {
    MtiStatusAdapter adapter = new MtiStatusAdapter("/mti/status");
    long missingUid = MessageOutcomeStats.getCount(MTI, "missing_uid", FAILURE);
    long invalidValidity = MessageOutcomeStats.getCount(MTI, "invalid_validity", FAILURE);

    adapter.handle("{\"op\":\"update\",\"state\":\"go\"}");
    adapter.handle(status("asset-1", "2026-10-01T10:00:00Z", null));

    assertEquals(missingUid + 1, MessageOutcomeStats.getCount(MTI, "missing_uid", FAILURE));
    assertEquals(invalidValidity + 1, MessageOutcomeStats.getCount(MTI, "invalid_validity", FAILURE));
    assertNull(adapter.snapshot("asset-1"), "a record without valid_until is not accepted");
  }

  @Test
  void outOfOrderUpdate_isFilteredByDesign_notAFailure() {
    MtiStatusAdapter adapter = new MtiStatusAdapter("/mti/status");
    long filtered = MessageOutcomeStats.getCount(MTI, "out_of_order", FILTERED);

    adapter.handle(status("asset-2", "2026-10-01T10:00:10Z", "2099-01-01T00:00:00Z"));
    adapter.handle(status("asset-2", "2026-10-01T10:00:00Z", "2099-01-01T00:00:00Z"));

    assertEquals(filtered + 1, MessageOutcomeStats.getCount(MTI, "out_of_order", FILTERED));
  }

  @Test
  void anyMessage_marksTheMtiFeedActive() {
    MtiStatusAdapter adapter = new MtiStatusAdapter("/mti/status");
    long before = System.currentTimeMillis();

    adapter.handle(status("asset-3", "2026-10-01T10:00:00Z", "2099-01-01T00:00:00Z"));

    FeedActivityRegistry.FeedState state = FeedActivityRegistry.snapshot().get(MtiStatusAdapter.FEED_NAME);
    assertNotNull(state);
    assertTrue(state.lastActivityMillis() >= before);
  }

  @Test
  void clearCache_forgetsEveryStatus() {
    MtiStatusAdapter adapter = new MtiStatusAdapter("/mti/status");
    adapter.handle(status("asset-4", "2026-10-01T10:00:00Z", "2099-01-01T00:00:00Z"));
    assertNotNull(adapter.snapshot("asset-4"));

    adapter.clearCache();

    assertNull(adapter.snapshot("asset-4"));
  }
}
