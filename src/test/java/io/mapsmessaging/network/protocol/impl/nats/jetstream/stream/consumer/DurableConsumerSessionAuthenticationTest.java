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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocol;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DurableConsumerSessionAuthenticationTest {

  @Test
  void derivesDurableIdentityFromLoggedInMapsSessionWithoutRetainingPassword() throws Exception {
    SessionState connection = connection();
    Session authenticated = mock(Session.class);
    SecurityContext security = mock(SecurityContext.class);
    when(authenticated.getSecurityContext()).thenReturn(security);
    when(security.isLoggedIn()).thenReturn(true);
    when(security.getSubject()).thenReturn(new Subject());
    when(security.getUsername()).thenReturn("alice");
    connection.setSession(authenticated);

    SessionContext durable = DurableConsumerSession.authenticatedContext(connection, "ORDERS", "worker");

    assertEquals(DurableConsumerSession.sessionId("alice", "ORDERS", "worker"), durable.getId());
    assertEquals("alice", durable.getUsername());
    assertNull(durable.getPassword(), "The authenticated client password must never be copied into a durable session");
    assertTrue(durable.isAuthorized(), "MAPS has already authenticated the parent NATS session");
    assertTrue(durable.isPersistentSession());
    assertFalse(durable.isInternal(), "The derived session must still undergo normal MAPS authorization");
    assertEquals(TimeUnit.DAYS.toSeconds(365), durable.getExpiry());
    assertEquals(512, durable.getReceiveMaximum());
  }

  @Test
  void rejectsMissingOrClosedAuthenticatedSession() throws Exception {
    SessionState connection = connection();
    assertThrows(IOException.class,
        () -> DurableConsumerSession.authenticatedContext(connection, "ORDERS", "worker"));

    Session closed = mock(Session.class);
    when(closed.isClosed()).thenReturn(true);
    connection.setSession(closed);
    assertThrows(IOException.class,
        () -> DurableConsumerSession.authenticatedContext(connection, "ORDERS", "worker"));
  }

  @Test
  void rejectsConnectionThatHasNotSuccessfullyAuthenticated() throws Exception {
    SessionState connection = connection();
    Session unverified = mock(Session.class);
    SecurityContext security = mock(SecurityContext.class);
    when(unverified.getSecurityContext()).thenReturn(security);
    connection.setSession(unverified);

    assertThrows(IOException.class,
        () -> DurableConsumerSession.authenticatedContext(connection, "ORDERS", "worker"));

    when(security.isLoggedIn()).thenReturn(true);
    assertThrows(IOException.class,
        () -> DurableConsumerSession.authenticatedContext(connection, "ORDERS", "worker"),
        "A logged-in flag without an authenticated subject must not authorize a durable session");

    when(security.getSubject()).thenReturn(new Subject());
    assertThrows(IOException.class,
        () -> DurableConsumerSession.authenticatedContext(connection, "ORDERS", "worker"),
        "An authenticated session without a user name must not authorize a durable session");
  }

  @Test
  void natsSessionStateDoesNotContainAnIndependentClientPasswordCache() {
    assertFalse(Arrays.stream(SessionState.class.getDeclaredFields())
            .map(Field::getName)
            .map(String::toLowerCase)
            .anyMatch(field -> field.contains("password") || field.contains("credential")),
        "NATS connection state must not retain the CONNECT password for creating durable consumers");
  }

  private static SessionState connection() {
    NatsProtocol protocol = mock(NatsProtocol.class);
    when(protocol.getEndPoint()).thenReturn(mock(EndPoint.class));
    when(protocol.getMaxReceiveSize()).thenReturn(512);
    return new SessionState(protocol);
  }
}
