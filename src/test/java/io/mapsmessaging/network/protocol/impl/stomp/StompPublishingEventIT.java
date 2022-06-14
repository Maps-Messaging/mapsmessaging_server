/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.stomp;

import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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

class StompPublishingEventIT extends StompBaseTest {

  @Test
  @Disabled("Old log implementation in the client")
  @DisplayName("Test with a STOMP client that doesn't pass content length with small content")
  void testPublishPerformanceSmallEventsContentLength() throws URISyntaxException, StompException, InterruptedException, IOException, TimeoutException {
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
    WaitForState.waitFor(20, TimeUnit.SECONDS, () -> counter.get() == messageCount);
    delay(100);
    Assertions.assertTrue(client.isConnected());
    subscription.unsubscribe();
    Assertions.assertTrue(client.isConnected());
    client.disconnect(1000);
    Assertions.assertFalse(client.isConnected());
    System.err.println("Took "+(System.currentTimeMillis() - startTime)+"ms to send&receive "+messageCount);
  }
}