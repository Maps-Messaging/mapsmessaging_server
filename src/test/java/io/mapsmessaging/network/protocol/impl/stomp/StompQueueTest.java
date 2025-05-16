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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.projectodd.stilts.stomp.Headers;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.StompMessages;
import org.projectodd.stilts.stomp.Subscription.AckMode;
import org.projectodd.stilts.stomp.client.ClientSubscription;
import org.projectodd.stilts.stomp.client.StompClient;

class StompQueueTest extends StompBaseTest {
  private static final long MAX_WAIT_SECONDS = 15;

  String getQueue(){
    return "/queue/testQueue1";
  }

  @Test
  @Disabled("Old log implementation in the client")
  void testBasicQueueLogic() throws URISyntaxException, StompException, InterruptedException, IOException, TimeoutException {
    String queueName = getQueue();
    List<StompQueueClient> clients = new ArrayList<>();
    //
    // add clients to the queue
    //
    for(int x=0;x<10;x++){
      clients.add(new StompQueueClient("stomp://127.0.0.1", queueName,true));
    }

    //
    // Now connect a new client, so we can publish to the queue
    StompClient client = new StompClient("stomp://127.0.0.1");
    client.connect(10000);
    Assertions.assertTrue(client.isConnected());
    byte[] buffer = new byte[1024];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x + 32) % 110);
    }
    clients.forEach(StompQueueClient::clear);
    String tmp = new String(buffer);
    for(int x=0;x<clients.size()*10;x++){ // Each client should consume 10 events
      StompMessage msg = StompMessages.createStompMessage(queueName, tmp);
      Headers header = msg.getHeaders();
      header.put("message_id", "testBasicQueueLogic "+x);
      client.send(msg);
    }

    WaitForState.waitFor(MAX_WAIT_SECONDS, TimeUnit.SECONDS, ()->{
      for(StompQueueClient queueClient:clients){
        if(queueClient.counter.sum() < 10){ // Ensures even distribution across all clients
          return false;
        }
      }
      return true;
    });

    long total = 0;
    for(StompQueueClient queueClient:clients){
      total += queueClient.counter.sum();
    }
    for(StompQueueClient queueClient:clients){
      queueClient.close();
    }


    try {
      client.disconnect(10);
    }
    catch(TimeoutException timeout){
      // We can ignore the timeout here
    } catch (StompException e) { // Ignore network disconnect on closure
      if(!e.getMessage().startsWith("Connection timed out")){
        throw e;
      }
    }
    if(10L*clients.size() != total){
      for(StompQueueClient report:clients){
        System.err.println(report.toString());
      }
    }
    Assertions.assertEquals(10L*clients.size(), total);
  }

  @Test
  @Disabled("Old log implementation in the client")
  void testBasicUnsubscribeQueueLogic() throws URISyntaxException, StompException, InterruptedException, IOException, TimeoutException {
    String queueName = getQueue();

    List<StompQueueClient> clients = new ArrayList<>();
    //
    // add clients to the queue
    //
    for(int x=0;x<10;x++){
      clients.add(new StompQueueClient("stomp://127.0.0.1", queueName,true));
    }

    //
    // Now connect a new client so we can publish to the queue
    StompClient client = new StompClient("stomp://127.0.0.1");
    client.connect(10000);
    Assertions.assertTrue(client.isConnected());
    byte[] buffer = new byte[1024];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x + 32) % 110);
    }
    clients.forEach(StompQueueClient::clear);

    String tmp = new String(buffer);
    for(int x=0;x<clients.size()*10;x++){ // Each client should consume 10 events
      StompMessage msg = StompMessages.createStompMessage(queueName, tmp);
      Headers header = msg.getHeaders();
      header.put("message_id", "testBasicUnsubscribeQueueLogic <part I>"+x);
      client.send(msg);
    }

    WaitForState.waitFor(MAX_WAIT_SECONDS, TimeUnit.SECONDS, ()->{
      for(StompQueueClient queueClient:clients){
        if(queueClient.counter.sum() < 10){ // Ensures even distribution across all clients
          return false;
        }
      }
      return true;
    });
    int total = 0;
    for(StompQueueClient queueClient:clients){
      total += queueClient.counter.sum();
    }

    if(10*clients.size() != total){
      for(StompQueueClient queueClient:clients){
        queueClient.close();
      }
      Assertions.assertEquals(10*clients.size(), total);
    }

    //
    // Close 1/2 of the clients
    for(int x=0;x<5;x++){
      clients.remove(0).close();
    }
    for(StompQueueClient queueClient:clients) {
      queueClient.counter.reset();
    }

    for(int x=0;x<clients.size()*10;x++){ // Each client should consume 10 events
      StompMessage msg = StompMessages.createStompMessage(queueName, tmp);
      Headers header = msg.getHeaders();
      header.put("message_id", "testBasicUnsubscribeQueueLogic <part II>"+x);
      client.send(msg);
    }
    WaitForState.waitFor(MAX_WAIT_SECONDS, TimeUnit.SECONDS, ()->{
      for(StompQueueClient queueClient:clients){
        if(queueClient.counter.sum() < 10){ // Ensures even distribution across all clients
          return false;
        }
      }
      return true;
    });

    total = 0;
    for(StompQueueClient queueClient:clients){
      total += queueClient.counter.sum();
    }
    for(StompQueueClient queueClient:clients){
      queueClient.close();
    }
    client.disconnect(100);

    Assertions.assertEquals(10*clients.size(), total);
  }

  @Test
  @Disabled("Old log implementation in the client")
  void testBasicSubscribeQueueLogic() throws URISyntaxException, StompException, InterruptedException, IOException, TimeoutException {
    String queueName = getQueue();
    List<StompQueueClient> clients = new ArrayList<>();
    //
    // add clients to the queue
    //
    for(int x=0;x<10;x++){
      clients.add(new StompQueueClient("stomp://127.0.0.1", queueName,true));
    }

    //
    // Now connect a new client so we can publish to the queue
    StompClient client = new StompClient("stomp://127.0.0.1");
    client.connect(10000);
    Assertions.assertTrue(client.isConnected());
    byte[] buffer = new byte[1024];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x + 32) % 110);
    }
    clients.forEach(StompQueueClient::clear);

    String tmp = new String(buffer);
    for(int x=0;x<clients.size()*10;x++){ // Each client should consume 10 events
      StompMessage msg = StompMessages.createStompMessage(queueName, tmp);
      Headers header = msg.getHeaders();
      header.put("message_id", "testBasicUnsubscribeQueueLogic <part III>"+x);
      client.send(msg);
    }

    WaitForState.waitFor(MAX_WAIT_SECONDS, TimeUnit.SECONDS, ()->{
      for(StompQueueClient queueClient:clients){
        if(queueClient.counter.sum() < 10){ // Ensures even distribution across all clients
          return false;
        }
      }
      return true;
    });

    AtomicLong total = new AtomicLong();
    for(StompQueueClient queueClient:clients){
      total.addAndGet(queueClient.counter.sum());
    }
    if(10L*clients.size() != total.get()){
      for(StompQueueClient report:clients){
        System.err.println(report.toString());
      }
    }

    Assertions.assertEquals(10L*clients.size(), total.get());

    for(StompQueueClient queueClient:clients) {
      queueClient.clear();
    }
    //
    // Add another 10
    for(int x=0;x<10;x++){
      clients.add(new StompQueueClient("stomp://127.0.0.1", queueName,true));
    }

    for(int x=0;x<clients.size()*10;x++){ // Each client should consume 10 events
      StompMessage msg = StompMessages.createStompMessage(queueName, tmp);
      Headers header = msg.getHeaders();
      header.put("message_id", "testBasicUnsubscribeQueueLogic <part II>"+x);
      client.send(msg);
    }

    WaitForState.waitFor(MAX_WAIT_SECONDS, TimeUnit.SECONDS, ()->{
      for(StompQueueClient queueClient:clients){
        if(queueClient.counter.sum() < 10){ // Ensures even distribution across all clients
          return false;
        }
      }
      return true;
    });

    total.set(0);
    clients.forEach(c -> total.addAndGet(c.counter.sum()));
    for (StompQueueClient c : clients) {
        c.close();
    }
    client.disconnect(100);
    if(10L*clients.size() != total.get()){
      for(StompQueueClient report:clients){
        System.err.println(report.toString());
      }
    }

    Assertions.assertEquals(10*clients.size(), total.get());
  }

  @Test
  @Disabled("Old log implementation in the client")
  void testBasicRollingUnsubscribeQueueLogic() throws URISyntaxException, StompException, InterruptedException, IOException, TimeoutException {
    String queueName = getQueue();

    List<StompQueueClient> clients = new ArrayList<>();
    //
    // add clients to the queue
    //
    for(int x=0;x<10;x++){
      clients.add(new StompQueueClient("stomp://127.0.0.1", queueName,x == 0)); // Only 1 client ACKs
    }

    //
    // Now connect a new client so we can publish to the queue
    StompClient client = new StompClient("stomp://127.0.0.1");
    client.connect(10000);
    Assertions.assertTrue(client.isConnected());
    byte[] buffer = new byte[1024];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x + 32) % 110);
    }
    clients.forEach(StompQueueClient::clear);

    String tmp = new String(buffer);
    for(int x=0;x<clients.size()*10;x++){ // Each client should consume 10 events
      StompMessage msg = StompMessages.createStompMessage(queueName, tmp);
      Headers header = msg.getHeaders();
      header.put("message_id", "testBasicRollingUnsubscribeQueueLogic <part I>"+x);
      client.send(msg);
    }

    WaitForState.waitFor(MAX_WAIT_SECONDS, TimeUnit.SECONDS, ()->{
      for(StompQueueClient queueClient:clients){
        if(queueClient.counter.sum() < 10){ // Ensures even distribution across all clients
          return false;
        }
      }
      return true;
    });

    long total = 0;
    for(StompQueueClient queueClient:clients){
      total += queueClient.counter.sum();
    }
    Assertions.assertEquals(10*clients.size(), total);

    //
    // Close 1/2 of the clients
    while(clients.size() != 1){
      clients.remove(1).close();
    }
    StompQueueClient remaining = clients.remove(0);

    WaitForState.waitFor(MAX_WAIT_SECONDS, TimeUnit.SECONDS, () -> remaining.counter.sum() == 100);
    total = (int) remaining.counter.sum();
    remaining.close();
    Assertions.assertEquals(100, total);
    client.disconnect(100);

  }


  static private class StompQueueClient{
    private final StompClient client;
    private final ClientSubscription subscription;
    private final LongAdder counter = new LongAdder();
    private final List<String> receivedList;

    public StompQueueClient(String url, String queueName, boolean ack) throws URISyntaxException, StompException, InterruptedException, SSLException, TimeoutException {
      client = new StompClient(url);
      client.connect(10000);
      receivedList = new ArrayList<>();
      Assertions.assertTrue(client.isConnected());
      subscription = client.subscribe(queueName)
          .withAckMode(AckMode.CLIENT_INDIVIDUAL)
          .withMessageHandler(stompMessage -> {
            if(ack) {
              try {
                String msgId = stompMessage.getHeaders().get("message_id");
                if(msgId != null){
                  receivedList.add(msgId);
                }
                stompMessage.ack();
              } catch (StompException e) {
                e.printStackTrace();
              }
            }
            counter.increment();
          })
          .start();
    }

    public void clear(){
      receivedList.clear();
      counter.reset();
    }
    public String toString(){
      StringBuilder sb = new StringBuilder();
      receivedList.forEach(s -> sb.append(s).append(","));
      return sb.toString();
    }
    public void close() throws StompException, InterruptedException, TimeoutException {
      subscription.unsubscribe();
      client.disconnect(1000);
    }
  }
}
