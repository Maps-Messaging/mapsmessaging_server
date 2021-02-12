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

package org.maps.messaging.api;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.maps.messaging.api.features.ClientAcknowledgement;
import org.maps.messaging.api.features.DestinationType;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.subscription.SubscriptionController;
import org.maps.messaging.engine.session.ProtocolMessageListener;
import org.maps.messaging.engine.session.SessionManagerTest;

public class SessionTest extends MessageAPITest implements ProtocolMessageListener {


  private final AtomicInteger receivedKeepAlive  = new AtomicInteger(0);
  private final Queue<MessageDetails> messageList = new ConcurrentLinkedQueue<>();


  @Test
  @DisplayName("Simple session construction tests")
  public void sessionConstructionTest(TestInfo testInfo) throws Exception {
    Session session = createSession(testInfo.getTestMethod().get().getName(), 5, 2, false, this);
    Assertions.assertTrue(SessionManagerTest.getInstance().hasSessions());
    Destination destinationImpl = session.findDestination("topic1", DestinationType.TOPIC);
    SubscriptionContextBuilder subContextBuilder = new SubscriptionContextBuilder("topic1", ClientAcknowledgement.AUTO);
    SubscribedEventManager subscription = session.addSubscription(subContextBuilder.build());
    Assertions.assertNotNull(subscription);
    //
    // Send a message
    //
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData("Hi There".getBytes());
    destinationImpl.storeMessage(messageBuilder.build());
    synchronized (messageList) {
      int counter = 0;
      while (messageList.isEmpty() && counter < 5) {
        counter++;
        messageList.wait(100);
      }
    }
    Assertions.assertFalse(messageList.isEmpty());

    close(session);
    Assertions.assertFalse(hasSessions());
    TimeUnit.SECONDS.sleep(4);
    if(SessionManagerTest.getInstance().hasIdleSessions()){
      for(String sessionName:SessionManagerTest.getInstance().getIdleSessions()){
        SubscriptionController controller = SessionManagerTest.getInstance().getIdleSubscriptions(sessionName);
        System.err.println("Left over controller::"+controller.getSessionId()+" Timeout::"+controller.getTimeout());
        super.closeSubscriptionController(controller);
      }
    }
    Assertions.assertFalse(SessionManagerTest.getInstance().hasIdleSessions());
  }

  @Test
  @DisplayName("Simple session close on duplication tests")
  public void sessionCloseOnDuplicateTest(TestInfo testInfo) throws Exception {
    Session session1 = createSession(testInfo.getTestMethod().get().getName(), 5, 2, false, this);
    Assertions.assertTrue(hasSessions());
    Assertions.assertFalse(session1.isClosed());

    Session session2 = createSession(testInfo.getTestMethod().get().getName(), 5, 2, false, this);
    Assertions.assertTrue(hasSessions());
    Assertions.assertTrue(session1.isClosed());
    Assertions.assertFalse(session2.isClosed());
  }

  @Test
  @DisplayName("Simple session expiry tests")
  public void sessionExpiryTest(TestInfo testInfo) throws Exception {
    Session session = createSession(testInfo.getTestMethod().get().getName(), 5, 2, true, this);

    Assertions.assertTrue(hasSessions());

    SubscriptionContextBuilder subContectBuilder = new SubscriptionContextBuilder("topic1", ClientAcknowledgement.AUTO);
    SubscribedEventManager subscription = session.addSubscription(subContectBuilder.build());
    Assertions.assertNotNull(subscription);

    close(session);
    Assertions.assertFalse(hasSessions());
    Assertions.assertTrue(SessionManagerTest.getInstance().hasIdleSessions());
    SubscriptionController subscriptionController = SessionManagerTest.getInstance().getIdleSubscriptions(testInfo.getTestMethod().get().getName());
    Assertions.assertNotNull(subscriptionController);
    Assertions.assertEquals(subscriptionController.getSubscriptions().size(), 1);
    Assertions.assertNotNull(subscriptionController.getSubscriptions().get("topic1"));
    TimeUnit.SECONDS.sleep(3);
    Assertions.assertFalse(SessionManagerTest.getInstance().hasIdleSessions());
  }

  @Test
  @DisplayName("Simple session keep alive state")
  public void sessionKeepAlive(TestInfo testInfo) throws Exception {
    receivedKeepAlive.set(0);
    Session session = createSession(testInfo.getTestMethod().get().getName(), 5, 1, true, this);
    Assertions.assertTrue(hasSessions());
    int count = 0;
    while(receivedKeepAlive.get() == 0 && count < 600){
      delay(10);
      count++;
    }
    Assertions.assertTrue(receivedKeepAlive.get() != 0);
    receivedKeepAlive.set(0);
    close(session);
    Assertions.assertFalse(hasSessions());
    Assertions.assertTrue(SessionManagerTest.getInstance().hasIdleSessions());
    TimeUnit.SECONDS.sleep(6);
    Assertions.assertEquals(receivedKeepAlive.get(), 0);
    Assertions.assertFalse(SessionManagerTest.getInstance().hasIdleSessions());
  }

  @Test
  @DisplayName("Simple session subscriptions")
  public void idleSessionSubscriptions(TestInfo testInfo) throws Exception {
    receivedKeepAlive.set(0);
    Session session = createSession(testInfo.getTestMethod().get().getName(), 5, 10000, true, this);
    session.resumeState();

    Destination destination = session.findDestination("topic1", DestinationType.TOPIC);
    Assertions.assertNotNull(session.deleteDestination(destination));

    destination = session.findDestination("topic1", DestinationType.TOPIC);
    SubscriptionContextBuilder subContextBuilder = new SubscriptionContextBuilder("topic1", ClientAcknowledgement.AUTO);
    SubscribedEventManager subscription = session.addSubscription(subContextBuilder.build());
    Assertions.assertNotNull(subscription);

    Assertions.assertTrue(hasSessions());

    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData("Hi There".getBytes());
    messageBuilder.storeOffline(true);
    messageBuilder.setQoS(QualityOfService.AT_MOST_ONCE);
    destination.storeMessage(messageBuilder.build());

    int count = 0;
    while(messageList.isEmpty() && count < 100){
      delay(1);
      count++;
    }
    Assertions.assertFalse(messageList.isEmpty());
    messageList.clear();
    close(session);

    messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData("Hi There".getBytes());
    messageBuilder.storeOffline(true);
    messageBuilder.storeOffline(true);
    messageBuilder.setQoS(QualityOfService.AT_MOST_ONCE);
    destination.storeMessage(messageBuilder.build());
    count = 0;
    while(messageList.isEmpty() && count < 100){
      delay(1);
      count++;
    }
    Assertions.assertTrue(messageList.isEmpty());

    session = createSession(testInfo.getTestMethod().get().getName(), 5, 1, true, this);
    session.resumeState();
    count = 0;
    while(messageList.isEmpty() && count < 100){
      delay(1);
      count++;
    }
    Assertions.assertFalse(messageList.isEmpty());

    close(session);
    Assertions.assertFalse(hasSessions());
    Assertions.assertTrue(SessionManagerTest.getInstance().hasIdleSessions());
    while(SessionManagerTest.getInstance().hasIdleSessions()){
      delay(1);
    }

  }

  @Override
  public void sendMessage(@NotNull Destination destination, @NotNull String normalisedName, @NotNull SubscribedEventManager subscription, @NotNull Message message, @NotNull Runnable completionTask) {
    MessageDetails md = new MessageDetails(destination, subscription, message);
    messageList.add(md);
    completionTask.run();
  }

  @Override
  public void sendKeepAlive() {
    receivedKeepAlive.incrementAndGet();
  }

  private final static class MessageDetails{
    Message message;
    Destination destination;
    SubscribedEventManager subscription;

    public MessageDetails(Destination destination, SubscribedEventManager subscription, Message message) {
      this.destination = destination;
      this.message = message;
      this.subscription = subscription;
    }

  }
}
