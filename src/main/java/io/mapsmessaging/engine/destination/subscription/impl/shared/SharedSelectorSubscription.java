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

package io.mapsmessaging.engine.destination.subscription.impl.shared;

import io.mapsmessaging.api.message.Filter;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManager;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.selector.operators.ParserExecutor;

public class SharedSelectorSubscription extends SharedSubscription {

  private final ParserExecutor parserExecutor;

  public SharedSelectorSubscription(DestinationImpl destinationImpl,
      SubscriptionContext info,
      ParserExecutor parserExecutor,
      String id,
      MessageStateManager messageStateManager,
      AcknowledgementController acknowledgementController,
      String name) {
    super(destinationImpl, info, id, messageStateManager, acknowledgementController, name);
    this.parserExecutor = parserExecutor;
  }

  @Override
  public int register(Message message) {
    if (Filter.getInstance().filterMessage(parserExecutor, message, getDestinationImpl())) {
      return super.register(message);
    }
    return 0;
  }
}
