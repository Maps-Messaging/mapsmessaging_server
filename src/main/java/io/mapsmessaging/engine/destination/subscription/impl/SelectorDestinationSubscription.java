/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.engine.destination.subscription.impl;

import io.mapsmessaging.api.message.Filter;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManager;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.selector.operators.ParserExecutor;


public class SelectorDestinationSubscription extends DestinationSubscription {

  private final ParserExecutor selector;

  public SelectorDestinationSubscription(
      DestinationImpl destinationImpl,
      SubscriptionContext context,
      SessionImpl sessionImpl,
      String sessionid,
      AcknowledgementController acknowledgementController,
      MessageStateManager messageStateManager,
      ParserExecutor selector) {
    super(destinationImpl, context, sessionImpl, sessionid, acknowledgementController, messageStateManager);
    this.selector = selector;
  }

  @Override
  public int register(Message message) {
    if (Filter.getInstance().filterMessage(selector, message, getDestinationImpl())) {
      return super.register(message);
    }
    return 0;
  }

}
