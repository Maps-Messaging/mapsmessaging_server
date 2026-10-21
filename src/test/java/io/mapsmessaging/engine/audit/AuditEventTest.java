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

package io.mapsmessaging.engine.audit;

import io.mapsmessaging.logging.LEVEL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class AuditEventTest {

  @Test
  void auditEvents_exposeExpectedLoggingContract() {
    Map<AuditEvent, ExpectedEvent> expectedEvents = Map.of(
        AuditEvent.SUCCESSFUL_LOGIN, new ExpectedEvent("{} successfully logged in", AuditEvent.AUDIT_CATEGORY.AUTHENTICATION, 1),
        AuditEvent.SUCCESSFUL_LOGOUT, new ExpectedEvent("{} successfully logged off", AuditEvent.AUDIT_CATEGORY.AUTHENTICATION, 1),
        AuditEvent.AUTHORISATION_FAILED, new ExpectedEvent("{} requested {} access to {} but was denied", AuditEvent.AUDIT_CATEGORY.AUTHORISATION, 3),
        AuditEvent.DESTINATION_CREATED, new ExpectedEvent("Destination {} created", AuditEvent.AUDIT_CATEGORY.CREATION, 1),
        AuditEvent.DESTINATION_DELETED, new ExpectedEvent("Destination {} deleted", AuditEvent.AUDIT_CATEGORY.DELETION, 1)
    );

    Assertions.assertEquals(AuditEvent.values().length, expectedEvents.size());
    for (AuditEvent event : AuditEvent.values()) {
      ExpectedEvent expectedEvent = expectedEvents.get(event);

      Assertions.assertNotNull(expectedEvent, "Missing expected contract for " + event);
      Assertions.assertAll(event.name(),
          () -> Assertions.assertEquals(LEVEL.AUDIT, event.getLevel()),
          () -> Assertions.assertEquals(expectedEvent.message(), event.getMessage()),
          () -> Assertions.assertSame(expectedEvent.category(), event.getCategory()),
          () -> Assertions.assertEquals(expectedEvent.parameterCount(), event.getParameterCount())
      );
    }
  }

  @Test
  void auditCategories_exposeMessagingDivisionAndExpectedDescriptions() {
    Map<AuditEvent.AUDIT_CATEGORY, String> expectedDescriptions = Map.of(
        AuditEvent.AUDIT_CATEGORY.AUTHORISATION, "Authorisation",
        AuditEvent.AUDIT_CATEGORY.AUTHENTICATION, "Authentication",
        AuditEvent.AUDIT_CATEGORY.CREATION, "Creation",
        AuditEvent.AUDIT_CATEGORY.DELETION, "Deletion",
        AuditEvent.AUDIT_CATEGORY.MODIFICATION, "Modification"
    );

    Assertions.assertEquals(AuditEvent.AUDIT_CATEGORY.values().length, expectedDescriptions.size());
    for (AuditEvent.AUDIT_CATEGORY category : AuditEvent.AUDIT_CATEGORY.values()) {
      Assertions.assertAll(category.name(),
          () -> Assertions.assertEquals("Messaging", category.getDivision()),
          () -> Assertions.assertEquals(expectedDescriptions.get(category), category.getDescription())
      );
    }
  }

  private record ExpectedEvent(String message, AuditEvent.AUDIT_CATEGORY category, int parameterCount) {
  }
}
