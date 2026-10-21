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

package io.mapsmessaging.hardware.device.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataFilterTest {

  @Test
  void alwaysSend_acceptsNullAndUnchangedPayloads() {
    assertTrue(DataFilter.ALWAYS_SEND.send(null, null));
    assertTrue(DataFilter.ALWAYS_SEND.send(new byte[]{1, 2}, new byte[]{1, 2}));
  }

  @Test
  void onChange_rejectsEqualPayloads() {
    assertFalse(DataFilter.ON_CHANGE.send(new byte[0], new byte[0]));
    assertFalse(DataFilter.ON_CHANGE.send(new byte[]{1, 2, 3}, new byte[]{1, 2, 3}));
  }

  @Test
  void onChange_acceptsDifferentPayloadsAndLengths() {
    assertTrue(DataFilter.ON_CHANGE.send(new byte[]{1, 2, 3}, new byte[]{1, 2, 4}));
    assertTrue(DataFilter.ON_CHANGE.send(new byte[]{1}, new byte[]{1, 2}));
  }

  @Test
  void onChange_acceptsMissingPreviousOrCurrentPayload() {
    assertTrue(DataFilter.ON_CHANGE.send(null, new byte[]{1}));
    assertTrue(DataFilter.ON_CHANGE.send(new byte[]{1}, null));
    assertTrue(DataFilter.ON_CHANGE.send(null, null));
  }
}
