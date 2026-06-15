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

package io.mapsmessaging.utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UniqueIdHelperTest {

  @Test
  void compute_roundTripsPriorityAndBaseId() {
    long baseId = 0x0123_4567_89AB_CDEFL;

    long uniqueId = UniqueIdHelper.compute(baseId, 10);

    assertEquals(10, UniqueIdHelper.priority(uniqueId));
    assertEquals(baseId, UniqueIdHelper.baseId(uniqueId));
  }

  @Test
  void compute_masksPriorityBitsFromBaseId() {
    long uniqueId = UniqueIdHelper.compute(-1L, 0);

    assertEquals(0, UniqueIdHelper.priority(uniqueId));
    assertEquals(0x0FFF_FFFF_FFFF_FFFFL, UniqueIdHelper.baseId(uniqueId));
  }

  @Test
  void compute_acceptsPriorityBoundaries() {
    assertEquals(0, UniqueIdHelper.priority(UniqueIdHelper.compute(1, 0)));
    assertEquals(15, UniqueIdHelper.priority(UniqueIdHelper.compute(1, 15)));
  }

  @Test
  void compute_rejectsPriorityOutsideFourBitRange() {
    assertThrows(IllegalArgumentException.class, () -> UniqueIdHelper.compute(1, -1));
    assertThrows(IllegalArgumentException.class, () -> UniqueIdHelper.compute(1, 16));
  }
}
