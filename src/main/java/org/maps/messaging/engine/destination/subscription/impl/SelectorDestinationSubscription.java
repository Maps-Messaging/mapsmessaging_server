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

package org.maps.messaging.engine.destination.subscription.impl;

import org.maps.logging.LogMessages;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.messaging.engine.destination.subscription.state.MessageStateManager;
import org.maps.messaging.engine.destination.subscription.transaction.AcknowledgementController;
import org.maps.messaging.engine.session.SessionImpl;
import org.maps.selector.ParseException;
import org.maps.selector.operators.ParserExecutor;


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
    try {
      if(selector.evaluate(message)) {
        return super.register(message);
      }
    } catch (ParseException e) {
      logger.log(LogMessages.DESTINATION_SUBSCRIPTION_EXCEPTION_SELECTOR, selector.toString(), e);
    }
    return 0;
  }
}
