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

package io.mapsmessaging.api;

import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.ProtocolMessageListener;
import io.mapsmessaging.engine.session.SessionManagerTest;
import io.mapsmessaging.test.WaitForState;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class SessionTest extends MessageAPITest implements ProtocolMessageListener {


  private final AtomicInteger receivedKeepAlive  = new AtomicInteger(0);
  private final Queue<MessageDetails> messageList = new ConcurrentLinkedQueue<>();


  @Test
  @DisplayName("Simple session construction tests")
  public void sessionConstructionTest(TestInfo testInfo) throws Exception {
    Session session = createSession(testInfo.getTestMethod().get().getName(), 5, 2, false, this);
    Assertions.assertTrue(SessionManagerTest.getInstance().hasSessions());
    Destination destinationImpl = session.findDestination("topic1", DestinationType.TOPIC).get();
    Assertions.assertNotNull(destinationImpl);
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
  void sessionCloseOnDuplicateTest(TestInfo testInfo) throws Exception {
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
  void sessionExpiryTest(TestInfo testInfo) throws Exception {
    Session session = createSession(testInfo.getTestMethod().get().getName(), 5, 2, true, this, true);
    close(session);
    session = createSession(testInfo.getTestMethod().get().getName(), 5, 2, true, this);
    Assertions.assertTrue(hasSessions());

    SubscriptionContextBuilder subContectBuilder = new SubscriptionContextBuilder("topic1", ClientAcknowledgement.AUTO);
    SubscribedEventManager subscription = session.addSubscription(subContectBuilder.build());
    Assertions.assertNotNull(subscription);

    close(session);
    Assertions.assertFalse(hasSessions());
    int size = SessionManagerTest.getInstance().getIdleSessions().size();
    Assertions.assertTrue(size>0);
    SubscriptionController subscriptionController = SessionManagerTest.getInstance().getIdleSubscriptions(testInfo.getTestMethod().get().getName());
    Assertions.assertNotNull(subscriptionController);
    Map<String, SubscriptionContext> map = subscriptionController.getSubscriptions();
    Assertions.assertEquals(map.size(), 1);
    SubscriptionContext context = map.get("$normaltopic1");
    Assertions.assertNotNull(context);
    TimeUnit.SECONDS.sleep(3);
    Assertions.assertTrue(SessionManagerTest.getInstance().getIdleSessions().size() < size);
  }

  @Test
  @DisplayName("Simple session keep alive state")
  void sessionKeepAlive(TestInfo testInfo) throws Exception {
    receivedKeepAlive.set(0);
    Session session = createSession(testInfo.getTestMethod().get().getName(), 5, 12, true, this);
    Assertions.assertTrue(hasSessions());
    WaitForState.waitFor(10, TimeUnit.SECONDS, () -> receivedKeepAlive.get() != 0);
    Assertions.assertTrue(receivedKeepAlive.get() != 0);
    receivedKeepAlive.set(0);
    close(session);
    Assertions.assertFalse(hasSessions());
    Assertions.assertTrue(SessionManagerTest.getInstance().hasIdleSessions());
    WaitForState.waitFor(12, TimeUnit.SECONDS, () -> receivedKeepAlive.get() != 0);
    Assertions.assertEquals(receivedKeepAlive.get(), 0);
    Assertions.assertFalse(SessionManagerTest.getInstance().hasIdleSessions());
  }

  @Test
  @DisplayName("Simple session subscriptions")
  public void idleSessionSubscriptions(TestInfo testInfo) throws Exception {
    receivedKeepAlive.set(0);
    Session session = createSession(testInfo.getTestMethod().get().getName(), 5, 10000, true, this);
    session.resumeState();

    Destination destination = session.findDestination("topic1", DestinationType.TOPIC).get();
    Assertions.assertNotNull(destination);
    Assertions.assertNotNull(session.deleteDestination(destination));

    destination = session.findDestination("topic1", DestinationType.TOPIC).get();
    Assertions.assertNotNull(destination);

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
    count = 0;
    while(SessionManagerTest.getInstance().hasIdleSessions() && count < 110){
      delay(100);
      count++;
    }
    Assertions.assertFalse(SessionManagerTest.getInstance().hasIdleSessions());
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    MessageDetails md = new MessageDetails(messageEvent.getSubscription(), messageEvent.getMessage());
    messageList.add(md);
    messageEvent.getCompletionTask().run();
  }

  @Override
  public void sendKeepAlive() {
    receivedKeepAlive.incrementAndGet();
  }

  private final static class MessageDetails{
    Message message;
    SubscribedEventManager subscription;

    public MessageDetails(SubscribedEventManager subscription, Message message) {
      this.message = message;
      this.subscription = subscription;
    }

  }
}
