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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.values;

import lombok.Getter;

@Getter
public enum ModemMessageStatusFlag {
  GNSS_AVAILABLE(0),
  MESSAGE_RECEIVED(1),
  TRANSMIT_COMPLETE(2),
  REGISTERED_SATELLITE_NETWORK(3),
  DEVICE_RESET(4),
  JAMMING_ANTENNA_CUT(5),
  DEVICE_RESET_IMMINENT(6),
  LOW_POWER_WAKE_CHANGED(7),
  UTC_TIME_UPDATED(8),
  POSITION_FIX_TIMED_OUT(9),
  REQUESTED_EVENT_CACHE(10),
  MOBILE_PING_REPLY_RECEIVED(11);

  private final int bit;

  ModemMessageStatusFlag(int bit) {
    this.bit = bit;
  }

  public boolean isSet(int s88Value) {
    return (s88Value & (1 << bit)) != 0;
  }
}