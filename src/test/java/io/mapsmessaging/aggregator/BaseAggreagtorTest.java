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

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.test.BaseTestConfig;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BaseAggreagtorTest extends BaseTestConfig {


  protected Session createSession(String sessionId, MessageListener listener) throws LoginException, IOException {
    return createSession(sessionId, 60, 60, false, listener, true);
  }

  protected void closeSession(Session session) throws IOException {
    super.close(session);
  }

  protected SubscribedEventManager addSubscription(String topicName, Session session) throws IOException {
    SubscriptionContextBuilder scb = new SubscriptionContextBuilder(topicName, ClientAcknowledgement.AUTO);
    SubscriptionContext context = scb.setReceiveMaximum(100).setQos(QualityOfService.AT_MOST_ONCE).build();
    return session.addSubscription(context);
  }

  protected void closeSubscription(SubscribedEventManager subscription, Session session) {
    session.removeSubscription(subscription.getContext().getKey());
  }

  protected void publish(String topicName, byte[] payload, Session session) throws ExecutionException, InterruptedException, TimeoutException {
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(payload);
    publish(topicName, messageBuilder.build(), session);
  }

  protected void publish(String topicName, Message message, Session session) throws ExecutionException, InterruptedException, TimeoutException {
    session.findDestination(topicName, DestinationType.TOPIC).thenApply(destination -> {
      try {
        if (destination != null) {
          destination.storeMessage(message);
        }
      } catch (IOException ioException) {
        throw new RuntimeException(ioException);
      }
      return destination;
    }).get(1, TimeUnit.SECONDS);
  }

}
