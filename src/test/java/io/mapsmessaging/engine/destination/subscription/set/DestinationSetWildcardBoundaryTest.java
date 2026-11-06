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

package io.mapsmessaging.engine.destination.subscription.set;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DestinationSetWildcardBoundaryTest {

  @ParameterizedTest
  @CsvSource({
      "a/b/+, a/b",
      "a/+, a",
      "a/+/c, a",
      "a/+/c, a/b",
      "a/b, a/b/c",
      "a/b/c, a/b",
      "a/b/, a/b",
      "a/b, a/b/",
      "a/+/+, a/b",
      "a/+, a/b/",
      "/+, /a/b",
      "a//+, a/"
  })
  void matches_rejects_missing_or_extra_topic_levels(String filter, String topic) {
    assertFalse(DestinationSet.matches(filter, topic));
  }

  @ParameterizedTest
  @CsvSource({
      "a/b/+, a/b/c",
      "a/b/+, a/b/",
      "a/+/+, a/b/",
      "a/+/c, a//c",
      "+/a, /a",
      "+/+, /",
      "a//+, a//",
      "a/b/, a/b/"
  })
  void matches_preserves_empty_topic_levels(String filter, String topic) {
    assertTrue(DestinationSet.matches(filter, topic));
  }

  @ParameterizedTest
  @CsvSource({
      "a/#, a",
      "a/#, a/",
      "a/#, a/b/c",
      "a/+/#, a/b",
      "a/+/#, a/",
      "a/+/#, a//c",
      "a/b/#, a/b",
      "#, a/b/c"
  })
  void matches_terminal_hash_matches_zero_or_more_levels(String filter, String topic) {
    assertTrue(DestinationSet.matches(filter, topic));
  }

  @ParameterizedTest
  @CsvSource({
      "a/+/#, a",
      "a/#/c, a/b/c",
      "a/b#, a/b/c",
      "a/#suffix, a/b",
      "a/++, a/b",
      "a/#/, a/b"
  })
  void matches_rejects_incomplete_or_malformed_wildcards(String filter, String topic) {
    assertFalse(DestinationSet.matches(filter, topic));
  }

  @Test
  void matches_requires_explicit_system_topic_prefix() {
    assertFalse(DestinationSet.matches("#", "$SYS/status"));
    assertFalse(DestinationSet.matches("+/status", "$SYS/status"));
    assertTrue(DestinationSet.matches("$SYS/#", "$SYS"));
    assertTrue(DestinationSet.matches("$SYS/#", "$SYS/status"));
    assertTrue(DestinationSet.matches("$SYS/+", "$SYS/"));
    assertTrue(DestinationSet.matches("$SYS/status", "$SYS/status"));
    assertFalse(DestinationSet.matches("$SYS/+", "$SYS"));
    assertTrue(DestinationSet.matches("a/+", "a/$SYS"));
  }

  @Test
  void matches_null_input_returns_false() {
    assertFalse(DestinationSet.matches((String) null, "a/b"));
    assertFalse(DestinationSet.matches("a/+", null));
  }

  @Test
  void interest_context_requires_the_single_wildcard_level() {
    SubscriptionContext context = new SubscriptionContext("a/b/+");
    DestinationSet destinations = new DestinationSet(context, Map.of());

    assertFalse(DestinationSet.matches(context, "a/b"));
    assertFalse(destinations.interest("a/b"));
    assertTrue(destinations.interest("a/b/"));
    assertTrue(destinations.interest("a/b/c"));
    assertFalse(destinations.interest("a/b/c/d"));
  }
}
