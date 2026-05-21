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

package io.mapsmessaging.network.protocol.impl.rest;

import io.mapsmessaging.logging.Category;
import io.mapsmessaging.logging.LEVEL;
import io.mapsmessaging.logging.LogMessage;
import lombok.Getter;

@Getter
public enum RestProtocolLogMessages implements LogMessage {

  //-------------------------------------------------------------------------------------------------------------

  // <editor-fold desc="Generic messages">
  INITIALISE_REST_ENDPOINT(LEVEL.INFO, PULSAR_CATEGORY.PROTOCOL, "Initialising rest endpoint on {}"),
  CLOSE_REST_ENDPOINT(LEVEL.INFO, PULSAR_CATEGORY.PROTOCOL, "Closing rest endpoint on {}"),
  ;

  private final String message;
  private final LEVEL level;
  private final Category category;
  private final int parameterCount;

  RestProtocolLogMessages(LEVEL level, Category category, String message) {
    this.message = message;
    this.level = level;
    this.category = category;
    int location = message.indexOf("{}");
    int count = 0;
    while (location != -1) {
      count++;
      location = message.indexOf("{}", location + 2);
    }
    this.parameterCount = count;
  }

  public enum PULSAR_CATEGORY implements Category {
    PROTOCOL("Protocol");

    private final @Getter String description;

    public String getDivision() {
      return "Inter-Protocol";
    }

    PULSAR_CATEGORY(String description) {
      this.description = description;
    }
  }

}
