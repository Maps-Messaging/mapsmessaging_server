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

package org.maps.network.protocol.impl.stomp;

import net.ser1.stomp.Client;
import net.ser1.stomp.Listener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.projectodd.stilts.stomp.Headers;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.StompMessages;
import org.projectodd.stilts.stomp.Subscription.AckMode;
import org.projectodd.stilts.stomp.client.ClientSubscription;
import org.projectodd.stilts.stomp.client.ClientTransaction;
import org.projectodd.stilts.stomp.client.StompClient;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class StompPublishEventTest extends StompBaseTest {

  @Test
  @DisplayName("Test with a STOMP client that passes content length with large content")
  void testPublishLargeEventsContentLength() throws URISyntaxException, StompException, InterruptedException {
    StompClient client = new StompClient("stomp://127.0.0.1/");
    client.connect(10000);
    Assertions.assertTrue(client.isConnected());
    String topicName = getTopicName();
    AtomicInteger counter = new AtomicInteger(0);
    ClientSubscription subscription = client
        .subscribe(topicName)
        .withAckMode(AckMode.AUTO)
        .withMessageHandler(stompMessage -> counter.incrementAndGet())
        .start();
    byte[] buffer = new byte[10240];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x % 90)+32);
    }
    String tmp = new String(buffer);
    for (int x = 0; x < 5; x++) {
      StompMessage msg = StompMessages.createStompMessage(topicName, tmp);
      client.send(msg);
    }
    long timer = System.currentTimeMillis() + 20000;
    while(counter.get() != 5 & timer > System.currentTimeMillis()){
      delay(10);
    }
    subscription.unsubscribe();
    Assertions.assertTrue(client.isConnected());
    client.disconnect(1000);
    Assertions.assertFalse(client.isConnected());
  }

  @Test
  @DisplayName("Test with a STOMP client that passes content length with small content")
  void testPublishSmallEventsContentLength() throws URISyntaxException, StompException, InterruptedException {
    StompClient client = new StompClient("stomp://127.0.0.1/");
    client.connect(10000);
    Assertions.assertTrue(client.isConnected());
    String topicName = getTopicName();
    AtomicBoolean end = new AtomicBoolean(false);
    ClientSubscription subscription = client.subscribe(topicName).
        withAckMode(AckMode.AUTO).
        withMessageHandler(stompMessage -> {
          Headers map = stompMessage.getHeaders();
          if(map.get("packet_id").equals("END")){
            System.err.println("End Received");
            end.set(true);
          }
        }).
        start();
    byte[] buffer = new byte[10];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x + 33) % 100);
    }
    String tmp = new String(buffer);
    for (int x = 0; x < 1000; x++) {
      StompMessage msg = StompMessages.createStompMessage(topicName, tmp);
      msg.getHeaders().put("packet_id", "" + x);
      client.send(msg);
    }
    StompMessage msg = StompMessages.createStompMessage(topicName, tmp);
    msg.getHeaders().put("packet_id", "END");
    client.send(msg);
    long timeout = System.currentTimeMillis()+20000;
    while(!end.get() && timeout > System.currentTimeMillis()){
      delay(1);
    }
    Assertions.assertTrue(timeout> System.currentTimeMillis());

    Assertions.assertTrue(client.isConnected());
    subscription.unsubscribe();
    Assertions.assertTrue(client.isConnected());
    client.disconnect(1000);
    Assertions.assertFalse(client.isConnected());
  }

  @Test
  @DisplayName("Test Transactional publishing with content length")
  void testTransactionalPublishContentLength() throws URISyntaxException, StompException, InterruptedException {
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
    for (int x = 0; x < 100; x++) {
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

  @Test
  @DisplayName("Test publishing with large content length")
  void testPublishLargeEvents() throws IOException, LoginException, InterruptedException {
    Client client = new Client("127.0.0.1", 8675, null, null);
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map = new HashMap<>();
    map.put("id","subscribe1");
    String topicName = getTopicName();

    AtomicBoolean end = new AtomicBoolean(false);
    Listener listener = (map12, s) -> {
      if(map12.containsKey("packet_id")){
        if(map12.get("packet_id").equals("END")){
          end.set(true);
        }
      }
    };
    client.subscribe(topicName, listener,  map);
    byte[] buffer = new byte[10240];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x % 90)+32);
    }
    String tmp = new String(buffer);
    for (int x = 0; x < 10; x++) {
      StompMessage msg = StompMessages.createStompMessage(topicName, tmp);
      Map<String, String> map1 = new HashMap<>();
      map1.put("packet_id",""+x);
      msg.getHeaders().put("packet_id", "" + x);
      client.send(topicName, tmp, map1);
    }
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map1 = new HashMap<>();
    map1.put("packet_id","END");
    client.send(topicName, tmp, map1);
    long timeout = System.currentTimeMillis()+60000;
    while(!end.get() && timeout > System.currentTimeMillis()){
      delay(1);
    }
    Assertions.assertTrue(timeout> System.currentTimeMillis());
    Assertions.assertTrue(client.isConnected());
    client.unsubscribe(topicName,map);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
  }

  @Test
  @DisplayName("Test with small content length")
  void testPublishSmallEvents() throws IOException, LoginException, InterruptedException {
    Client client = new Client("127.0.0.1", 8675, null, null);
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
    for (int x = 0; x < 1000; x++) {
      StompMessage msg = StompMessages.createStompMessage(topicName, tmp);
      Map<String, String> map1 = new HashMap<>();
      map1.put("packet_id",""+x);
      msg.getHeaders().put("packet_id", "" + x);
      client.send(topicName, tmp, map1);
    }
    Assertions.assertTrue(client.isConnected());
    Map<String, String> map1 = new HashMap<>();
    map1.put("packet_id","END");
    TimeUnit.MILLISECONDS.sleep(10);
    client.sendW(topicName, tmp, map1);
    client.unsubscribe(topicName,map);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
  }

  @Test
  @DisplayName("Test Transactional publishing")
  void testTransactionalPublish() throws StompException, IOException, LoginException, InterruptedException {
    Client client = new Client("127.0.0.1", 8675, null, null);
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
    client.begin(map);
    for (int x = 0; x < 1000; x++) {
      client.send(topicName, tmp, map);
    }
    client.commit(map);

    map.put("transaction","transactionId2");
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

  @Test
  @DisplayName("Test Transactional delay publishing")
  void testTransactionalDelayPublish() throws StompException, IOException, LoginException, InterruptedException {
    Client client = new Client("127.0.0.1", 8675, null, null);
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

  @Test
  @DisplayName("WebSocket Stomp Publish Test")
  void webSocketPublishTest() throws InterruptedException, ExecutionException, TimeoutException {
    WebSocketClient client = new StandardWebSocketClient();
    WebSocketStompClient webSocketStompClient = new WebSocketStompClient(client);
    StompSessionHandlerImpl handler = new StompSessionHandlerImpl();
    ListenableFuture<StompSession> futureSession = webSocketStompClient.connect("ws://localhost:8675", handler );

    StompSession stompSession = futureSession.get(5000, TimeUnit.MILLISECONDS);
    Assertions.assertNotNull(stompSession);

    StompSession.Subscription subscription = stompSession.subscribe("/topic/test", handler);
    byte[] tmpBuffer = new byte[10];
    Arrays.fill(tmpBuffer, (byte) 0b01010101);
    for(int x=0;x<10;x++) {
      stompSession.send("/topic/test", tmpBuffer);
    }
    Assertions.assertTrue(handler.subscriptionCount.await(5, TimeUnit.SECONDS));
    Assertions.assertEquals( 0, handler.subscriptionCount.getCount());

    handler.subscriptionCount = new CountDownLatch(10);
    Arrays.fill(tmpBuffer, (byte) 0);
    for(int x=0;x<10;x++) {
      stompSession.send("/topic/test", tmpBuffer);
    }
    Assertions.assertTrue(handler.subscriptionCount.await(5, TimeUnit.SECONDS));
    Assertions.assertEquals( 0, handler.subscriptionCount.getCount());

    subscription.unsubscribe();
    delay(100);
    stompSession.disconnect();
    delay(100);
  }

  private static final class StompSessionHandlerImpl implements StompSessionHandler {
    CountDownLatch subscriptionCount = new CountDownLatch(10);
    @Override
    public void afterConnected(StompSession stompSession, StompHeaders stompHeaders) {
      System.err.println("After Connected");
    }

    @Override
    public void handleException(StompSession stompSession, StompCommand stompCommand, StompHeaders stompHeaders, byte[] bytes, Throwable throwable) {
      System.err.println("Handle Exception");
    }

    @Override
    public void handleTransportError(StompSession stompSession, Throwable throwable) {
      System.err.println("Handle Transport Error");
    }

    @Override
    public Type getPayloadType(StompHeaders stompHeaders) {
      return null;
    }

    @Override
    public void handleFrame(StompHeaders stompHeaders, Object o) {
      System.err.println("Received frame::"+stompHeaders+" "+o);
      subscriptionCount.countDown();
    }
  }

}
