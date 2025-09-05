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

package io.mapsmessaging.engine.destination.tasks;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.tasks.EngineTask;
import io.mapsmessaging.engine.tasks.MessageResponse;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.engine.tasks.ValueResponse;


public class NextMessageTask extends EngineTask {

  private final DestinationSubscription subscription;

  public NextMessageTask(DestinationSubscription subscription) {
    super();
    this.subscription = subscription;
  }

  @Override
  public Response taskCall() throws Exception {
    if(subscription.getDestinationImpl().isClosed()){
      return new ValueResponse<Message>(null);
    }

    Message msg = subscription.rawGetNext();
    if(msg != null){
      MessageEvent messageEvent = new MessageEvent(subscription.getDestinationImpl().getFullyQualifiedNamespace(), subscription, msg, subscription.getCompletionTask());
      return new MessageResponse(messageEvent);
    }
    return new MessageResponse(null);
  }
}