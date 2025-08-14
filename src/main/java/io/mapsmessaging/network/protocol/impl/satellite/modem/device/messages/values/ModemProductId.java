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

package io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.values;

public enum ModemProductId {
  NON_MODEM(0),
  ALL_IDP_DEVICES(4),
  OGI(5),
  ST_6000_6100_6100ME(6),
  ST_2100(7),
  RESERVED(8),
  ST_9100(9);

  private final int id;

  ModemProductId(int id) {
    this.id = id;
  }

  public static ModemProductId fromId(int id) {
    for (ModemProductId p : values()) {
      if (p.id == id) return p;
    }
    return null;
  }

  public int getId() {
    return id;
  }
}
