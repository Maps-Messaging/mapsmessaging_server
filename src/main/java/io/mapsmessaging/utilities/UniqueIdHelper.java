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

import io.mapsmessaging.api.features.Priority;

public final class UniqueIdHelper {

  private static final int PRIORITY_BITS = 32 - Integer.numberOfLeadingZeros(Priority.HIGHEST.ordinal()); // e.g., 4 for value 10
  private static final int PRIORITY_SHIFT = Long.SIZE - PRIORITY_BITS; // e.g., 64 - 4 = 60
  private static final long PRIORITY_MASK = (1L << PRIORITY_BITS) - 1; // e.g., 0b1111
  private static final long BASE_ID_MASK = (1L << PRIORITY_SHIFT) - 1; // lower bits only

  private UniqueIdHelper() {
    // utility class
  }

  public static long compute(long baseId, int priority) {
    if ((priority & ~0x0F) != 0) {
      throw new IllegalArgumentException("Priority must be between 0 and 15");
    }
    return ((long) priority << PRIORITY_SHIFT) | (baseId & BASE_ID_MASK);
  }

  public static int priority(long uniqueId) {
    return (int) ((uniqueId >>> PRIORITY_SHIFT) & PRIORITY_MASK);
  }

  public static long baseId(long uniqueId) {
    return uniqueId & BASE_ID_MASK;
  }

}

