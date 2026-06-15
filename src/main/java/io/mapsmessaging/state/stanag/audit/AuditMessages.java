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

package io.mapsmessaging.state.stanag.audit;

import io.mapsmessaging.logging.Category;
import io.mapsmessaging.logging.LEVEL;
import io.mapsmessaging.logging.LogMessage;
import lombok.Getter;

public enum AuditMessages implements LogMessage {

  STANAG_COMMAND_RECEIVED(LEVEL.AUDIT, CATEGORY.C2, "Received STANAG task {} for drone {} command {}"),
  STANAG_COMMAND_REJECTED(LEVEL.AUDIT, CATEGORY.C2, "Rejected STANAG task {} for drone {} reason {}"),
  STANAG_COMMAND_TRANSLATED(LEVEL.AUDIT, CATEGORY.C2, "Translated STANAG task {} to drone command {} for drone {}"),
  DRONE_COMMAND_DISPATCHED(LEVEL.AUDIT, CATEGORY.C2, "Dispatched drone command {} to drone {} command {}"),
  DRONE_COMMAND_ACKNOWLEDGED(LEVEL.AUDIT, CATEGORY.C2, "Acknowledged drone command {} for drone {} command {}"),
  DRONE_COMMAND_FAILED(LEVEL.AUDIT, CATEGORY.C2, "Failed drone command {} for drone {} reason {}");

  private final @Getter LEVEL level;
  private final @Getter Category category;
  private final @Getter String message;
  private final @Getter int parameterCount;

  AuditMessages(
      LEVEL level,
      Category category,
      String message
  ) {
    this.level = level;
    this.category = category;
    this.message = message;
    this.parameterCount = countParameters(message);
  }

  private int countParameters(String message) {
    int count = 0;
    int location = message.indexOf("{}");

    while (location != -1) {
      count++;
      location = message.indexOf("{}", location + 2);
    }

    return count;
  }

  private enum CATEGORY implements Category {

    C2("Command and Control");

    private final @Getter String description;

    CATEGORY(String description) {
      this.description = description;
    }

    @Override
    public String getDivision() {
      return "Audit";
    }
  }
}