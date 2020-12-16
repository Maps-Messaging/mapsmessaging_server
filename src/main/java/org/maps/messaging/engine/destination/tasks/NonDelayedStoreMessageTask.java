/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.engine.destination.tasks;

import java.util.concurrent.FutureTask;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.subscription.DestinationSubscriptionManager;
import org.maps.messaging.engine.tasks.FutureResponse;
import org.maps.messaging.engine.tasks.LongResponse;
import org.maps.messaging.engine.tasks.Response;

public class NonDelayedStoreMessageTask extends StoreMessageTask {
  private final DestinationImpl destination;
  private final Message message;
  private final DestinationSubscriptionManager subscriptionManager;

  public NonDelayedStoreMessageTask(DestinationImpl destination, DestinationSubscriptionManager subscriptionManager,  Message message){
    super();
    this.destination = destination;
    this.message = message;
    this.subscriptionManager = subscriptionManager;
  }

  @Override
  public Response taskCall() throws Exception {
    if(subscriptionManager.hasSubscriptions()){
      storeMessage(destination, message);
      destination.getStats().messagePublished();
      FutureTask<Response> response = destination.submit(new SubscriptionUpdateTask(destination, subscriptionManager, message));
      return new FutureResponse(response);
    }
    // We are also complete here, no queue required
    destination.getStats().noInterest();
    return new LongResponse(0);
  }
}
