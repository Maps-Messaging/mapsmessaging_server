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

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.engine.Sender;

import java.util.IdentityHashMap;
import java.util.Map;

public class SubscriptionManager {

  private final Map<SubscriptionContext, Sender> subscriptions;

  public SubscriptionManager() {
    subscriptions = new IdentityHashMap<>();
  }

  public synchronized void close() {
    for (Map.Entry<SubscriptionContext, Sender> entry : subscriptions.entrySet()) {
      Object sessionContext = entry.getValue().getSession().getContext();
      if (sessionContext instanceof Session session) {
        String alias = entry.getKey().getAlias();
        if (isDurable(entry.getValue())) {
          session.hibernateSubscription(alias);
        } else {
          session.removeSubscription(alias);
        }
      }
    }
    subscriptions.clear();
  }

  public synchronized void put(SubscribedEventManager manager, Sender sender) {
    put(manager.getContext(), sender);
  }

  public synchronized void put(SubscriptionContext context, Sender sender) {
    subscriptions.put(context, sender);
  }

  public synchronized void remove(SubscribedEventManager manager) {
    remove(manager.getContext());
  }

  public synchronized void remove(SubscriptionContext context) {
    subscriptions.remove(context);
  }

  public synchronized Sender get(SubscribedEventManager manager) {
    Sender sender = subscriptions.get(manager.getContext());
    if (sender != null && sender.getContext() instanceof SubscriptionContext) {
      sender.setContext(manager);
    }
    return sender;
  }

  public static boolean isDurable(Sender sender) {
    if (sender.getSource() instanceof Source source) {
      return source.getDurable() != null && source.getDurable() != TerminusDurability.NONE;
    }
    return false;
  }
}
