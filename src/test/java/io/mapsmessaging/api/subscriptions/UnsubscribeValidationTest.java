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

package io.mapsmessaging.api.subscriptions;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.FakeProtocol;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.LoginException;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class UnsubscribeValidationTest extends MessageAPITest implements MessageListener {

  private static final int EVENT_COUNT = 1000;

  void topicUnsubscribeEventCleanUp(TestInfo testInfo) throws LoginException, IOException {
    eventCleanUpTest(testInfo.getTestMethod().get().getName(), "topic/topic1", false, true, false);

  }

  @Test
  void queueUnsubscribeEventCleanUp(TestInfo testInfo) throws LoginException, IOException {
    eventCleanUpTest(testInfo.getTestMethod().get().getName(), "queue/queue1", true, true, false);
  }

  void topicDisconnectEventCleanUp(TestInfo testInfo) throws LoginException, IOException {
    eventCleanUpTest(testInfo.getTestMethod().get().getName(), "topic/topic1", false, false, false);

  }

  @Test
  void queueDisconnectEventCleanUp(TestInfo testInfo) throws LoginException, IOException {
    eventCleanUpTest(testInfo.getTestMethod().get().getName(), "queue/queue1", true, false, false);
  }


  void topicResumeEventCleanUp(TestInfo testInfo) throws LoginException, IOException {
    eventCleanUpTest(testInfo.getTestMethod().get().getName(), "topic/topic1", false, false, true);
  }

  @Test
  void queueResumeEventCleanUp(TestInfo testInfo) throws LoginException, IOException {
    eventCleanUpTest(testInfo.getTestMethod().get().getName(), "queue/queue1", true, false, true);
  }


  @SneakyThrows
  public void eventCleanUpTest(String name, String destinationName, boolean isQueue, boolean unsubscribe, boolean resumeSession) throws LoginException, IOException {
    SessionContextBuilder scb1 = new SessionContextBuilder(name+"_1", new ProtocolClientConnection(new FakeProtocol(this)));
    scb1.setReceiveMaximum(1); // ensure it is low
    scb1.setSessionExpiry(2); // 2 seconds, more than enough time
    scb1.setPersistentSession(true); // store the details

    Session delayedSession1 = createSession(scb1, this);

    SessionContextBuilder scb2 = new SessionContextBuilder(name+"_2", new ProtocolClientConnection(new FakeProtocol(this)));
    scb2.setReceiveMaximum(1); // ensure it is low
    scb2.setSessionExpiry(2); // 2 seconds, more than enough time
    scb2.setPersistentSession(true); // store the details
    Session delayedSession2 = createSession(scb2, this);

    Session publisher = createSession(name, 60, 60, false, this);
    DestinationType destinationType = DestinationType.TOPIC;
    if(isQueue){
      destinationType = DestinationType.QUEUE;
    }
    Destination destination = publisher.findDestination(destinationName, destinationType).get();
    Assertions.assertEquals(destination.getResourceType(), destinationType);

    // Subscribe to the topic with individual Ack, this causes the server to stop sending events until the client acks them
    SubscriptionContext context = createContext(destinationName, ClientAcknowledgement.INDIVIDUAL, QualityOfService.AT_LEAST_ONCE, 1);
    SubscribedEventManager subscription = delayedSession1.addSubscription(context);
    Assertions.assertNotNull(subscription);

    for(int x=0;x<EVENT_COUNT;x++) {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(("Hi There "+x).getBytes());
      messageBuilder.storeOffline(true);
      messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
      destination.storeMessage(messageBuilder.build());

      // Add a second subscription 1/2 way through
      if(x == EVENT_COUNT/2){
        context = createContext(destinationName, ClientAcknowledgement.INDIVIDUAL, QualityOfService.AT_LEAST_ONCE, 1);
        SubscribedEventManager subscription2 = delayedSession2.addSubscription(context);
        Assertions.assertNotNull(subscription2);
      }
    }
    if(resumeSession){
      // This should put BOTH sessions into hibernate BUT NOT lose any events
      close(delayedSession1);
      close(delayedSession2);

      //Confirm the events are where they are meant to be
      Assertions.assertEquals(EVENT_COUNT, destination.getStoredMessages());

      // Restart the sessions
      delayedSession1 = createSession(scb1, this);
      delayedSession2 = createSession(scb2, this);
    }

    // At this point we should have a large number of events pending for the subscription, so let's unsubscribe and see
    Assertions.assertEquals(EVENT_COUNT, destination.getStoredMessages());
    if(unsubscribe) {
      delayedSession1.removeSubscription(destinationName);
      if (isQueue) {
        Assertions.assertEquals(EVENT_COUNT, destination.getStoredMessages());
      } else {
        Assertions.assertEquals(EVENT_COUNT / 2 - 1, destination.getStoredMessages());
      }
    }
    close(delayedSession1);
    close(delayedSession2);
    if(isQueue) {
      // Since we auto store events on queues, the only way to remove them is to deliver and ack them
      Assertions.assertEquals(EVENT_COUNT, destination.getStoredMessages());
      context = createContext(destinationName, ClientAcknowledgement.AUTO, QualityOfService.AT_LEAST_ONCE, 10);
      subscription = publisher.addSubscription(context);
      Assertions.assertNotNull(subscription);
      WaitForState.waitFor(2, TimeUnit.SECONDS, () -> destination.getStoredMessages() == 0);
    }

    WaitForState.waitFor(7, TimeUnit.SECONDS, () -> destination.getStoredMessages() == 0);
    Assertions.assertEquals(0, destination.getStoredMessages());
    close(publisher);
  }

  private SubscriptionContext createContext(String destinationName,ClientAcknowledgement clientAcknowledgement,  QualityOfService qos, int receiveMax){
    SubscriptionContextBuilder subContextBuilder2 = new SubscriptionContextBuilder(destinationName, clientAcknowledgement);
    subContextBuilder2.setReceiveMaximum(receiveMax);
    subContextBuilder2.setQos(qos);
    return subContextBuilder2.build();


  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    messageEvent.getCompletionTask().run();
  }
}
