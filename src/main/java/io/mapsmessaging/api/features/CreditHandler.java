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

/**
 * Specifies if the client is responsible to update the flow control values (credit based) or if the server automatically manages it
 */
public enum CreditHandler {

  AUTO(0, "Auto", "Automatically manages the credit based on the number of unacknowledged events and the initial max outstanding"),
  CLIENT(1, "Client", "The client needs to top up the credit on a regular basis, once it gets to 0 no more events will be sent");


  public static CreditHandler getInstance(int value) {
    switch (value) {
      case 0:
        return AUTO;
      case 1:
        return CLIENT;

      default:
        throw new IllegalArgumentException("Invalid handler value supplied :: " + value);
    }
  }

  @Getter
  private final int value;
  @Getter
  private final String name;
  @Getter
  private final String description;

  CreditHandler(int value, String name, String description) {
    this.value = value;
    this.name = name;
    this.description = description;
  }

}
