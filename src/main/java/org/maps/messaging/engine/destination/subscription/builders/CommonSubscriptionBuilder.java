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

package org.maps.messaging.engine.destination.subscription.builders;

import java.io.IOException;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.DestinationStateManagerFactory;
import org.maps.messaging.engine.destination.subscription.Subscription;
import org.maps.messaging.engine.destination.subscription.SubscriptionBuilder;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.messaging.engine.destination.subscription.impl.shared.SharedSelectorSubscription;
import org.maps.messaging.engine.destination.subscription.impl.shared.SharedSubscription;
import org.maps.messaging.engine.destination.subscription.impl.shared.SharedSubscriptionManager;
import org.maps.messaging.engine.destination.subscription.state.BoundedMessageStateManager;
import org.maps.messaging.engine.destination.subscription.state.MessageStateManager;
import org.maps.messaging.engine.destination.subscription.state.MessageStateManagerImpl;
import org.maps.messaging.engine.destination.subscription.state.ProxyMessageStateManager;
import org.maps.messaging.engine.destination.subscription.transaction.AcknowledgementController;
import org.maps.messaging.engine.destination.subscription.transaction.AutoAcknowledgementController;
import org.maps.messaging.engine.destination.subscription.transaction.CreditManager;
import org.maps.messaging.engine.destination.subscription.transaction.FixedCreditManager;
import org.maps.messaging.engine.session.SessionImpl;

public abstract  class CommonSubscriptionBuilder  extends SubscriptionBuilder {

  public CommonSubscriptionBuilder(DestinationImpl destination, SubscriptionContext context)  throws IOException {
    super(destination, context);
  }

  //
  // Ok this may look confusing, however, we have the key name being the Share Name. Under this we can have multiple subscriptions with different filters or
  // in fact no filters, we now need to find the relevant bucket this subscription falls into or create a new one
  //
  protected Subscription construct(String name, SessionImpl session, String sessionId) throws IOException {
    // Lookup the top level subscription manager for this share name
    SharedSubscriptionManager sharedSubscriptionManager = destination.findShareRegister(name);
    if (sharedSubscriptionManager == null) {
      sharedSubscriptionManager = new SharedSubscriptionManager(name);
      destination.addShareRegistry(sharedSubscriptionManager);
    }

    // Now see if there is an existing subscription that matches this unique instance
    String lookupName = name;
    if(parserExecutor != null){
      lookupName += "_selector_"+parserExecutor.hashCode();
    }
    else{
      lookupName += "_normal";
    }


    SharedSubscription sharedSubscription = sharedSubscriptionManager.find(lookupName);
    if(sharedSubscription == null){
      MessageStateManagerImpl messageStateManager = DestinationStateManagerFactory.getInstance().create(destination, true, "shared_" + lookupName);
      BoundedMessageStateManager boundedMessageStateManager = sharedSubscriptionManager.getMessageStateManager();
      boundedMessageStateManager.add(messageStateManager);
      MessageStateManager proxy = new ProxyMessageStateManager(messageStateManager, boundedMessageStateManager);
      CreditManager creditManager = new FixedCreditManager(1024);
      AcknowledgementController acknowledgementController = new AutoAcknowledgementController(creditManager);
      if (parserExecutor != null) {
        sharedSubscription = new SharedSelectorSubscription(destination, context, parserExecutor, sessionId, proxy, acknowledgementController, lookupName);
      }
      else{
        sharedSubscription = new SharedSubscription(destination, context, sessionId, messageStateManager, acknowledgementController, lookupName);
      }
      sharedSubscriptionManager.add(lookupName, sharedSubscription);
    }

    // Now add this session to the shared subscription
    return sharedSubscription.addSession(session, sessionId, context, createAcknowledgementController(context.getAcknowledgementController()));
  }
}
