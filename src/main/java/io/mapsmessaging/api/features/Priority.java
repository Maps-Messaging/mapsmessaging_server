/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.api.features;

import lombok.Getter;
import lombok.ToString;

@ToString
public enum Priority {

  LOWEST(0, "Lowest priority"),
  ONE_ABOVE_LOWEST(1, "Lowest priority +1"),
  TWO_ABOVE_LOWEST(2, "Lowest priority +2"),
  ONE_BELOW_NORMAL(3, "Normal priority -1"),
  NORMAL(4, "Normal priority"),
  ONE_ABOVE_NORMAL(5, "Normal priority +1"),
  TWO_ABOVE_NORMAL(6, "Normal priority +2"),
  THREE_ABOVE_NORMAL(7, "Normal priority +3"),
  TWO_BELOW_HIGHEST(8, "Highest priority -2"),
  ONE_BELOW_HIGHEST(9, "Highest priority -1"),
  HIGHEST(10, "Highest priority");


  public static Priority getInstance(int val) {

    if (val > HIGHEST.getValue()) {
      return HIGHEST;
    }
    if (val < 0) {
      return LOWEST;
    }
    switch (val) {
      case 0:
        return LOWEST;
      case 1:
        return ONE_ABOVE_LOWEST;
      case 2:
        return TWO_ABOVE_LOWEST;
      case 3:
        return ONE_BELOW_NORMAL;
      case 4:
        return NORMAL;
      case 5:
        return ONE_ABOVE_NORMAL;
      case 6:
        return TWO_ABOVE_NORMAL;
      case 7:
        return THREE_ABOVE_NORMAL;
      case 8:
        return TWO_BELOW_HIGHEST;
      case 9:
        return ONE_BELOW_HIGHEST;
      case 10:
        return HIGHEST;

      default:
        throw new IllegalStateException("Unexpected value: " + val);
    }
  }

  @Getter
  private final int value;
  @Getter
  private final String description;

  Priority(int value, String description) {
    this.value = value;
    this.description = description;
  }
}
