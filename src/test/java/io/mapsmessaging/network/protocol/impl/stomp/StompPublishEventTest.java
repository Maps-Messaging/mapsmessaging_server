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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.security.auth.login.LoginException;
import net.ser1.stomp.Client;
import net.ser1.stomp.Listener;
import org.apache.activemq.transport.stomp.StompConnection;
import org.apache.activemq.transport.stomp.StompFrame;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
  @DisplayName("Test Transactional publishing with content length")
  void testTransactionalPublishContentLength() throws Exception {
    StompConnection stompConnection = new StompConnection();
    stompConnection.open("127.0.0.1", 8674 );
    stompConnection.connect("admin", getPassword("admin"), "client1");
    String topicName = getTopicName();
    StompSubscriber stompSubscriber = new StompSubscriber(topicName, "127.0.0.1", 8674, "admin");

    byte[] buffer = new byte[10];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x + 32) % 110);
    }
    String tmp = new String(buffer);
    stompConnection.begin("tx1");
    for (int x = 0; x < 10; x++) {
      stompConnection.send(topicName, tmp, "tx1", new HashMap<>());
    }
    stompConnection.commit("tx1");


    long waitTime = System.currentTimeMillis()+10000;
    int start = stompSubscriber.counter.get();
    while(waitTime > System.currentTimeMillis() && stompSubscriber.counter.get() != 100){
      delay(10);
      if(start != stompSubscriber.counter.get()){
        waitTime = System.currentTimeMillis()+10000;
        start = stompSubscriber.counter.get();
      }
    }
    Assertions.assertEquals(10, stompSubscriber.counter.get());

    stompConnection.begin("tx2");
    for (int x = 0; x < 100; x++) {
      stompConnection.send(topicName, tmp, "tx2", new HashMap<>());
    }
    stompConnection.abort("tx2");
      try {
          stompConnection.receive(1000);
          Assertions.fail("No events expected");
      } catch (SocketTimeoutException e) {
        //excpected
      }

    stompSubscriber.unsubscribe();
    stompSubscriber.close();
    stompConnection.disconnect();
    Assertions.assertEquals(10, stompSubscriber.counter.get());

  }
  

  @Override
  public void message(Map map, String s) {
    if (map.get("packet_id").equals("END")) {
      System.err.println("End Received");
      end.set(true);
    }
  }

  private class StompSubscriber implements Runnable{

    private final StompConnection stompConnection;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicBoolean end = new AtomicBoolean(false);
    private final String topicName;
    private final HashMap<String, String> headers;

    public StompSubscriber(String topicName, String hostname, int port, String username) throws Exception {
      this.topicName = topicName;
      stompConnection = new StompConnection();
      stompConnection.open(hostname, port );
      stompConnection.connect(username, getPassword(username), "client-subscribe");
      headers = new HashMap<>();
      headers.put("id", topicName);
      stompConnection.subscribe(topicName, "client-individual", headers);
      Thread t = new Thread(this);
      t.start();
    }

    public void close() throws Exception {
      end.set(true);
      stompConnection.disconnect();
    }

    public void unsubscribe() throws Exception {
      stompConnection.unsubscribe(topicName, headers);
    }

    @Override
    public void run() {
      while (!end.get()) {
        try {
          try {
            StompFrame stompFrame = stompConnection.receive(1000);
            if (stompFrame.isMessage()) {
                stompConnection.ack(stompFrame);
            }
            counter.incrementAndGet();
          } catch (SocketTimeoutException e) {
            System.err.println("SocketTimeoutException");
            // ignore
          }
        } catch (Exception e) {
          e.printStackTrace();
          end.set(true);
        }
      }
    }
  }
}
