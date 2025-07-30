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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.values;


public enum ModemResetReason {
  UNKNOWN(0),
  POWER_ON(1),
  NEW_TRAFFIC_CHANNEL(2),
  EXTERNAL_RESET(3),
  OVER_THE_AIR_RESET(4),
  BROWN_OUT(5),
  WATCH_DOG(6),
  SOFTWARE(7),
  RESERVED(8),
  DSP_ERROR_1(9),
  DSP_ERROR_2(10),
  DSP_ERROR_3(11),
  DSP_ERROR_4(12);

  private final int value;

  ModemResetReason(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public boolean isSet(int bits) {
    return (bits & (1 << value)) != 0;
  }

  public static ModemResetReason fromValue(int value) {
    for (ModemResetReason r : values()) {
      if (r.value == value) return r;
    }
    return UNKNOWN;
  }
}
