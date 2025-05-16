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

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import io.mapsmessaging.api.Session;
import org.apache.qpid.proton.engine.Sender;

import java.util.LinkedHashMap;
import java.util.Map;

public class SubscriptionManager {

  private final Map<String, Sender> subscriptions;

  public SubscriptionManager() {
    subscriptions = new LinkedHashMap<>();
  }

  public void close() {
    for (Map.Entry<String, Sender> entry : subscriptions.entrySet()) {
      Object sessionContext = entry.getValue().getSession().getContext();
      if (sessionContext != null) {
        Session session = (Session) sessionContext;
        session.removeSubscription(entry.getKey());
      }
    }
    subscriptions.clear();
  }

  public synchronized void put(String alias, Sender sender) {
    subscriptions.put(alias, sender);
  }

  public synchronized void remove(String alias) {
    subscriptions.remove(alias);
  }

  public synchronized Sender get(String alias) {
    return subscriptions.get(alias);
  }
}
