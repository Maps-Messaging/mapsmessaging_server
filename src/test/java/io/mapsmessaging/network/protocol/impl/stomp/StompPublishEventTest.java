/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.stomp;

import net.ser1.stomp.Client;
import net.ser1.stomp.Listener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.StompMessages;
import org.projectodd.stilts.stomp.Subscription.AckMode;
import org.projectodd.stilts.stomp.client.ClientSubscription;
import org.projectodd.stilts.stomp.client.ClientTransaction;
import org.projectodd.stilts.stomp.client.StompClient;

import javax.net.ssl.SSLException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class StompPublishEventTest extends StompBaseTest implements Listener {

  private final AtomicBoolean end = new AtomicBoolean(false);

  @ParameterizedTest
  @MethodSource("testParameters")
  @DisplayName("Test with a STOMP client that passes content length with large content")
  void testPublishLargeEventsContentLength(String protocol, boolean auth) throws IOException, LoginException, InterruptedException {
    mainTest(protocol, auth, 10240);
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  @DisplayName("Test with a STOMP client that passes content length with small content")
  void testClientLength(String protocol, boolean auth) throws IOException, LoginException, InterruptedException {
    mainTest(protocol, auth, 10);
  }

  void mainTest(String protocol, boolean auth, int length) throws IOException, LoginException, InterruptedException {
    Client client = getClient(protocol, auth);
    Assertions.assertTrue(client.isConnected());
    String topicName = getTopicName();
    Map<String, String> map = new HashMap<>();
    map.put("id", "subscribe1");
    client.subscribeW(topicName, this, map);

    byte[] buffer = new byte[length];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x % 90) + 33);
    }
    String tmp = new String(buffer);
    for (int x = 0; x < 10; x++) {
      Map<String, String> pubMap = new HashMap<>();
      pubMap.put("packet_id", "" + x);
      client.sendW(topicName, tmp, pubMap);
    }
    Map<String, String> pubMap = new HashMap<>();
    pubMap.put("packet_id", "END");
    client.sendW(topicName, tmp, pubMap);
    long timeout = System.currentTimeMillis() + 20000;
    while (!end.get() && timeout > System.currentTimeMillis()) {
      delay(1);
    }
    Assertions.assertTrue(timeout > System.currentTimeMillis());

    Assertions.assertTrue(client.isConnected());
    client.unsubscribeW(topicName);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
  }

  @Test
  @Disabled("Old log implementation in the client")
  @DisplayName("Test Transactional publishing with content length")
  void testTransactionalPublishContentLength() throws URISyntaxException, StompException, InterruptedException, SSLException, TimeoutException {
    StompClient client = new StompClient("stomp://127.0.0.1/");
    client.connect(10000);
    String topicName = getTopicName();

    Assertions.assertTrue(client.isConnected());
    AtomicInteger counter = new AtomicInteger(0);
    ClientSubscription subscription = client.subscribe(topicName)
        .withAckMode(AckMode.CLIENT_INDIVIDUAL)
        .withMessageHandler(stompMessage -> {
          counter.incrementAndGet();
          try {
            stompMessage.ack();
          } catch (StompException e) {
            e.printStackTrace();
          }
        })
        .start();
    byte[] buffer = new byte[10];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x + 32) % 110);
    }
    String tmp = new String(buffer);
    ClientTransaction transaction = client.begin();
    for (int x = 0; x < 10; x++) {
      StompMessage msg = StompMessages.createStompMessage(topicName, tmp);
      transaction.send(msg);
    }
    transaction.commit();
    long waitTime = System.currentTimeMillis()+10000;
    int start = counter.get();
    while(waitTime > System.currentTimeMillis() && counter.get() != 100){
      delay(10);
      if(start != counter.get()){
        waitTime = System.currentTimeMillis()+10000;
        start = counter.get();
      }
    }

    waitTime = System.currentTimeMillis()+10000;
    start = counter.get();
    while(waitTime > System.currentTimeMillis() && counter.get() != 100){
      delay(10);
      if(start != counter.get()){
        waitTime = System.currentTimeMillis()+10000;
        start = counter.get();
      }
    }

    transaction = client.begin();
    for (int x = 0; x < 100; x++) {
      StompMessage msg = StompMessages.createStompMessage(topicName, tmp);
      transaction.send(msg);
    }
    transaction.abort();
    Assertions.assertTrue(client.isConnected());


    subscription.unsubscribe();
    Assertions.assertEquals(100, counter.get());
    Assertions.assertTrue(client.isConnected());
    client.disconnect(1000);
    Assertions.assertFalse(client.isConnected());
  }


  @ParameterizedTest
  @MethodSource("testParameters")
  @DisplayName("Test Transactional delay publishing")
  void testTransactionalDelayPublish(String protocol, boolean auth) throws IOException, LoginException, InterruptedException {
    Client client = getClient(protocol, auth);
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map = new HashMap<>();
    String topicName = getTopicName();

    map.put("id","subscribe1");
    client.subscribe(topicName, map);
    byte[] buffer = new byte[10];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x % 90)+32);
    }
    String tmp = new String(buffer);
    map = new HashMap<>();
    map.put("transaction","transactionId1");
    map.put("delay", "2000");
    client.begin(map);
    for (int x = 0; x < 1000; x++) {
      client.send(topicName, tmp, map);
    }
    client.commit(map);

    map.put("transaction","transactionId2");
    map.put("delay", "2000");
    client.begin(map);
    for (int x = 0; x < 1000; x++) {
      client.send(topicName, tmp, map);
    }
    client.abort(map);

    Map<String, String> map1 = new HashMap<>();
    map1.put("packet_id","END");
    client.sendW(topicName, tmp, map1);
    Assertions.assertTrue(client.isConnected());
    client.unsubscribe(topicName, map);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
  }

  @Override
  public void message(Map map, String s) {
    if (map.get("packet_id").equals("END")) {
      System.err.println("End Received");
      end.set(true);
    }
  }
}
