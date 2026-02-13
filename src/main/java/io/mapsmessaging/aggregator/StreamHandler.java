/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.aggregator;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorContributionMode;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;

import java.io.IOException;

public class StreamHandler {

  private final AggregatorInputConfigDTO config;
  private SubscribedEventManager subscriptionManager;

  public StreamHandler(AggregatorInputConfigDTO config) {
    this.config = config;
  }

  public String getTopicName() {
    return config.getTopicName();
  }

  public AggregatorContributionMode getContributionMode() {
    return config.getContributionMode();
  }

  public void start(Session session) throws IOException {
    if (config.getTransformer() != null) {
      // TODO build transformer chain
    }
    addSubscription(session);
  }

  public void stop(Session session) {
    if (subscriptionManager != null) {
      session.removeSubscription(subscriptionManager.getContext().getKey());
    }
  }

  /**
   * Called from the single-consumer worker thread.
   * Returns null to indicate the event was dropped (e.g. transformer filtered it out).
   */
  public Message process(MessageEvent event) {
    // TODO apply transformer chain; if it drops -> return null
    return event.getMessage();
  }

  private void addSubscription(Session session) throws IOException {
    SubscriptionContextBuilder builder = new SubscriptionContextBuilder(config.getTopicName(), ClientAcknowledgement.AUTO);
    if (config.getSelector() != null) {
      builder.setSelector(config.getSelector());
    }
    SubscriptionContext context = builder.build();
    subscriptionManager = session.addSubscription(context);
  }
}
