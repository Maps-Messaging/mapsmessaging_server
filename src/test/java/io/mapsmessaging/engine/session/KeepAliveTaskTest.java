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

package io.mapsmessaging.engine.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Principal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class KeepAliveTaskTest {

  @Test
  void run_contains_runtime_exception_and_keeps_periodic_task_active() throws InterruptedException {
    AtomicInteger invocationCount = new AtomicInteger();
    CountDownLatch successfulInvocation = new CountDownLatch(1);
    ClientConnection clientConnection = new TestClientConnection(invocationCount, successfulInvocation);
    KeepAliveTask keepAliveTask = new KeepAliveTask(clientConnection);
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(keepAliveTask, 0, 10, TimeUnit.MILLISECONDS);

    try {
      assertTrue(successfulInvocation.await(1, TimeUnit.SECONDS));
      assertTrue(invocationCount.get() >= 2);
      assertFalse(scheduledFuture.isDone());
    } finally {
      scheduledFuture.cancel(true);
      scheduler.shutdownNow();
      assertTrue(scheduler.awaitTermination(1, TimeUnit.SECONDS));
    }
  }

  @Test
  void default_interval_retains_previous_timeout_grace() {
    ClientConnection clientConnection = new TestClientConnection(new AtomicInteger(), new CountDownLatch(1));

    assertEquals(65_000L, clientConnection.getKeepAliveTaskInterval());
  }

  private static final class TestClientConnection implements ClientConnection {

    private final AtomicInteger invocationCount;
    private final CountDownLatch successfulInvocation;

    private TestClientConnection(AtomicInteger invocationCount, CountDownLatch successfulInvocation) {
      this.invocationCount = invocationCount;
      this.successfulInvocation = successfulInvocation;
    }

    @Override
    public long getTimeOut() {
      return 60_000L;
    }

    @Override
    public String getName() {
      return "test";
    }

    @Override
    public String getVersion() {
      return "test";
    }

    @Override
    public void sendKeepAlive() {
      if (invocationCount.incrementAndGet() == 1) {
        throw new IllegalStateException("expected test exception");
      }
      successfulInvocation.countDown();
    }

    @Override
    public Principal getPrincipal() {
      return null;
    }

    @Override
    public String getAuthenticationConfig() {
      return "";
    }

    @Override
    public String getUniqueName() {
      return "test";
    }

    @Override
    public String getProtocolName() {
      return "test";
    }

    @Override
    public String getRemoteIp() {
      return "127.0.0.1";
    }
  }
}
