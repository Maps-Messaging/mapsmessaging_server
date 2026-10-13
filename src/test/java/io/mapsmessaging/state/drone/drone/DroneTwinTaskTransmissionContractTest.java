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

package io.mapsmessaging.state.drone.drone;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DroneTwinTaskTransmissionContractTest {

  @Test
  void transmission_extension_preserves_caller_owned_values_and_reference_identity() {
    DroneTwin twin = new DroneTwin();
    assertNull(twin.getTaskTransmission());
    Map<String, Object> state = new HashMap<>();
    Object extensionValue = new Object();
    state.put("extension", extensionValue);
    state.put("optional", null);
    twin.setTaskTransmission(state);
    assertSame(state, twin.getTaskTransmission());
    assertSame(extensionValue, twin.getTaskTransmission().get("extension"));
    assertTrue(twin.getTaskTransmission().containsKey("optional"));
    assertNull(twin.getTaskTransmission().get("optional"));
    twin.setTaskTransmission(null);
    assertNull(twin.getTaskTransmission());
  }

  @Test
  void extension_state_can_be_set_and_replaced_by_another_thread() throws Exception {
    DroneTwin twin = new DroneTwin();
    Map<String, Object> initial = Map.of("status", "pending");
    Map<String, Object> complete = Map.of("status", "complete");
    try (var executor = Executors.newSingleThreadExecutor()) {
      executor.submit(() -> twin.setTaskTransmission(initial)).get(5, TimeUnit.SECONDS);
      assertSame(initial, twin.getTaskTransmission());
      executor.submit(() -> twin.setTaskTransmission(complete)).get(5, TimeUnit.SECONDS);
      assertSame(complete, twin.getTaskTransmission());
    }
  }
}
