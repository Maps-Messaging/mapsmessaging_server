/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class MavlinkTransactionStatusDispatcherTest {

  @Test
  void statusRunsOnAnotherThread() throws InterruptedException {
    CountDownLatch dispatched = new CountDownLatch(1);
    AtomicReference<Thread> executedOn = new AtomicReference<>();
    Thread submittingThread = Thread.currentThread();

    try (MavlinkTransactionStatusDispatcher dispatcher = new MavlinkTransactionStatusDispatcher()) {
      dispatcher.dispatch(() -> {
        executedOn.set(Thread.currentThread());
        dispatched.countDown();
      });

      assertTrue(dispatched.await(2, TimeUnit.SECONDS));
      assertNotSame(submittingThread, executedOn.get());
    }
  }

  @Test
  void statusesStayOrderedAndCloseDrainsAcceptedWork() {
    List<Integer> observed = new CopyOnWriteArrayList<>();

    MavlinkTransactionStatusDispatcher dispatcher = new MavlinkTransactionStatusDispatcher();
    for (int index = 0; index < 20; index++) {
      int status = index;
      dispatcher.dispatch(() -> observed.add(status));
    }
    dispatcher.close();

    assertEquals(20, observed.size());
    for (int index = 0; index < observed.size(); index++) {
      assertEquals(index, observed.get(index));
    }
    assertThrows(RejectedExecutionException.class, () -> dispatcher.dispatch(() -> {}));
  }

  @Test
  void aBusyPublisherNeverFallsBackToCallerThread() throws InterruptedException {
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch next = new CountDownLatch(1);

    try (MavlinkTransactionStatusDispatcher dispatcher = new MavlinkTransactionStatusDispatcher()) {
      dispatcher.dispatch(() -> {
        started.countDown();
        try {
          release.await();
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
        }
      });
      assertTrue(started.await(2, TimeUnit.SECONDS));
      dispatcher.dispatch(next::countDown);
      assertEquals(1L, next.getCount());
      release.countDown();
      assertTrue(next.await(2, TimeUnit.SECONDS));
    } finally {
      release.countDown();
    }
  }
}
