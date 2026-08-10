/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.nats.conv;

import io.mapsmessaging.test.BaseTestConfig;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighFanoutOrderingTest extends BaseTestConfig {

  @Test
  void testHighFanoutOrdering() throws Exception {
    final int nConns = 10;
    final int nSubs = 100;
    final int nPubs = 500;

    final AtomicInteger[] counters = new AtomicInteger[nConns];

    String url = "nats://localhost:4222";
    String subject = "test.inbox." + System.nanoTime();
    CountDownLatch latch = new CountDownLatch(nConns * nSubs);

    List<Connection> connections = new ArrayList<>(nConns);
    List<Dispatcher> dispatchers = new ArrayList<>(nConns);

    Connection publisherConnection = null;

    try {
      for (int i = 0; i < nConns; i++) {
        Connection conn = Nats.connect(new Options.Builder().server(url).build());
        connections.add(conn);

        AtomicInteger local = new AtomicInteger();
        counters[i] = local;

        Dispatcher dispatcher = conn.createDispatcher(msg -> local.incrementAndGet());
        dispatchers.add(dispatcher);

        for (int j = 0; j < nSubs; j++) {
          AtomicInteger expected = new AtomicInteger(0);

          dispatcher.subscribe(subject, msg -> {
            int val = Integer.parseInt(new String(msg.getData()));
            int exp = expected.getAndIncrement();
            if (val == exp && exp + 1 == nPubs) {
              latch.countDown();
            }
          });
        }
      }

      publisherConnection = Nats.connect(new Options.Builder().server(url).build());
      for (int i = 0; i < nPubs; i++) {
        publisherConnection.publish(subject, Integer.toString(i).getBytes());
      }
      publisherConnection.flush(Duration.ofSeconds(30));

      boolean completed = latch.await(30, TimeUnit.SECONDS);
      assertEquals(true, completed, "Not all subscriptions received ordered messages");
    }
    finally {
      if (publisherConnection != null) {
        publisherConnection.close();
      }

      for (int i = 0; i < connections.size(); i++) {
        Connection conn = connections.get(i);
        Dispatcher dispatcher = dispatchers.get(i);

        if (dispatcher != null) {
          conn.closeDispatcher(dispatcher);
        }
        conn.close();
      }
    }
  }
}
