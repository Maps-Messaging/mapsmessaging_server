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

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.transaction.*;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;

import java.io.IOException;

public abstract class SubscriptionBuilder {

  private static final Logger logger = LoggerFactory.getLogger(SubscriptionBuilder.class);

  protected final SubscriptionContext context;
  protected final DestinationImpl destination;
  protected final ParserExecutor parserExecutor;

  protected SubscriptionBuilder(DestinationImpl destination, SubscriptionContext context) throws IOException {
    this.context = context;
    this.destination = destination;
    this.parserExecutor = compileParser(context.getSelector());
  }

  protected SubscriptionBuilder(DestinationImpl destination, SubscriptionContext context, SubscriptionContext parent) throws IOException {
    this.context = context;
    this.destination = destination;
    String selector = context.getSelector();
    String parentSelector = parent.getSelector();
    this.parserExecutor = compileParser(combineSelectors(selector, parentSelector));
  }

  private String combineSelectors(String lhs, String rhs) {
    StringBuilder sb = new StringBuilder();
    boolean hasLhs = false;
    if (lhs != null && !lhs.isEmpty()) {
      sb.append(lhs).append(" ");
      hasLhs = true;
    }

    if (rhs != null && !rhs.isEmpty()) {
      if (hasLhs) {
        sb.append(" and ");
      }
      sb.append(rhs);
    }
    return sb.toString();
  }

  public abstract Subscription construct(SessionImpl session, String sessionId, String uniqueSessionId, long sessionUniqueId) throws IOException;

  protected AcknowledgementController createAcknowledgementController(ClientAcknowledgement acknowledgementController) {
    CreditManager creditManager = createCreditManager(context);
    switch (acknowledgementController) {
      case INDIVIDUAL:
        return new IndividualAcknowledgementController(creditManager);

      case BLOCK:
        return new ClientAcknowledgementController(creditManager);

      case AUTO:
      default:
        return new AutoAcknowledgementController(creditManager);
    }
  }

  protected CreditManager createCreditManager(SubscriptionContext context) {
    switch (context.getCreditHandler()) {
      case CLIENT:
        return new ClientCreditManager(context.getReceiveMaximum());

      case AUTO:
      default:
        return new FixedCreditManager(context.getReceiveMaximum());
    }
  }

  protected ParserExecutor compileParser(String selector) throws IOException {
    ParserExecutor parser;
    if (selector != null && !selector.isEmpty()) {
      try {
        parser = SelectorParser.compile(selector);
      } catch (ParseException e) {
        logger.log(ServerLogMessages.SUBSCRIPTION_MGR_SELECTOR_EXCEPTION, context.getSelector(), e);
        throw new IOException("Failed to parse selector", e);
      }
    } else {
      parser = null;
    }
    return parser;
  }


}
