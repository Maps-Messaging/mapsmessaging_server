/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.api.subscriptions;

import io.mapsmessaging.api.MessageAPITest;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.engine.session.FakeProtocolImpl;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.LoginException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.test.WaitForState;

class SelectorTest extends MessageAPITest implements MessageListener {

  private static final int EVENT_COUNT = 1000;

  @Test
  void topicTrueSelectorTest(TestInfo testInfo) throws LoginException, IOException, InterruptedException {
    String destinationName = "topic/selectorTest";
    String name = testInfo.getTestMethod().get().getName();
    SessionContextBuilder scb1 = new SessionContextBuilder(name+"_1", new FakeProtocolImpl(this));
    scb1.setReceiveMaximum(1); // ensure it is low
    scb1.setSessionExpiry(2); // 2 seconds, more then enough time
    scb1.setKeepAlive(60); // large enough to not worry about
    scb1.setPersistentSession(true); // store the details
    Session session1 = createSession(scb1, this);

    SessionContextBuilder scb2 = new SessionContextBuilder(name+"_2", new FakeProtocolImpl(this));
    scb2.setReceiveMaximum(1); // ensure it is low
    scb2.setSessionExpiry(2); // 2 seconds, more then enough time
    scb2.setKeepAlive(60); // large enough to not worry about
    scb2.setPersistentSession(true); // store the details
    Session session2 = createSession(scb2, this);


    // Subscribe to the topic with individual Ack, this causes the server to stop sending events until the client acks them
    SubscriptionContextBuilder subContextBuilder = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.INDIVIDUAL);
    subContextBuilder.setReceiveMaximum(1);
    subContextBuilder.setQos(QualityOfService.AT_LEAST_ONCE);
    subContextBuilder.setSelector("true = true");
    SubscribedEventManager subscription = session1.addSubscription(subContextBuilder.build());
    Assertions.assertNotNull(subscription);

    SubscriptionContextBuilder subContextBuilder2 = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.INDIVIDUAL);
    subContextBuilder2.setReceiveMaximum(1);
    subContextBuilder2.setQos(QualityOfService.AT_LEAST_ONCE);
    subContextBuilder2.setSelector("true = false");
    SubscribedEventManager subscription2 = session2.addSubscription(subContextBuilder2.build());
    Assertions.assertNotNull(subscription2);

    Session publisher = createSession(name, 60, 60, false,this);
    Destination destination = publisher.findDestination(destinationName, DestinationType.TOPIC);
    for(int x=0;x<EVENT_COUNT;x++) {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(("Hi There "+x).getBytes());
      messageBuilder.storeOffline(true);
      Map<String, TypedData> map = new LinkedHashMap<>();
      map.put("odd", new TypedData((x%2) == 1));
      messageBuilder.setDataMap(map);
      messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
      destination.storeMessage(messageBuilder.build());
    }
    close(publisher);

    // OK we have 2 subscribers with different selectors, each take 50% of all events so the store should have 100%
    Assertions.assertEquals(EVENT_COUNT, destination.getStoredMessages());

    //
    // OK lets disconnect one of the subscriptions
    //
    Assertions.assertEquals(EVENT_COUNT, destination.getStoredMessages());
    close(session2);

    // Now we wait for the 5 second disconnect and expiry of the session
    WaitForState.waitFor(4, TimeUnit.SECONDS, () -> destination.getStoredMessages() == EVENT_COUNT);
    Assertions.assertEquals(EVENT_COUNT, destination.getStoredMessages());

    close(session1);
    WaitForState.waitFor(8, TimeUnit.SECONDS, () -> destination.getStoredMessages() == 0);
    Assertions.assertEquals(0, destination.getStoredMessages());
  }

  @Test
  public void topicSelectorDisconnectTest(TestInfo testInfo) throws LoginException, IOException, InterruptedException {
    String destinationName = "topic/selectorTest";
    String name = testInfo.getTestMethod().get().getName();
    SessionContextBuilder scb1 = new SessionContextBuilder(name+"_1", new FakeProtocolImpl(this));
    scb1.setReceiveMaximum(1); // ensure it is low
    scb1.setSessionExpiry(2); // 2 seconds, more then enough time
    scb1.setKeepAlive(60); // large enough to not worry about
    scb1.setPersistentSession(true); // store the details
    Session session1 = createSession(scb1, this);

    SessionContextBuilder scb2 = new SessionContextBuilder(name+"_2", new FakeProtocolImpl(this));
    scb2.setReceiveMaximum(1); // ensure it is low
    scb2.setSessionExpiry(2); // 2 seconds, more then enough time
    scb2.setKeepAlive(60); // large enough to not worry about
    scb2.setPersistentSession(true); // store the details
    Session session2 = createSession(scb2, this);


    // Subscribe to the topic with individual Ack, this causes the server to stop sending events until the client acks them
    SubscriptionContextBuilder subContextBuilder = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.INDIVIDUAL);
    subContextBuilder.setReceiveMaximum(1);
    subContextBuilder.setQos(QualityOfService.AT_LEAST_ONCE);
    subContextBuilder.setSelector("odd = true");
    SubscribedEventManager subscription = session1.addSubscription(subContextBuilder.build());
    Assertions.assertNotNull(subscription);

    SubscriptionContextBuilder subContextBuilder2 = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.INDIVIDUAL);
    subContextBuilder2.setReceiveMaximum(1);
    subContextBuilder2.setQos(QualityOfService.AT_LEAST_ONCE);
    subContextBuilder2.setSelector("odd = false");
    SubscribedEventManager subscription2 = session2.addSubscription(subContextBuilder2.build());
    Assertions.assertNotNull(subscription);

    Session publisher = createSession(name, 60, 60, false, this);
    Destination destination = publisher.findDestination(destinationName, DestinationType.TOPIC);
    for(int x=0;x<EVENT_COUNT;x++) {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(("Hi There "+x).getBytes());
      messageBuilder.storeOffline(true);
      Map<String, TypedData> map = new LinkedHashMap<>();
      map.put("odd", new TypedData((x%2) == 1));
      messageBuilder.setDataMap(map);
      messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
      destination.storeMessage(messageBuilder.build());
    }
    close(publisher);

    // OK we have 2 subscribers with different selectors, each take 50% of all events so the store should have 100%
    Assertions.assertEquals(EVENT_COUNT, destination.getStoredMessages());

    //
    // OK lets disconnect one of the subscriptions
    //
    Assertions.assertEquals(EVENT_COUNT, destination.getStoredMessages());
    close(session2);

    // Now we wait for the 2 second disconnect and expiry of the session
    WaitForState.waitFor(8, TimeUnit.SECONDS, () -> destination.getStoredMessages() == EVENT_COUNT/2);
    Assertions.assertEquals(EVENT_COUNT/2, destination.getStoredMessages());

    close(session1);
    WaitForState.waitFor(8, TimeUnit.SECONDS, () -> destination.getStoredMessages() == 0);
    Assertions.assertEquals(0, destination.getStoredMessages());
  }

  @Test
  public void topicSelectorUnsubscribeTest(TestInfo testInfo) throws LoginException, IOException, InterruptedException {
    String destinationName = "topic/selectorTest";
    String name = testInfo.getTestMethod().get().getName();
    SessionContextBuilder scb1 = new SessionContextBuilder(name+"_1", new FakeProtocolImpl(this));
    scb1.setReceiveMaximum(1); // ensure it is low
    scb1.setSessionExpiry(2); // 2 seconds, more then enough time
    scb1.setKeepAlive(60); // large enough to not worry about
    scb1.setPersistentSession(true); // store the details
    Session session1 = createSession(scb1, this);

    SessionContextBuilder scb2 = new SessionContextBuilder(name+"_2", new FakeProtocolImpl(this));
    scb2.setReceiveMaximum(1); // ensure it is low
    scb2.setSessionExpiry(2); // 2 seconds, more then enough time
    scb2.setKeepAlive(60); // large enough to not worry about
    scb2.setPersistentSession(true); // store the details
    Session session2 = createSession(scb2, this);


    // Subscribe to the topic with individual Ack, this causes the server to stop sending events until the client acks them
    SubscriptionContextBuilder subContextBuilder = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.INDIVIDUAL);
    subContextBuilder.setReceiveMaximum(1);
    subContextBuilder.setQos(QualityOfService.AT_LEAST_ONCE);
    subContextBuilder.setSelector("odd = true");
    SubscribedEventManager subscription = session1.addSubscription(subContextBuilder.build());
    Assertions.assertNotNull(subscription);

    SubscriptionContextBuilder subContextBuilder2 = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.INDIVIDUAL);
    subContextBuilder2.setReceiveMaximum(1);
    subContextBuilder2.setQos(QualityOfService.AT_LEAST_ONCE);
    subContextBuilder2.setSelector("odd = false");
    SubscribedEventManager subscription2 = session2.addSubscription(subContextBuilder2.build());
    Assertions.assertNotNull(subscription2);

    Session publisher = createSession(name, 60, 60, false, this);
    Destination destination = publisher.findDestination(destinationName, DestinationType.TOPIC);
    for(int x=0;x<EVENT_COUNT;x++) {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(("Hi There "+x).getBytes());
      messageBuilder.storeOffline(true);
      Map<String, TypedData> map = new LinkedHashMap<>();
      map.put("odd", new TypedData((x%2) == 1));
      messageBuilder.setDataMap(map);
      messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
      destination.storeMessage(messageBuilder.build());
    }
    close(publisher);


    // OK we have 2 subscribers with different selectors, each take 50% of all events so the store should have 100%
    Assertions.assertEquals(EVENT_COUNT, destination.getStoredMessages());

    session1.removeSubscription(destinationName);
    WaitForState.waitFor(500, TimeUnit.MILLISECONDS, () -> destination.getStoredMessages() == (EVENT_COUNT/2));
    Assertions.assertEquals(EVENT_COUNT/2, destination.getStoredMessages());

    session2.removeSubscription(destinationName);
    WaitForState.waitFor(100, TimeUnit.MILLISECONDS, () -> destination.getStoredMessages() == 0);
    Assertions.assertEquals(0, destination.getStoredMessages());
  }

  @Test
  public void topicSelectorOverlappedUnsubscribeTest(TestInfo testInfo) throws LoginException, IOException, InterruptedException {
    String destinationName = "topic/selectorTest";
    String name = testInfo.getTestMethod().get().getName();
    SessionContextBuilder scb1 = new SessionContextBuilder(name+"_1", new FakeProtocolImpl(this));
    scb1.setReceiveMaximum(1); // ensure it is low
    scb1.setSessionExpiry(2); // 2 seconds, more then enough time
    scb1.setKeepAlive(60); // large enough to not worry about
    scb1.setPersistentSession(true); // store the details
    Session session1 = createSession(scb1, this);

    SessionContextBuilder scb2 = new SessionContextBuilder(name+"_2", new FakeProtocolImpl(this));
    scb2.setReceiveMaximum(1); // ensure it is low
    scb2.setSessionExpiry(2); // 2 seconds, more then enough time
    scb2.setKeepAlive(60); // large enough to not worry about
    scb2.setPersistentSession(true); // store the details
    Session session2 = createSession(scb2, this);


    // Subscribe to the topic with individual Ack, this causes the server to stop sending events until the client acks them
    SubscriptionContextBuilder subContextBuilder = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.INDIVIDUAL);
    subContextBuilder.setReceiveMaximum(1);
    subContextBuilder.setQos(QualityOfService.AT_LEAST_ONCE);
    subContextBuilder.setSelector("value = 0 OR value = 1");
    SubscribedEventManager subscription = session1.addSubscription(subContextBuilder.build());
    Assertions.assertNotNull(subscription);

    SubscriptionContextBuilder subContextBuilder2 = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.INDIVIDUAL);
    subContextBuilder2.setReceiveMaximum(1);
    subContextBuilder2.setQos(QualityOfService.AT_LEAST_ONCE);
    subContextBuilder2.setSelector("value = 1 or value = 2");
    SubscribedEventManager subscription2 = session2.addSubscription(subContextBuilder2.build());
    Assertions.assertNotNull(subscription2);

    Session publisher = createSession(name, 60, 60, false, this);
    Destination destination = publisher.findDestination(destinationName, DestinationType.TOPIC);
    int roundOut = EVENT_COUNT/3;
    roundOut = roundOut *3;
    for(int x=0;x<roundOut;x++) {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(("Hi There "+x).getBytes());
      messageBuilder.storeOffline(true);
      Map<String, TypedData> map = new LinkedHashMap<>();
      map.put("value", new TypedData(x%3));
      messageBuilder.setDataMap(map);
      messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
      destination.storeMessage(messageBuilder.build());
    }
    close(publisher);


    // OK we have 2 subscribers with different selectors, each take 50% of all events so the store should have 100%
    Assertions.assertEquals(roundOut, destination.getStoredMessages());
    session1.removeSubscription(destinationName);
    int rounded = roundOut;
    WaitForState.waitFor(500, TimeUnit.MILLISECONDS, () -> destination.getStoredMessages() == (2 * rounded/3));
    Assertions.assertEquals(2 * roundOut/3, destination.getStoredMessages());

    session2.removeSubscription(destinationName);
    WaitForState.waitFor(500, TimeUnit.MILLISECONDS, () -> destination.getStoredMessages() == 0);
    Assertions.assertEquals(0, destination.getStoredMessages());
  }

  @Override
  public void sendMessage(@NotNull Destination destination, @NotNull String normalisedName, @NotNull SubscribedEventManager subscription, @NotNull Message message, @NotNull Runnable completionTask) {
    System.err.println("Received from "+destination.getName()+" for "+subscription.getContext().getAlias());
  }
}
