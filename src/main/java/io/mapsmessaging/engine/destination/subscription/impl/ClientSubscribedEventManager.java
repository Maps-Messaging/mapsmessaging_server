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

package io.mapsmessaging.engine.destination.subscription.impl;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.tasks.CreditUpdateTask;
import io.mapsmessaging.engine.destination.subscription.tasks.SubscriptionTransactionTask;
import io.mapsmessaging.engine.tasks.Response;
import lombok.Setter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

public class ClientSubscribedEventManager implements SubscribedEventManager {

  private final DestinationImpl destination;
  @Setter
  private SubscribedEventManager subscription;

  public ClientSubscribedEventManager(DestinationImpl destination, SubscribedEventManager subscription) {
    this.subscription = subscription;
    this.destination = destination;
  }

  @Override
  public void rollbackReceived(long messageId) {
    destination.submit(new SubscriptionTransactionTask(subscription, messageId, false));
  }

  @Override
  public void ackReceived(long messageId) {
    destination.submit(new SubscriptionTransactionTask(subscription, messageId, true));
  }

  @Override
  public SubscriptionContext getContext() {
    return subscription.getContext();
  }

  @Override
  public List<SubscriptionContext> getContexts() {
    return subscription.getContexts();
  }

  @Override
  public void updateCredit(int credit) {
    destination.submit(new CreditUpdateTask(subscription, credit));
  }

  @Override
  public boolean isEmpty() {
    return subscription.isEmpty();
  }

  @Override
  public int getPending() {
    return subscription.getPending();
  }

  @Override
  public Future<Response> getNext() throws IOException {
    return subscription.getNext();
  }

  @Override
  public int getDepth() {
    return subscription.getDepth();
  }

}
