package io.mapsmessaging.network.protocol.impl.nats.conv;

import io.mapsmessaging.test.BaseTestConfig;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighFanoutOrderingTest extends BaseTestConfig {

  @Test
  void testHighFanoutOrdering() throws Exception {
    final int nConns = 100;
    final int nSubs = 100;
    final int nPubs = 500;
    final AtomicInteger[] counters = new AtomicInteger[nConns];

    String url = "nats://localhost:4222"; // Assume NATS server is running locally

    String subject = "test.inbox." + System.nanoTime();
    CountDownLatch latch = new CountDownLatch(nConns * nSubs);

    for (int i = 0; i < nConns; i++) {
      Connection conn = Nats.connect(new Options.Builder().server(url).build());
      AtomicInteger local = new AtomicInteger();
      counters[i] = local;
      for (int j = 0; j < nSubs; j++) {
        AtomicInteger expected = new AtomicInteger(0);
        Dispatcher d = conn.createDispatcher(msg -> {
          local.incrementAndGet();
          int val = Integer.parseInt(new String(msg.getData()));
          int exp = expected.getAndIncrement();
          if (val == exp && exp + 1 == nPubs) {
            latch.countDown();
          }
        });
        d.subscribe(subject);
      }
    }

    // Publisher connection
    try (Connection pubConn = Nats.connect(new Options.Builder().server(url).build())) {
      for (int i = 0; i < nPubs; i++) {
        pubConn.publish(subject, Integer.toString(i).getBytes());
        pubConn.flush(Duration.ofSeconds(10));
      }
    }
    // Wait until all subs receive messages in order
    boolean completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
    assertEquals(true, completed, "Not all subscriptions received ordered messages");
  }
}
