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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.test.WaitForState;

class FilteredQueueSubscriptionTest extends MessageAPITest implements MessageListener {


  private final AtomicBoolean error = new AtomicBoolean(false);
  private final String[] selector = {"key between 0 and 1", "key between 1 and 2", "key = 0 OR key = 2"};
  private final int[][] expected = {{0,1},{1,2}, {0, 2} };

  private final int EVENT_COUNT = (10000/selector.length)*selector.length;
  private final int SESSION_PER_SELECTOR = 10;

  private final AtomicInteger[] counters = new AtomicInteger[selector.length];


  @Test
  void filteredQueueSubscription(TestInfo testInfo) throws LoginException, IOException {
    for(int x=0;x<counters.length;x++){
      counters[x] = new AtomicInteger(0);
    }
    String name = testInfo.getTestMethod().get().getName();
    Session[] sessions = new Session[selector.length*SESSION_PER_SELECTOR];
    SubscribedEventManager[] subscribedEventManagers = new SubscribedEventManager[sessions.length];
    String destinationName = "queue/selectorTest";


    for (int x = 0; x < sessions.length; x++) {
      MessageListener listener = new SelectorProtocolListener(expected[x % selector.length]);
      SessionContextBuilder scb = new SessionContextBuilder(name + "_"+x, new FakeProtocolImpl(listener));
      scb.setReceiveMaximum(1); // ensure it is low
      scb.setSessionExpiry(2); // 2 seconds, more then enough time
      scb.setKeepAlive(60); // large enough to not worry about
      scb.setPersistentSession(false); // store the details
      sessions[x] = createSession(scb, listener);
      sessions[x].findDestination(destinationName, DestinationType.QUEUE);
      SubscriptionContextBuilder subContextBuilder = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.INDIVIDUAL);
      subContextBuilder.setReceiveMaximum(10);
      subContextBuilder.setQos(QualityOfService.AT_LEAST_ONCE);
      subContextBuilder.setSelector(selector[x % selector.length]);
      subscribedEventManagers[x] = sessions[x].addSubscription(subContextBuilder.build());
      Assertions.assertNotNull(subscribedEventManagers[x]);
    }

    Session publisher = createSession(name, 60, 10, false, this);
    Destination destination = publisher.findDestination(destinationName, DestinationType.TOPIC);
    Assertions.assertNotNull(destination);

    for(int x=0;x<EVENT_COUNT;x++) {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(("Hi There "+x).getBytes());
      messageBuilder.storeOffline(true);
      Map<String, TypedData> map = new LinkedHashMap<>();
      map.put("key", new TypedData(x%3));
      messageBuilder.setDataMap(map);
      messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
      destination.storeMessage(messageBuilder.build());
    }

    Assertions.assertFalse(error.get(), "Received an event outside of the required selector range");

    int total = 0;
    for(AtomicInteger counter:counters){
      WaitForState.waitFor(100, TimeUnit.MILLISECONDS, ()->{return EVENT_COUNT/3 == counter.get();});
      Assertions.assertEquals(EVENT_COUNT/3, counter.get(), "The delivered numbers should be the same");
      total += counter.get();
    }

    for (Session value : sessions) {
      value.removeSubscription(destinationName);
    }
    Assertions.assertEquals(EVENT_COUNT, total, "The number of sent messages should match the number of received");
    for(Session session:sessions){
      close(session);
    }
  }

  @Test
  void multipleFilteredQueueSubscription(TestInfo testInfo) throws LoginException, IOException {
    for(int x=0;x<counters.length;x++){
      counters[x] = new AtomicInteger(0);
    }
    String[] destinationName = new String[10];
    for(int x=0;x<destinationName.length;x++){
      destinationName[x] = "queue/selectorTest"+x;
    }

    String name = testInfo.getTestMethod().get().getName();
    Session[] sessions = new Session[selector.length * SESSION_PER_SELECTOR * destinationName.length];
    SubscribedEventManager[] subscribedEventManagers = new SubscribedEventManager[sessions.length];

    int index =0 ;
    for (String s : destinationName) {
      for (int x = 0; x < selector.length * SESSION_PER_SELECTOR; x++) {
        MessageListener listener = new SelectorProtocolListener(expected[x % selector.length]);
        SessionContextBuilder scb = new SessionContextBuilder(name + "_" + index, new FakeProtocolImpl(listener));
        scb.setReceiveMaximum(1); // ensure it is low
        scb.setSessionExpiry(0); // 2 seconds, more then enough time
        scb.setKeepAlive(60); // large enough to not worry about
        scb.setPersistentSession(false); // store the details
        sessions[index] = createSession(scb, listener);
        sessions[index].findDestination(s, DestinationType.QUEUE);

        SubscriptionContextBuilder subContextBuilder = new SubscriptionContextBuilder(s, ClientAcknowledgement.INDIVIDUAL);
        subContextBuilder.setReceiveMaximum(10);
        subContextBuilder.setQos(QualityOfService.AT_LEAST_ONCE);
        subContextBuilder.setSelector(selector[x % selector.length]);
        subscribedEventManagers[index] = sessions[index].addSubscription(subContextBuilder.build());
        Assertions.assertNotNull(subscribedEventManagers[index]);
        index++;
      }
    }
    Session[] publishers = new Session[destinationName.length];
    Destination[] destinations = new Destination[destinationName.length];
    for(int x=0;x< publishers.length;x++) {
      publishers[x] = createSession(name+"_pub_"+x, 60, 0, false, this);
      destinations[x] = publishers[x].findDestination(destinationName[x], DestinationType.TOPIC);
    }

    for(int x=0;x<EVENT_COUNT;x++) {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(("Hi There "+x).getBytes());
      messageBuilder.storeOffline(true);
      Map<String, TypedData> map = new LinkedHashMap<>();
      map.put("key", new TypedData(x%3));
      messageBuilder.setDataMap(map);
      messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
      for(Destination destination:destinations) {
        destination.storeMessage(messageBuilder.build());
      }
    }

    for(Session session:publishers){
      close(session);
    }
    Assertions.assertFalse(error.get(), "Received an event outside of the required selector range");

    int total = 0;
    for(AtomicInteger counter:counters){
      Assertions.assertEquals(destinations.length * EVENT_COUNT/3, counter.get(), "The delivered numbers should be the same");
      total += counter.get();
    }
    for (int y =0;y<sessions.length;y++) {
      SubscriptionContext context = subscribedEventManagers[y].getContext();
      sessions[y].removeSubscription(context.getAlias());
      close(sessions[y]);
    }
    Assertions.assertEquals(EVENT_COUNT * destinationName.length, total, "The number of sent messages should match the number of received");
  }


  @Override
  public void sendMessage(@NotNull Destination destination, @NotNull String normalisedName, @NotNull SubscribedEventManager subscription, @NotNull Message message, @NotNull Runnable completionTask) {
    throw new Error("This should not be called in this context");
  }

  private class SelectorProtocolListener implements MessageListener {

    private final int[] expected;

    public SelectorProtocolListener(int[] expected){
      this.expected = expected;
    }

    @Override
    public void sendMessage(@NotNull Destination destination, @NotNull String normalisedName, @NotNull SubscribedEventManager subscription,@NotNull Message message,@NotNull Runnable completionTask) {
      int received = (Integer)message.getDataMap().get("key").getData();
      boolean found = false;
      for(int expect:expected){
        if(received == expect){
          subscription.ackReceived(message.getIdentifier());
          completionTask.run();
          counters[received].incrementAndGet();
          found = true;
          break;
        }
      }
      if(!found){
        error.getAndSet(true);
        System.err.println("Received an unexpected event");
      }
    }
  }
}
