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

public enum Clazz {
  REQUEST(0, "Request", "Request packet"),
  SUCCESS(2, "Success", "Successful response packet"),
  ERROR(4, "Error", "Error response packet"),
  SERVER_ERROR(5, "ServerError", "Server error response packet"),
  SIGNAL(7, "Signal", "Signal packet");


  @Getter
  private final int value;
  @Getter
  private final String name;
  @Getter
  private final String description;

  Clazz(int val, String name, String description) {
    value = val;
    this.name = name;
    this.description = description;
  }

  public static Clazz valueOf(final int value) {
    switch (value) {
      case 0:
        return REQUEST;
      case 2:
        return SUCCESS;
      case 4:
        return ERROR;
      case 5:
        return SERVER_ERROR;
      case 7:
        return SIGNAL;
      default:
        return null;
    }
  }

  @Override
  public String toString() {
    return "Clazz:" + value + " " + name + " ( " + description + " )";
  }
}
