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

package io.mapsmessaging.state.kpi;

/** Red/amber/green state of one KPI. {@link #NO_DATA} when the KPI's population is empty. */
public enum Band {
  GREEN(0),
  AMBER(1),
  RED(2),
  NO_DATA(-1);

  private final int code;

  Band(int code) {
    this.code = code;
  }

  /** Exported value: 0 green, 1 amber, 2 red, -1 no data. */
  public int getCode() {
    return code;
  }

  boolean isWorseThan(Band other) {
    return code > other.code;
  }

  static Band worst(Band first, Band second) {
    return first.code >= second.code ? first : second;
  }
}
