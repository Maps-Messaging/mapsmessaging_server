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

package io.mapsmessaging.network.protocol.impl.coap.packet;

import lombok.Getter;

public enum TYPE {

  CON(0, "Confirm", "Requires the packet be confirmed"),
  NON(1, "Non-Confirmable", "No Confirmation required for the packet"),
  ACK(2, "Ack", "Acknowledgement packet"),
  RST(3, "Reset", "Resets the current session");


  @Getter
  private final int value;
  @Getter
  private final String name;
  @Getter
  private final String description;

  TYPE(int val, String name, String description) {
    value = val;
    this.name = name;
    this.description = description;
  }

  @Override
  public String toString() {
    return "TYPE:" + value + " " + name + " ( " + description + " )";
  }

  public static TYPE valueOf(int val) {
    switch (val) {
      case 0:
        return CON;
      case 1:
        return NON;
      case 2:
        return ACK;
      case 3:
        return RST;

      default:
        return null;
    }
  }

}
