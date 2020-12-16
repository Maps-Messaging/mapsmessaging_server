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

package org.maps.messaging.engine.destination.subscription.tasks;

import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.engine.tasks.EngineTask;
import org.maps.messaging.engine.tasks.Response;
import org.maps.messaging.engine.tasks.VoidResponse;

public class CreditUpdateTask extends EngineTask {


  private final SubscribedEventManager subscription;
  private final int credit;

  public CreditUpdateTask(SubscribedEventManager subscription,  int credit){
    this.subscription = subscription;
    this.credit = credit;
  }

  @Override
  public Response taskCall() {
    subscription.updateCredit(credit);
    return new VoidResponse();
  }
}
