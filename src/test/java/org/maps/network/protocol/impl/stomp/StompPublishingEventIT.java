package org.maps.network.protocol.impl.stomp;

import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.StompMessages;
import org.projectodd.stilts.stomp.Subscription.AckMode;
import org.projectodd.stilts.stomp.client.ClientSubscription;
import org.projectodd.stilts.stomp.client.StompClient;

public class StompPublishingEventIT extends StompBaseTest {

  @Test
  @DisplayName("Test with a STOMP client that doesn't pass content length with small content")
  public void testPublishPerformanceSmallEventsContentLength() throws URISyntaxException, StompException, InterruptedException {
    StompClient client = new StompClient("stomp://127.0.0.1/");
    client.connect(10000);
    Assertions.assertTrue(client.isConnected());
    String topicName = getTopicName();
    AtomicInteger counter = new AtomicInteger(0);
    ClientSubscription subscription = client.subscribe(topicName)
        .withAckMode(AckMode.AUTO)
        .withMessageHandler(stompMessage -> counter.incrementAndGet())
        .start();
    byte[] buffer = new byte[10];
    for (int x = 0; x < buffer.length; x++) {
      buffer[x] = (byte) ((x + 32) % 110);
    }
    int messageCount = 10000;
    String tmp = new String(buffer);
    for (int x = 0; x < messageCount; x++) {
      StompMessage msg = StompMessages.createStompMessage(topicName, tmp);
      msg.getHeaders().put("packet_id", "" + x);
      client.send(msg);
    }
    long startTime = System.currentTimeMillis();
    long waitTime = System.currentTimeMillis()+20000;
    while(waitTime > System.currentTimeMillis() && counter.get() != messageCount){
      delay(1);
    }
    delay(100);
    Assertions.assertTrue(client.isConnected());
    subscription.unsubscribe();
    Assertions.assertTrue(client.isConnected());
    client.disconnect(1000);
    Assertions.assertFalse(client.isConnected());
    System.err.println("Took "+(System.currentTimeMillis() - startTime)+"ms to send&receive "+messageCount);
  }
}