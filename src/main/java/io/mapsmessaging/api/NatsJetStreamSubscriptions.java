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
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.impl.ClientSubscribedEventManager;
import io.mapsmessaging.engine.tasks.SubscriptionResponse;
import io.mapsmessaging.engine.tasks.Response;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** NATS-only permission and persistent-subscription adapters. */
public final class NatsJetStreamSubscriptions {

  private NatsJetStreamSubscriptions() {
  }

  public static SubscribedEventManager restore(Session session, Destination destination) throws IOException {
    check(session, destination);
    try {
      Response response = destination.destinationImpl.submit(() -> {
        var subscription = destination.destinationImpl.getSubscription(session.getName());
        if (subscription == null) {
          return new SubscriptionResponse(null);
        }
        if (!subscription.isSync()) {
          throw new IOException("JetStream subscription must be synchronous");
        }
        subscription.wakeUp(session.sessionImpl);
        return new SubscriptionResponse(new ClientSubscribedEventManager(destination.destinationImpl, subscription));
      }).get(30, TimeUnit.SECONDS);
      if (response instanceof SubscriptionResponse result) {
        return result.getSubscription();
      }
      throw new IOException("Invalid restored JetStream subscription response");
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted restoring JetStream subscription", interrupted);
    } catch (ExecutionException | TimeoutException failure) {
      throw new IOException("Unable to restore JetStream subscription", failure);
    }
  }

  public static SubscribedEventManager subscribe(Session session, Destination destination,
                                                 SubscriptionContext context) throws IOException {
    check(session, destination);
    return session.addSubscription(context);
  }

  public static SubscribedEventManager ensure(Session session, Destination destination,
                                              SubscriptionContext context) throws IOException {
    SubscribedEventManager existing = restore(session, destination);
    if (existing != null) {
      return existing;
    }
    // A topic name in persisted metadata does not prove that its old destination
    // object still exists. Retire only the stale NATS registration, then bind the
    // ordinary subscription to the current destination through the normal API.
    String topic = destination.getFullyQualifiedNamespace();
    if (filters(session).contains(topic)) {
      session.removeSubscription(topic);
    }
    return subscribe(session, destination, context);
  }

  public static Set<String> filters(Session session) {
    Set<String> filters = new HashSet<>();
    for (var context : session.getSessionInformation().getSubscriptionInfo().getSubscriptionContextList()) {
      filters.add(context.getDestinationName());
    }
    return filters;
  }

  public static void remove(Session session, SubscribedEventManager manager) {
    // MAPS persists and indexes subscriptions by filter, not by the NATS alias.
    if (manager != null && manager.getContext() != null) {
      session.removeSubscription(manager.getContext().getFilter());
    }
  }

  private static void check(Session session, Destination destination) throws IOException {
    var identity = session.getSecurityContext().getIdentity();
    AuthManager auth = AuthManager.getInstance();
    if (!auth.canAccess(identity, ServerPermissions.SUBSCRIBE, destination.protectedResource)
        || (session.sessionImpl.getContext().isPersistentSession()
            && !auth.canAccess(identity, ServerPermissions.DURABLE, destination.protectedResource))) {
      throw new IOException("You don't have permission to subscribe to this JetStream resource");
    }
  }
}
