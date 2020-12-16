/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.stomp;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.projectodd.stilts.stomp.Headers;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.StompMessages;
import org.projectodd.stilts.stomp.Subscription.AckMode;
import org.projectodd.stilts.stomp.client.ClientSubscription;
import org.projectodd.stilts.stomp.client.StompClient;

public class StompQueueTest extends StompBaseTest {

  String getQueue(){
    return "/queue/testQueue1";
  }

  @Test
  public void testBasicQueueLogic() throws URISyntaxException, StompException, InterruptedException {
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
    String tmp = new String(buffer);
    for(int x=0;x<clients.size()*10;x++){ // Each client should consume 10 events
      StompMessage msg = StompMessages.createStompMessage(queueName, tmp);
      Headers header = msg.getHeaders();
      header.put("message_id", "testBasicQueueLogic "+x);
      client.send(msg);
    }

    boolean completed = false;
    long endTime = System.currentTimeMillis() + 10000;
    while(System.currentTimeMillis() < endTime && !completed){
      completed = true;
      for(StompQueueClient queueClient:clients){
        if(queueClient.counter.sum() < 10){ // Ensures even distribution across all clients
          completed = false;
          break;
        }
      }
      delay(100);
    }
    int total = 0;
    for(StompQueueClient queueClient:clients){
      queueClient.close();
      total += queueClient.counter.sum();
    }

    if(total == 0){
      System.err.println("Error!!!");
    }
    client.disconnect(10);
    Assertions.assertEquals(10*clients.size(), total);
    Assertions.assertTrue(completed);
  }

  @Test
  public void testBasicUnsubscribeQueueLogic() throws URISyntaxException, StompException, InterruptedException {
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
    String tmp = new String(buffer);
    for(int x=0;x<clients.size()*10;x++){ // Each client should consume 10 events
      StompMessage msg = StompMessages.createStompMessage(queueName, tmp);
      Headers header = msg.getHeaders();
      header.put("message_id", "testBasicUnsubscribeQueueLogic <part I>"+x);
      client.send(msg);
    }

    boolean completed = false;
    long endTime = System.currentTimeMillis() + 10000;
    while(System.currentTimeMillis() < endTime && !completed){
      completed = true;
      for(StompQueueClient queueClient:clients){
        if(queueClient.counter.sum() < 10){ // Ensures even distribution across all clients
          completed = false;
          break;
        }
      }
      delay(100);
    }
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
    Assertions.assertTrue(completed);

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
    completed = false;
    endTime = System.currentTimeMillis() + 15000;
    while(System.currentTimeMillis() < endTime && !completed){
      completed = true;
      for(StompQueueClient queueClient:clients){
        if(queueClient.counter.sum() < 10){ // Ensures even distribution across all clients
          completed = false;
          break;
        }
      }
      delay(100);
    }
    total = 0;
    for(StompQueueClient queueClient:clients){
      queueClient.close();
      total += queueClient.counter.sum();
    }
    client.disconnect(100);

    Assertions.assertEquals(10*clients.size(), total);
    Assertions.assertTrue(completed);
  }

  @Test
  public void testBasicSubscribeQueueLogic() throws URISyntaxException, StompException, InterruptedException {
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
    String tmp = new String(buffer);
    for(int x=0;x<clients.size()*10;x++){ // Each client should consume 10 events
      StompMessage msg = StompMessages.createStompMessage(queueName, tmp);
      Headers header = msg.getHeaders();
      header.put("message_id", "testBasicUnsubscribeQueueLogic <part III>"+x);
      client.send(msg);
    }

    boolean completed = false;
    long endTime = System.currentTimeMillis() + 10000;
    while(System.currentTimeMillis() < endTime && !completed){
      completed = true;
      for(StompQueueClient queueClient:clients){
        if(queueClient.counter.sum() < 10){ // Ensures even distribution across all clients
          completed = false;
          break;
        }
      }
      delay(100);
    }
    int total = 0;
    for(StompQueueClient queueClient:clients){
      total += queueClient.counter.sum();
    }

    Assertions.assertEquals(10*clients.size(), total);
    Assertions.assertTrue(completed);

    for(StompQueueClient queueClient:clients) {
      queueClient.counter.reset();
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
    completed = false;
    endTime = System.currentTimeMillis() + 10000;
    while(System.currentTimeMillis() < endTime && !completed){
      completed = true;
      for(StompQueueClient queueClient:clients){
        if(queueClient.counter.sum() < 10){ // Ensures even distribution across all clients
          completed = false;
          break;
        }
      }
      delay(100);
    }
    total = 0;
    for(StompQueueClient queueClient:clients){
      queueClient.close();
      total += queueClient.counter.sum();
    }
    client.disconnect(100);
    Assertions.assertEquals(10*clients.size(), total);
    Assertions.assertTrue(completed);
  }

  @Test
  public void testBasicRollingUnsubscribeQueueLogic() throws URISyntaxException, StompException, InterruptedException {
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
    String tmp = new String(buffer);
    for(int x=0;x<clients.size()*10;x++){ // Each client should consume 10 events
      StompMessage msg = StompMessages.createStompMessage(queueName, tmp);
      Headers header = msg.getHeaders();
      header.put("message_id", "testBasicRollingUnsubscribeQueueLogic <part I>"+x);
      client.send(msg);
    }

    boolean completed = false;
    long endTime = System.currentTimeMillis() + 10000;
    while(System.currentTimeMillis() < endTime && !completed){
      completed = true;
      for(StompQueueClient queueClient:clients){
        if(queueClient.counter.sum() < 10){ // Ensures even distribution across all clients
          completed = false;
          break;
        }
      }
      delay(100);
    }
    int total = 0;
    for(StompQueueClient queueClient:clients){
      total += queueClient.counter.sum();
    }

    Assertions.assertEquals(10*clients.size(), total);
    Assertions.assertTrue(completed);

    //
    // Close 1/2 of the clients
    while(clients.size() != 1){
      clients.remove(1).close();
    }
    StompQueueClient remaining = clients.remove(0);

    endTime = System.currentTimeMillis() + 10000;
    while(System.currentTimeMillis() < endTime && (remaining.counter.sum() < 100)){
      delay(100);
    }
    total = (int) remaining.counter.sum();
    remaining.close();
    Assertions.assertEquals(100, total);
    client.disconnect(100);

  }


  static private class StompQueueClient{
    private final StompClient client;
    private final ClientSubscription subscription;
    private final LongAdder counter = new LongAdder();

    public StompQueueClient(String url, String queueName, boolean ack) throws URISyntaxException, StompException, InterruptedException{
      client = new StompClient(url);
      client.connect(10000);
      Assertions.assertTrue(client.isConnected());
      subscription = client.subscribe(queueName)
          .withAckMode(AckMode.CLIENT_INDIVIDUAL)
          .withMessageHandler(stompMessage -> {
            if(ack) {
              try {
                stompMessage.ack();
              } catch (StompException e) {
                e.printStackTrace();
              }
            }
            counter.increment();
          })
          .start();
    }

    public void close() throws StompException, InterruptedException {
      subscription.unsubscribe();
      client.disconnect(1000);
    }
  }
}
