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

package io.mapsmessaging.api;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.engine.tasks.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NatsJetStreamSubscriptionsTest {

  @Test
  void restore_uses_existing_state_without_adding_another_subscription() throws Exception {
    SessionImpl engine = mock(SessionImpl.class);
    SecurityContext security = mock(SecurityContext.class);
    when(engine.getSecurityContext()).thenReturn(security);
    when(engine.getContext()).thenReturn(mock(io.mapsmessaging.engine.session.SessionContext.class));
    when(engine.getName()).thenReturn("durable");
    Session session = new Session(engine, event -> {});
    DestinationImpl resource = mock(DestinationImpl.class);
    when(resource.getResourceType()).thenReturn(io.mapsmessaging.api.features.DestinationType.TOPIC);
    when(resource.getFullyQualifiedNamespace()).thenReturn("EVENTS/orders/created");
    Destination destination = new Destination(resource, security);
    DestinationSubscription existing = mock(DestinationSubscription.class);
    when(existing.isSync()).thenReturn(true);
    when(resource.getSubscription("durable")).thenReturn(existing);
    doAnswer(invocation -> {
      Callable<Response> task = invocation.getArgument(0);
      return CompletableFuture.completedFuture(task.call());
    }).when(resource).submit(any(Callable.class));
    AuthManager auth = mock(AuthManager.class);
    when(auth.canAccess(any(), any(), any())).thenReturn(true);
    try (var authentication = mockStatic(AuthManager.class)) {
      authentication.when(AuthManager::getInstance).thenReturn(auth);
      assertNotNull(NatsJetStreamSubscriptions.restore(session, destination));
      verify(existing).wakeUp(engine);
      verify(engine, never()).addSubscription(any());
    }
  }

  @Test
  void denied_subscribe_permission_prevents_restoration() {
    SessionImpl engine = mock(SessionImpl.class);
    SecurityContext security = mock(SecurityContext.class);
    when(engine.getSecurityContext()).thenReturn(security);
    when(engine.getContext()).thenReturn(mock(io.mapsmessaging.engine.session.SessionContext.class));
    Session session = new Session(engine, event -> {});
    DestinationImpl resource = mock(DestinationImpl.class);
    when(resource.getResourceType()).thenReturn(io.mapsmessaging.api.features.DestinationType.TOPIC);
    when(resource.getFullyQualifiedNamespace()).thenReturn("EVENTS/orders/created");
    Destination destination = new Destination(resource, security);
    AuthManager auth = mock(AuthManager.class);
    try (var authentication = mockStatic(AuthManager.class)) {
      authentication.when(AuthManager::getInstance).thenReturn(auth);
      assertThrows(IOException.class, () -> NatsJetStreamSubscriptions.restore(session, destination));
      verify(resource, never()).submit(any(Callable.class));
    }
  }
  @Test
  void removal_uses_the_persisted_filter_instead_of_the_nats_alias() {
    Session session = mock(Session.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    var context = new io.mapsmessaging.api.SubscriptionContextBuilder(
        "EVENTS/orders/created", io.mapsmessaging.api.features.ClientAcknowledgement.INDIVIDUAL)
        .setAlias("jetstream:EVENTS:worker:0").build();
    when(manager.getContext()).thenReturn(context);
    NatsJetStreamSubscriptions.remove(session, manager);
    verify(session).removeSubscription(context.getFilter());
    verify(session, never()).removeSubscription(context.getAlias());
  }

  @Test
  void archive_filters_include_all_restored_topics_for_later_cleanup() {
    SessionImpl engine = mock(SessionImpl.class);
    var details = new io.mapsmessaging.dto.rest.session.SessionInformationDTO();
    var subscriptions = new io.mapsmessaging.dto.rest.session.SubscriptionInformationDTO();
    details.setSubscriptionInfo(subscriptions);
    when(engine.getSessionInformation()).thenReturn(details);
    var first = new SubscriptionContextBuilder("EVENTS/first", io.mapsmessaging.api.features.ClientAcknowledgement.INDIVIDUAL).build();
    var second = new SubscriptionContextBuilder("EVENTS/second", io.mapsmessaging.api.features.ClientAcknowledgement.INDIVIDUAL).build();
    subscriptions.setSubscriptionContextList(java.util.List.of(first.getDetails(), second.getDetails()));
    Session session = new Session(engine, event -> {});
    assertEquals(java.util.Set.of(first.getFilter(), second.getFilter()), NatsJetStreamSubscriptions.filters(session));
  }

}
