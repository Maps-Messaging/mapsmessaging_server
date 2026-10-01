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

package io.mapsmessaging.network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProtocolLookupStringContractTest {
  private Protocol protocol(Map<String, String> mappings) {
    Protocol protocol = mock(Protocol.class, CALLS_REAL_METHODS);
    doReturn(mappings).when(protocol).getTopicNameMapping();
    return protocol;
  }

  @Test
  void exact_mapping_and_unmatched_values_stay_unchanged() {
    Protocol protocol = protocol(Map.of("exact", "mapped"));
    assertEquals("mapped", protocol.parseForLookup("exact"));
    assertEquals("other///value", protocol.parseForLookup("other///value"));
  }

  @Test
  void wildcard_mapping_preserves_single_pass_slash_and_hash_removal() {
    Protocol protocol = protocol(Map.of("sensor/#", "local/"));
    assertEquals("local/sensor//value", protocol.parseForLookup("sensor///value#"));
  }

  @Test
  void later_wildcard_uses_previously_transformed_value() {
    Map<String, String> mappings = new LinkedHashMap<>();
    mappings.put("sensor/#", "local/");
    mappings.put("local/#", "second/");
    assertEquals("second/local/sensor/value", protocol(mappings).parseForLookup("sensor/value"));
  }
}
