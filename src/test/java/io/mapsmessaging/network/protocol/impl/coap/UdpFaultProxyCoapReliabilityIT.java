/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.test.network.UdpFaultProxy;
import io.mapsmessaging.test.network.UdpFaultProxy.Direction;
import io.mapsmessaging.test.network.UdpFaultProxy.Stats;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Additional real-CoAP network fault scenarios using the shared UDP proxy. */
class UdpFaultProxyCoapReliabilityIT extends BaseCoapTest {
  private static UdpFaultProxy proxy;

  @BeforeAll
  static void startProxy() throws Exception {
    proxy = new UdpFaultProxy(0, new InetSocketAddress("127.0.0.1", 5683));
  }

  @BeforeEach
  void before() {
    proxy.resetRules();
  }

  @AfterEach
  void after() {
    proxy.resetRules();
  }

  @AfterAll
  static void stopProxy() {
    if (proxy != null) {
      proxy.close();
    }
  }

  private static CoapClient client(String uri) {
    CoapClient client = new CoapClient(uri.replace(":5683/", ":" + proxy.getListenPort() + "/"));
    client.setTimeout(15_000L);
    return client;
  }

  private static final class ManagedClient implements AutoCloseable {
    private final CoapClient client;
    private ManagedClient(String uri) {
      client = UdpFaultProxyCoapReliabilityIT.client(uri);
    }
    @Override
    public void close() {
      client.shutdown();
    }
  }

  private static byte[] payload(int size, int seed) {
    byte[] data = new byte[size];
    for (int index = 0; index < size; index++) {
      data[index] = (byte) (seed + index * 31);
    }
    return data;
  }

  private static void successfulPut(CoapResponse response) {
    assertNotNull(response);
    assertTrue(response.getCode() == CoAP.ResponseCode.CHANGED || response.getCode() == CoAP.ResponseCode.CREATED,
        "Unexpected PUT response " + response.getCode());
  }

  @Test
  void intermediateBlock1ResponseLossDoesNotCorruptReassembly() throws Exception {
    try (ManagedClient managed = new ManagedClient(getUri())) {
      CoapClient client = managed.client;
      byte[] content = payload(1024, 17);
      int blockSize = 256;
      Stats before = proxy.stats();
      for (int block = 0; block < 4; block++) {
        Request request = Request.newPut();
        request.setPayload(Arrays.copyOfRange(content, block * blockSize, (block + 1) * blockSize));
        request.getOptions().setBlock1(4, block < 3, block);
        if (block == 1) {
          proxy.dropNext(Direction.SERVER_TO_CLIENT, 1);
        }
        CoapResponse response = client.advanced(request);
        assertNotNull(response, "Block " + block + " must complete despite lost Continue");
        if (block < 3) {
          assertEquals(CoAP.ResponseCode.CONTINUE, response.getCode());
        } else {
          successfulPut(response);
        }
      }
      Stats after = proxy.stats();
      assertEquals(1, after.dropped() - before.dropped(), "The intermediate Continue must be lost");
      assertTrue(after.clientToServerReceived() - before.clientToServerReceived() >= 5,
          "A block must be retransmitted");
      assertEquals(0, after.pendingRules());
      CoapResponse result = client.get();
      assertNotNull(result);
      assertArrayEquals(content, result.getPayload());
    }
  }

  @Test
  void repeatedConfirmablePutIsIdempotentAtResourceState() throws Exception {
    try (ManagedClient managed = new ManagedClient(getUri())) {
      CoapClient client = managed.client;
      byte[] value = payload(111, 29);
      Stats before = proxy.stats();
      proxy.dropNext(Direction.SERVER_TO_CLIENT, 1);
      successfulPut(client.put(value, 0));
      Stats after = proxy.stats();
      assertEquals(1, after.dropped() - before.dropped(), "Response loss must occur");
      assertTrue(after.clientToServerReceived() - before.clientToServerReceived() >= 2,
          "Client must retransmit the confirmable request");
      CoapResponse get = client.get();
      assertNotNull(get);
      assertArrayEquals(value, get.getPayload());
      assertEquals(0, proxy.stats().pendingRules());
    }
  }

  @Test
  void observeRecoversAfterLostNotificationAndCancels() throws Exception {
    String uri = getUri();
    try (ManagedClient sub = new ManagedClient(uri); ManagedClient pub = new ManagedClient(uri)) {
      CoapClient subscriber = sub.client;
      CoapClient publisher = pub.client;
      CountDownLatch initial = new CountDownLatch(1);
      AtomicInteger notifications = new AtomicInteger();
      CoapObserveRelation relation = subscriber.observe(new CoapHandler() {
        @Override
        public void onLoad(CoapResponse response) {
          notifications.incrementAndGet();
          initial.countDown();
        }
        @Override
        public void onError() {
          initial.countDown();
        }
      });
      assertNotNull(relation);
      assertTrue(initial.await(10, TimeUnit.SECONDS), "Observe must establish before fault injection");
      Stats before = proxy.stats();
      proxy.dropNext(Direction.SERVER_TO_CLIENT, 1);
      successfulPut(publisher.put(payload(31, 1), 0));
      assertEquals(1, proxy.stats().dropped() - before.dropped(), "One server datagram must be lost");
      int observedBeforeRecovery = notifications.get();
      successfulPut(publisher.put(payload(31, 2), 0));
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
      while (notifications.get() <= observedBeforeRecovery && System.nanoTime() < deadline) {
        Thread.sleep(20);
      }
      assertTrue(notifications.get() > observedBeforeRecovery, "Observe must receive a later update");
      relation.proactiveCancel();
      assertEquals(0, proxy.stats().pendingRules());
    }
  }

  @Test
  void concurrentClientsKeepPayloadsSeparateDuringReordering() throws Exception {
    String firstUri = getUri();
    String secondUri = getUri();
    try (ManagedClient one = new ManagedClient(firstUri); ManagedClient two = new ManagedClient(secondUri)) {
      CoapClient first = one.client;
      CoapClient second = two.client;
      byte[] firstPayload = payload(127, 3);
      byte[] secondPayload = payload(129, 79);
      Stats before = proxy.stats();
      proxy.reverseNext(Direction.CLIENT_TO_SERVER, 2, Duration.ofSeconds(2));
      CountDownLatch start = new CountDownLatch(1);
      java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
      Thread firstThread = new Thread(() -> putConcurrently(first, firstPayload, start, failure));
      Thread secondThread = new Thread(() -> putConcurrently(second, secondPayload, start, failure));
      firstThread.start();
      secondThread.start();
      start.countDown();
      firstThread.join(20_000);
      secondThread.join(20_000);
      assertFalse(firstThread.isAlive() || secondThread.isAlive(), "Concurrent PUTs must finish");
      if (failure.get() != null) {
        fail("Concurrent PUT failed", failure.get());
      }
      Stats after = proxy.stats();
      assertEquals(2, after.reversed() - before.reversed(), "Both datagrams must be reordered");
      assertEquals(0, after.pendingRules());
      assertArrayEquals(firstPayload, first.get().getPayload());
      assertArrayEquals(secondPayload, second.get().getPayload());
      assertEquals(0, proxy.stats().ioFailures());
    }
  }

  private static void putConcurrently(CoapClient client, byte[] value, CountDownLatch start,
                                      java.util.concurrent.atomic.AtomicReference<Throwable> failure) {
    try {
      start.await();
      successfulPut(client.put(value, 0));
    } catch (Throwable exception) {
      failure.compareAndSet(null, exception);
    }
  }
}
