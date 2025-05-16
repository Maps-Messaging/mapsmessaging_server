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

package io.mapsmessaging.network.protocol.impl.stomp;

import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.StompMessages;
import org.projectodd.stilts.stomp.Subscription.AckMode;
import org.projectodd.stilts.stomp.client.ClientSubscription;
import org.projectodd.stilts.stomp.client.StompClient;

class StompTransactionalSubscriptionImplTest extends StompBaseTest {

  @Test
  @Disabled("Old log implementation in the client")
  @DisplayName("Test publish events with priority using block based transactions")
  void testPriorityPublishTransactionalWithClientCommit() throws InterruptedException, URISyntaxException, StompException, SSLException, TimeoutException {
    StompClient client = new StompClient("stomp://127.0.0.1/");
    client.connect(10000);
    Assertions.assertTrue(client.isConnected());
    byte[] buffer = new byte[128];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x + 32) % 110);
    }
    String topicName = getTopicName();
    String tmp = new String(buffer);
    AtomicInteger counter = new AtomicInteger(0);
    ClientSubscription subscription = client.subscribe(topicName)
        .withAckMode(AckMode.CLIENT)
        .withMessageHandler(stompMessage -> {
          try {
            if(counter.incrementAndGet() % 10 == 0) {
              stompMessage.ack();
            }
          } catch (StompException e) {
            e.printStackTrace();
          }
        })
        .start();
    int msgNumber = 1000;
    for (int x = 0; x < msgNumber; x++) {
      StompMessage msg = StompMessages.createStompMessage(topicName, tmp);
      msg.getHeaders().put("priority", ""+x%10);
      client.send(msg);
    }
    int start = counter.get();
    long endTime = System.currentTimeMillis()+10000;
    while(counter.get()<msgNumber && System.currentTimeMillis()< endTime){
      delay(10);
      if(start != counter.get()){
        start = counter.get();
        endTime = System.currentTimeMillis()+10000;
      }
    }
    subscription.unsubscribe();
    client.disconnect();
    Assertions.assertEquals(msgNumber, counter.get());
  }


  @Test
  @Disabled("Old log implementation in the client")
  @DisplayName("Test publish events with priority using transactions")
  void testPriorityPublishTransactionalWithCommit() throws InterruptedException, URISyntaxException, StompException, IOException, TimeoutException {
    StompClient client = new StompClient("stomp://127.0.0.1/");
    client.connect(10000);
    Assertions.assertTrue(client.isConnected());
    byte[] buffer = new byte[128];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x + 32) % 110);
    }
    String topicName = getTopicName();
    String tmp = new String(buffer);
    AtomicInteger counter = new AtomicInteger(0);
    ClientSubscription subscription = client.subscribe(topicName)
        .withAckMode(AckMode.CLIENT_INDIVIDUAL)
        .withMessageHandler(stompMessage -> {
          try {
            stompMessage.ack();
            counter.incrementAndGet();
          } catch (StompException e) {
            e.printStackTrace();
          }
        })
        .start();
    int msgNumber = 1000;
    for (int x = 0; x < msgNumber; x++) {
      StompMessage msg = StompMessages.createStompMessage(topicName, tmp);
      msg.getHeaders().put("priority", ""+x%10);
      client.send(msg);
    }
    int start = counter.get();
    int loop = 20;
    while(counter.get() < msgNumber && loop > 0){
      WaitForState.waitFor(1, TimeUnit.SECONDS, ()->counter.get() == msgNumber);
      if(start != counter.get()){
        start = counter.get();
      }
      loop--;
    }
    subscription.unsubscribe();
    client.disconnect();
    Assertions.assertEquals(msgNumber, counter.get());
  }
}
