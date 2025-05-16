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

package io.mapsmessaging.api;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.tasks.Response;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

/**
 * This interface represents a class that a user of the messaging engine can use to signal completion of event delivery for a subscription. It contains the ability to interegate
 * the context of the subscription.
 */
public interface SubscribedEventManager {

  /**
   * Rolls back the specified message specified in the call. Once the message is rolled back it becomes availabe once more, of it a queue, could be delivered to another client
   *
   * @param messageId The message identifier to roll back
   */
  void rollbackReceived(long messageId);

  /**
   * Acknowledges the specified message so that the reference to the message is removed and if no other subscription has interest in the message then it is deleted from the
   * destination
   *
   * @param messageId The message identifier to acknowledge
   */
  void ackReceived(long messageId);

  /**
   * Retrieves the SubscriptionContext that created this subscription
   *
   * @return The SubscriptionContext that created the subscription
   */
  SubscriptionContext getContext();

  /**
   * If a wildcard subscription is used, then it is possible, to have multiple Subscription Contexts
   *
   * @return The total contexts for the client that maps to this subscription
   */
  List<SubscriptionContext> getContexts();

  /**
   * Updates the "Credit" count for this subscription, used only by CreditHandler.Client, else the server will manage this value automatically
   *
   * @param credit The change to the current credit state, can be both positive and negative
   */
  void updateCredit(int credit);


  /**
   * Returns true if the subscription has no current messages pending
   *
   * @return boolean flag indicating if the subscription has any pending messages
   */
  boolean isEmpty();

  /**
   * Returns the number of outstanding events for this subscription
   *
   * @return Returns the number of events that are currently outstanding
   */
  int getDepth();

  int getPending();

  Future<Response> getNext() throws IOException;
}
