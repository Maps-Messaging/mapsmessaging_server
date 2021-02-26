/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.messaging.engine.destination.tasks;

import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.delayed.MessageManager;
import org.maps.messaging.engine.destination.subscription.DestinationSubscriptionManager;
import org.maps.messaging.engine.tasks.LongResponse;
import org.maps.messaging.engine.tasks.Response;

public abstract class MessageProcessor extends SubscriptionTask {

  protected final DestinationImpl destination;
  protected final MessageManager messageManager;
  protected final DestinationSubscriptionManager subscriptionManager;
  private final long bucketId;

  protected MessageProcessor(DestinationImpl destination, DestinationSubscriptionManager subscriptionManager,MessageManager messageManager, long bucketId){
    super();
    this.destination = destination;
    this.messageManager = messageManager;
    this.subscriptionManager = subscriptionManager;
    this.bucketId = bucketId;
  }

  protected abstract long processMessage(Message message);

  @Override
  public Response taskCall() throws Exception {
    long counter = 0;
    while(true) {
      long messageId =messageManager.getNext(bucketId);
      if(messageId > -1) {
        Message message = destination.getMessage(messageId);
        if(message != null){
          counter += processMessage(message);
        }
        messageManager.remove(bucketId, messageId);
      }
      else{
        break;
      }
    }
    return new LongResponse(counter);
  }
}
