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
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.impl.SelectorDestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.session.SessionImpl;

import java.io.IOException;

public class StandardSubscriptionBuilder extends SubscriptionBuilder {

  private final boolean isPersistent;

  public StandardSubscriptionBuilder(DestinationImpl destination, SubscriptionContext context, boolean isPersistent) throws IOException {
    super(destination, context);
    this.isPersistent = destination.isPersistent() && isPersistent;
  }

  public Subscription construct(SessionImpl session, String sessionId, String uniqueSessionId, long sessionUniqueId) throws IOException {
    AcknowledgementController acknowledgementController = createAcknowledgementController(context.getAcknowledgementController());
    MessageStateManagerImpl stateManager = DestinationStateManagerFactory.create(destination, isPersistent, uniqueSessionId, sessionUniqueId, context.getMaxAtRest());

    if (parserExecutor == null) {
      return new DestinationSubscription(destination, context, session, sessionId, acknowledgementController, stateManager);
    } else {
      return new SelectorDestinationSubscription(destination, context, session, sessionId, acknowledgementController, stateManager, parserExecutor);
    }
  }
}
