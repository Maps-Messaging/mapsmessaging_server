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

package org.maps.messaging.api.subscriptions;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;
import javax.security.auth.login.LoginException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.MessageAPITest;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.MessageListener;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionContextBuilder;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.api.SubscriptionContextBuilder;
import org.maps.messaging.api.features.ClientAcknowledgement;
import org.maps.messaging.api.features.DestinationType;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.session.FakeProtocolImpl;

public class UnsubscribeValidation extends MessageAPITest implements MessageListener {

  private static final int EVENT_COUNT = 10000;

  @Test
  public void topicUnsubscribeEventCleanUp(TestInfo testInfo) throws LoginException, IOException {
    eventCleanUpTest(testInfo.getTestMethod().get().getName(), "topic/topic1", false, true, false);

  }

  @Test
  public void queueUnsubscribeEventCleanUp(TestInfo testInfo) throws LoginException, IOException {
    eventCleanUpTest(testInfo.getTestMethod().get().getName(), "queue/queue1", true, true, false);
  }

  @Test
  public void topicDisconnectEventCleanUp(TestInfo testInfo) throws LoginException, IOException {
    eventCleanUpTest(testInfo.getTestMethod().get().getName(), "topic/topic1", false, false, false);

  }

  @Test
  public void queueDisconnectEventCleanUp(TestInfo testInfo) throws LoginException, IOException {
    eventCleanUpTest(testInfo.getTestMethod().get().getName(), "queue/queue1", true, false, false);
  }


  @Test
  public void topicResumeEventCleanUp(TestInfo testInfo) throws LoginException, IOException {
    eventCleanUpTest(testInfo.getTestMethod().get().getName(), "topic/topic1", false, false, true);

  }

  @Test
  public void queueResumeEventCleanUp(TestInfo testInfo) throws LoginException, IOException {
    eventCleanUpTest(testInfo.getTestMethod().get().getName(), "queue/queue1", true, false, true);
  }


  public void eventCleanUpTest(String name, String destinationName, boolean isQueue, boolean unsubscribe, boolean resumeSession) throws LoginException, IOException {
    SessionContextBuilder scb1 = new SessionContextBuilder(name+"_1", new FakeProtocolImpl(this));
    scb1.setReceiveMaximum(1); // ensure it is low
    scb1.setSessionExpiry(2); // 2 seconds, more then enough time
    scb1.setKeepAlive(120); // large enough to not worry about
    scb1.setPersistentSession(true); // store the details

    Session delayedSession1 = createSession(scb1, this);

    SessionContextBuilder scb2 = new SessionContextBuilder(name+"_2", new FakeProtocolImpl(this));
    scb2.setReceiveMaximum(1); // ensure it is low
    scb2.setSessionExpiry(2); // 2 seconds, more then enough time
    scb2.setKeepAlive(120); // large enough to not worry about
    scb2.setPersistentSession(true); // store the details
    Session delayedSession2 = createSession(scb2, this);

    Session publisher = createSession(name, 60, 60, false, this);
    DestinationType destinationType = DestinationType.TOPIC;
    if(isQueue){
      destinationType = DestinationType.QUEUE;
    }
    Destination destination = publisher.findDestination(destinationName, destinationType);
    Assertions.assertEquals(destination.getResourceType(), destinationType);

    // Subscribe to the topic with individual Ack, this causes the server to stop sending events until the client acks them
    SubscriptionContextBuilder subContextBuilder = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.INDIVIDUAL);
    subContextBuilder.setReceiveMaximum(1);
    subContextBuilder.setQos(QualityOfService.AT_LEAST_ONCE);
    SubscribedEventManager subscription = delayedSession1.addSubscription(subContextBuilder.build());
    Assertions.assertNotNull(subscription);

    for(int x=0;x<EVENT_COUNT;x++) {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(("Hi There "+x).getBytes());
      messageBuilder.storeOffline(true);
      messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
      destination.storeMessage(messageBuilder.build());

      // Add a second subscription 1/2 way through
      if(x == EVENT_COUNT/2){
        SubscriptionContextBuilder subContextBuilder2 = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.INDIVIDUAL);
        subContextBuilder.setReceiveMaximum(1);
        subContextBuilder.setQos(QualityOfService.AT_LEAST_ONCE);
        SubscribedEventManager subscription2 = delayedSession2.addSubscription(subContextBuilder2.build());
        Assertions.assertNotNull(subscription);
      }
    }
    if(resumeSession){
      close(delayedSession1);
      close(delayedSession2);
      Assertions.assertEquals(EVENT_COUNT, destination.getStoredMessages());

      // This should put BOTH sessions into hibernate BUT NOT lose any events
      delayedSession1 = createSession(scb1, this);
      delayedSession2 = createSession(scb2, this);
    }

    // At this point we should have a large number of events pending for the subscription, so lets unsubscribe and see
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
      subContextBuilder = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.AUTO);
      subContextBuilder.setReceiveMaximum(1);
      subContextBuilder.setQos(QualityOfService.AT_LEAST_ONCE);
      subscription = publisher.addSubscription(subContextBuilder.build());
      Assertions.assertNotNull(subscription);
      int count =0;
      while(destination.getStoredMessages() != 0 && count < 20) {
        LockSupport.parkNanos(1000000000);
        count++;
      }

    }
    int count =0;
    while(destination.getStoredMessages() != 0 && count < 20) {
      LockSupport.parkNanos(1000000000);
      count++;
    }
    Assertions.assertEquals(0, destination.getStoredMessages());
    close(publisher);
  }

  @Override
  public void sendMessage(@NotNull Destination destination, @NotNull SubscribedEventManager subscription, @NotNull Message message, @NotNull Runnable completionTask) {
    completionTask.run();
  }
}
