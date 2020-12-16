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
import org.maps.messaging.engine.destination.subscription.impl.DestinationSubscription;
import org.maps.messaging.engine.destination.subscription.impl.SelectorDestinationSubscription;
import org.maps.messaging.engine.destination.subscription.state.MessageStateManagerImpl;
import org.maps.messaging.engine.destination.subscription.transaction.AcknowledgementController;
import org.maps.messaging.engine.session.SessionImpl;

public class StandardSubscriptionBuilder  extends SubscriptionBuilder {

  private final boolean isPersistent;

  public StandardSubscriptionBuilder(DestinationImpl destination, SubscriptionContext context, boolean isPersistent)  throws IOException {
    super(destination, context);
    this.isPersistent = isPersistent;
  }

  public Subscription construct(SessionImpl session, String sessionId) throws IOException {
    AcknowledgementController acknowledgementController = createAcknowledgementController(context.getAcknowledgementController());
    MessageStateManagerImpl stateManager = DestinationStateManagerFactory.getInstance().create(destination, isPersistent, sessionId);
    if (parserExecutor == null) {
      return new DestinationSubscription(destination, context, session, sessionId, acknowledgementController, stateManager);
    } else {
      return new SelectorDestinationSubscription(destination, context, session, sessionId, acknowledgementController, stateManager, parserExecutor);
    }
  }
}
