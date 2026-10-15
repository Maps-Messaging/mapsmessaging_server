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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JetStreamArchiveManagerIdentityTest {

  @Test
  void archiveIdentityIsStableAndIsolatedByUserAndStream() {
    String orders = JetStreamArchiveManager.archiveSessionId("alice", "ORDERS");
    assertEquals(orders, JetStreamArchiveManager.archiveSessionId("alice", "ORDERS"));
    assertNotEquals(orders, JetStreamArchiveManager.archiveSessionId("bob", "ORDERS"));
    assertNotEquals(orders, JetStreamArchiveManager.archiveSessionId("alice", "ALERTS"));
    assertTrue(orders.startsWith("nats-jetstream-archive-"));
    assertFalse(orders.contains("alice"));
  }
  @Test
  void archive_rejects_a_connection_without_authenticated_identity() {
    var connection = mock(io.mapsmessaging.network.protocol.impl.nats.state.SessionState.class);
    var session = mock(io.mapsmessaging.api.Session.class);
    var security = mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    when(connection.getSession()).thenReturn(session);
    when(session.getSecurityContext()).thenReturn(security);
    assertThrows(java.io.IOException.class,
        () -> JetStreamArchiveManager.getInstance().ensureArchive(connection, "AUTH_CHECK", "AUTH_CHECK/events"));
    when(security.isLoggedIn()).thenReturn(true);
    assertThrows(java.io.IOException.class,
        () -> JetStreamArchiveManager.getInstance().ensureArchive(connection, "AUTH_CHECK", "AUTH_CHECK/events"));
  }

  @Test
  void cached_and_restored_topic_names_are_rechecked_against_the_current_destination() throws Exception {
    var connection = mock(io.mapsmessaging.network.protocol.impl.nats.state.SessionState.class);
    var parent = mock(io.mapsmessaging.api.Session.class);
    var security = mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    when(connection.getSession()).thenReturn(parent);
    when(parent.getSecurityContext()).thenReturn(security);
    when(security.isLoggedIn()).thenReturn(true);
    when(security.getSubject()).thenReturn(new javax.security.auth.Subject());
    when(security.getUsername()).thenReturn("archive-recreation-test");
    var protocol = mock(io.mapsmessaging.network.protocol.impl.nats.NatsProtocol.class);
    when(protocol.getEndPoint()).thenReturn(mock(io.mapsmessaging.network.io.EndPoint.class));
    when(connection.getProtocol()).thenReturn(protocol);
    String stream = "RECREATED_" + java.util.UUID.randomUUID().toString().replace("-", "");
    String topic = stream + "/events";
    var archive = mock(io.mapsmessaging.api.Session.class);
    String archiveId = JetStreamArchiveManager.archiveSessionId("archive-recreation-test", stream);
    when(archive.getName()).thenReturn(archiveId);
    var firstDestination = mock(io.mapsmessaging.api.Destination.class);
    var replacementDestination = mock(io.mapsmessaging.api.Destination.class);
    when(archive.findDestination(topic, io.mapsmessaging.api.features.DestinationType.TOPIC)).thenReturn(
        java.util.concurrent.CompletableFuture.completedFuture(firstDestination),
        java.util.concurrent.CompletableFuture.completedFuture(replacementDestination));
    var sessions = mock(io.mapsmessaging.api.SessionManager.class);
    when(sessions.createAsync(any(io.mapsmessaging.engine.session.SessionContext.class),
        any(io.mapsmessaging.api.MessageListener.class))).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(archive));
    when(sessions.closeAsync(archive, true)).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(archive));
    var firstManager = mock(io.mapsmessaging.api.SubscribedEventManager.class);
    var replacementManager = mock(io.mapsmessaging.api.SubscribedEventManager.class);
    try (var sessionAccess = mockStatic(io.mapsmessaging.api.SessionManager.class);
         var subscriptions = mockStatic(io.mapsmessaging.api.NatsJetStreamSubscriptions.class)) {
      sessionAccess.when(io.mapsmessaging.api.SessionManager::getInstance).thenReturn(sessions);
      // Simulate a persistent topic name already present before the first lookup.
      subscriptions.when(() -> io.mapsmessaging.api.NatsJetStreamSubscriptions.filters(archive))
          .thenReturn(java.util.Set.of(topic));
      subscriptions.when(() -> io.mapsmessaging.api.NatsJetStreamSubscriptions.ensure(
          eq(archive), eq(firstDestination), any())).thenReturn(firstManager);
      subscriptions.when(() -> io.mapsmessaging.api.NatsJetStreamSubscriptions.ensure(
          eq(archive), eq(replacementDestination), any())).thenReturn(replacementManager);
      try {
        assertEquals(archiveId, JetStreamArchiveManager.getInstance().ensureArchive(connection, stream, topic));
        assertEquals(archiveId, JetStreamArchiveManager.getInstance().ensureArchive(connection, stream, topic));
        verify(archive, times(2)).findDestination(topic, io.mapsmessaging.api.features.DestinationType.TOPIC);
        subscriptions.verify(() -> io.mapsmessaging.api.NatsJetStreamSubscriptions.ensure(
            eq(archive), eq(firstDestination), any()));
        subscriptions.verify(() -> io.mapsmessaging.api.NatsJetStreamSubscriptions.ensure(
            eq(archive), eq(replacementDestination), any()));
      } finally {
        JetStreamArchiveManager.getInstance().releaseStream(stream);
      }
    }
  }

}
