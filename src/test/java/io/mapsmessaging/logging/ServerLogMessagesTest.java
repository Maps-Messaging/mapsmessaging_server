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

package io.mapsmessaging.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ServerLogMessagesTest {

  @Test
  void logMessages_reportPlaceholderCountForEveryMessage() {
    for (ServerLogMessages logMessage : ServerLogMessages.values()) {
      Assertions.assertEquals(
          countPlaceholders(logMessage.getMessage()),
          logMessage.getParameterCount(),
          logMessage.name()
      );
    }
  }

  @Test
  void serverCategories_exposeMessagingDivisionAndExpectedDescriptions() {
    Map<ServerLogMessages.SERVER_CATEGORY, String> expectedDescriptions = Map.ofEntries(
        Map.entry(ServerLogMessages.SERVER_CATEGORY.TEST, "Test"),
        Map.entry(ServerLogMessages.SERVER_CATEGORY.AUTHORISATION, "Authorisation"),
        Map.entry(ServerLogMessages.SERVER_CATEGORY.AUTHENTICATION, "Authentication"),
        Map.entry(ServerLogMessages.SERVER_CATEGORY.NETWORK, "Network"),
        Map.entry(ServerLogMessages.SERVER_CATEGORY.PROTOCOL, "Protocol"),
        Map.entry(ServerLogMessages.SERVER_CATEGORY.DISCOVERY, "Discovery"),
        Map.entry(ServerLogMessages.SERVER_CATEGORY.REST, "Rest"),
        Map.entry(ServerLogMessages.SERVER_CATEGORY.TRANSFORMATION, "Transformation"),
        Map.entry(ServerLogMessages.SERVER_CATEGORY.DEVICE, "Device"),
        Map.entry(ServerLogMessages.SERVER_CATEGORY.LICENSE, "License"),
        Map.entry(ServerLogMessages.SERVER_CATEGORY.DAEMON, "Daemon"),
        Map.entry(ServerLogMessages.SERVER_CATEGORY.STATE, "State"),
        Map.entry(ServerLogMessages.SERVER_CATEGORY.ENGINE, "Engine")
    );

    Assertions.assertEquals(ServerLogMessages.SERVER_CATEGORY.values().length, expectedDescriptions.size());
    for (ServerLogMessages.SERVER_CATEGORY category : ServerLogMessages.SERVER_CATEGORY.values()) {
      Assertions.assertAll(category.name(),
          () -> Assertions.assertEquals("Messaging", category.getDivision()),
          () -> Assertions.assertEquals(expectedDescriptions.get(category), category.getDescription())
      );
    }
  }

  @Test
  void representativeMessages_exposeExpectedLoggingContract() {
    Assertions.assertAll(
        () -> Assertions.assertEquals(LEVEL.WARN, ServerLogMessages.MESSAGE_DAEMON_STARTUP.getLevel()),
        () -> Assertions.assertSame(
            ServerLogMessages.SERVER_CATEGORY.DAEMON,
            ServerLogMessages.MESSAGE_DAEMON_STARTUP.getCategory()
        ),
        () -> Assertions.assertEquals(2, ServerLogMessages.MESSAGE_DAEMON_STARTUP.getParameterCount()),
        () -> Assertions.assertEquals(LEVEL.ERROR, ServerLogMessages.REST_API_FAILURE.getLevel()),
        () -> Assertions.assertSame(
            ServerLogMessages.SERVER_CATEGORY.REST,
            ServerLogMessages.REST_API_FAILURE.getCategory()
        ),
        () -> Assertions.assertEquals(0, ServerLogMessages.REST_API_FAILURE.getParameterCount())
    );
  }

  private int countPlaceholders(String message) {
    int parameterCount = 0;
    int location = message.indexOf("{}");
    while (location != -1) {
      parameterCount++;
      location = message.indexOf("{}", location + 2);
    }
    return parameterCount;
  }
}
