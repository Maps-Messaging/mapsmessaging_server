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

package io.mapsmessaging.engine.audit;

import io.mapsmessaging.logging.Category;
import io.mapsmessaging.logging.LEVEL;
import io.mapsmessaging.logging.LogMessage;
import lombok.Getter;

public enum AuditEvent implements LogMessage {

  // User state changes
  SUCCESSFUL_LOGIN("{} successfully logged in", AUDIT_CATEGORY.AUTHENTICATION),
  SUCCESSFUL_LOGOUT("{} successfully logged off", AUDIT_CATEGORY.AUTHENTICATION),


  // Destination
  DESTINATION_CREATED("Destination {} created", AUDIT_CATEGORY.CREATION),
  DESTINATION_DELETED("Destination {} deleted", AUDIT_CATEGORY.DELETION),

  ;


  private final @Getter String message;
  private final @Getter Category category;
  private final @Getter int parameterCount;

  AuditEvent(String message, Category category) {
    this.message = message;
    this.category = category;
    int count = 0;
    int location = message.indexOf("{}");
    while (location != -1) {
      count++;
      location = message.indexOf("{}", location + 2);
    }
    parameterCount = count;
  }

  @Override
  public LEVEL getLevel() {
    return LEVEL.AUDIT;
  }


  public enum AUDIT_CATEGORY implements Category {
    AUTHORISATION("Authorisation"),
    AUTHENTICATION("Authentication"),
    CREATION("Creation"),
    DELETION("Deletion"),
    MODIFICATION("Modification");

    private final @Getter String description;

    public String getDivision() {
      return "Messaging";
    }

    AUDIT_CATEGORY(String description) {
      this.description = description;
    }
  }
}
