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

package io.mapsmessaging.engine.destination.subscription.builders;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStateManagerFactory;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSelectorSubscription;
import io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscription;
import io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscriptionManager;
import io.mapsmessaging.engine.destination.subscription.state.BoundedMessageStateManager;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManager;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.engine.destination.subscription.state.ProxyMessageStateManager;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.destination.subscription.transaction.AutoAcknowledgementController;
import io.mapsmessaging.engine.destination.subscription.transaction.CreditManager;
import io.mapsmessaging.engine.destination.subscription.transaction.FixedCreditManager;
import io.mapsmessaging.engine.session.SessionImpl;

import java.io.IOException;

public abstract class CommonSubscriptionBuilder extends SubscriptionBuilder {

  protected CommonSubscriptionBuilder(DestinationImpl destination, SubscriptionContext context) throws IOException {
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
    if (parserExecutor != null) {
      lookupName += "_selector_" + parserExecutor.hashCode();
    } else {
      lookupName += "_normal";
    }
    while (lookupName.contains("/")) {
      lookupName = lookupName.replace("/", "");
    }

    while (lookupName.contains("\\")) {
      lookupName = lookupName.replace("\\", "");
    }
    lookupName = lookupName.trim();

    SharedSubscription sharedSubscription = sharedSubscriptionManager.find(lookupName);
    if (sharedSubscription == null) {
      // Will need to create a new one here for each shared subscription!!
      // ToDo!!!!
      MessageStateManagerImpl messageStateManager = DestinationStateManagerFactory.create(destination, true, "shared_" + lookupName, 0L, 0);
      BoundedMessageStateManager boundedMessageStateManager = sharedSubscriptionManager.getMessageStateManager();
      boundedMessageStateManager.add(messageStateManager);
      MessageStateManager proxy = new ProxyMessageStateManager(messageStateManager, boundedMessageStateManager);
      CreditManager creditManager = new FixedCreditManager(1024);
      AcknowledgementController acknowledgementController = new AutoAcknowledgementController(creditManager);
      if (parserExecutor != null) {
        sharedSubscription = new SharedSelectorSubscription(destination, context, parserExecutor, sessionId, proxy, acknowledgementController, lookupName);
      } else {
        sharedSubscription = new SharedSubscription(destination, context, sessionId, messageStateManager, acknowledgementController, lookupName);
      }
      sharedSubscriptionManager.add(lookupName, sharedSubscription);
    }

    // Now add this session to the shared subscription
    return sharedSubscription.addSession(session, sessionId, context, createAcknowledgementController(context.getAcknowledgementController()));
  }
}
