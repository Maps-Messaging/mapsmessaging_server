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
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.engine.session.FakeProtocol;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.security.auth.login.LoginException;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class DelayedPublishTest extends MessageAPITest {
  private static final int EVENT_COUNT = 10;

  @SneakyThrows
  @Test
  void delayedPublishTest(TestInfo testInfo) throws LoginException, IOException {
    String destinationName = "topic/delayTest";
    AtomicLong counter = new AtomicLong(0);
    String name = testInfo.getTestMethod().get().getName();

    CountingMessageReceiver messageReceiver = new CountingMessageReceiver(counter);
    SessionContextBuilder scb1 = new SessionContextBuilder(name+"_1", new ProtocolClientConnection(new FakeProtocol(messageReceiver)));
    scb1.setReceiveMaximum(10); // ensure it is low
    scb1.setSessionExpiry(60); // 60 seconds, more then enough time
    scb1.setPersistentSession(true); // store the details
    Session session = createSession(scb1, messageReceiver);

    // Subscribe to the topic with individual Ack, this causes the server to stop sending events until the client acks them
    SubscriptionContextBuilder subContextBuilder = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.AUTO);
    subContextBuilder.setReceiveMaximum(EVENT_COUNT)
      .setQos(QualityOfService.AT_LEAST_ONCE);

    SubscribedEventManager subscription = session.addSubscription(subContextBuilder.build());
    Assertions.assertNotNull(subscription);

    Session publisher = createSession(name, 60, 60, false, new DropMessageReceiver());
    Destination destination = publisher.findDestination(destinationName, DestinationType.TOPIC).get();
    Assertions.assertNotNull(destination);
    for(int x=0;x<EVENT_COUNT;x++) {
      Map<String, TypedData> map = new LinkedHashMap<>();
      map.put("time", new TypedData(System.currentTimeMillis()));

      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData("Here is an event".getBytes())
        .storeOffline(true)
        .setDataMap(map)
        .setDelayed(2000)
        .setQoS(QualityOfService.AT_LEAST_ONCE);
      destination.storeMessage(messageBuilder.build());
    }
    close(publisher);

    // OK we have 2 subscribers with different selectors, each take 50% of all events so the store should have 100%
    WaitForState.waitFor(200, TimeUnit.MILLISECONDS, () -> destination.getStoredMessages() == EVENT_COUNT);

    Assertions.assertEquals(EVENT_COUNT, destination.getStoredMessages());
    Assertions.assertEquals(0, counter.get());

    WaitForState.waitFor(10, TimeUnit.SECONDS, () -> counter.get() == EVENT_COUNT);
    Assertions.assertEquals(EVENT_COUNT, counter.get());
    close(session);
  }


  private static class CountingMessageReceiver implements MessageListener {

    private final AtomicLong counter;

    private CountingMessageReceiver(AtomicLong counter){
      this.counter = counter;
    }

    @Override
    public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
      counter.incrementAndGet();
      messageEvent.getCompletionTask().run();
    }
  }

  private static class DropMessageReceiver implements MessageListener{

    private DropMessageReceiver(){
    }

    @Override
    public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
      messageEvent.getCompletionTask().run();
      messageEvent.getSubscription().ackReceived(messageEvent.getMessage().getIdentifier());
    }
  }

}
