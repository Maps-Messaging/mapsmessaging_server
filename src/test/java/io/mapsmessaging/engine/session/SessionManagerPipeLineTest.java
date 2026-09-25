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

import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.persistence.SessionDetails;
import io.mapsmessaging.engine.session.will.WillTaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionManagerPipeLineTest {

  private DestinationManager destinationManager;
  private PersistentSessionManager persistentSessionManager;
  private SubscriptionControllerFactory subscriptionControllerFactory;
  private SessionStateFileStore stateFileStore;
  private WillTaskManager willTaskManager;
  private QueuedExecutorService executor;
  private CapturingExpiryScheduler expiryScheduler;
  private LongAdder connected;
  private LongAdder disconnected;
  private LongAdder expired;
  private SessionManagerPipeLine pipeline;

  @BeforeEach
  void setUp() {
    destinationManager = mock(DestinationManager.class);
    persistentSessionManager = mock(PersistentSessionManager.class);
    subscriptionControllerFactory = mock(SubscriptionControllerFactory.class);
    stateFileStore = mock(SessionStateFileStore.class);
    willTaskManager = mock(WillTaskManager.class);
    executor = new QueuedExecutorService();
    expiryScheduler = new CapturingExpiryScheduler();
    connected = new LongAdder();
    disconnected = new LongAdder();
    expired = new LongAdder();

    pipeline = new SessionManagerPipeLine(destinationManager, persistentSessionManager, connected, disconnected, expired,
        executor, expiryScheduler, stateFileStore, subscriptionControllerFactory, willTaskManager);
  }

  @Test
  void restoredDisconnectedSessionSchedulesCleanupWithoutWallClockWait() throws Exception {
    String sessionId = "restored-session";
    String stateFile = "/tmp/restored-session.bin";
    SessionDetails details = mock(SessionDetails.class);
    SubscriptionController controller = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(details.getExpiryTime()).thenReturn(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
    when(subscriptionControllerFactory.create(sessionId, details, destinationManager, subscriptions)).thenReturn(controller);

    pipeline.addDisconnectedSession(sessionId, stateFile, details, subscriptions);

    assertSame(controller, pipeline.getIdleSubscriptions(sessionId));
    assertEquals(1, disconnected.sum());
    assertEquals(0, expired.sum());
    assertNotNull(expiryScheduler.task);

    expiryScheduler.fire();

    assertSame(controller, pipeline.getIdleSubscriptions(sessionId));
    assertEquals(1, executor.queuedTasks());

    executor.runNext();

    assertNull(pipeline.getIdleSubscriptions(sessionId));
    assertEquals(0, disconnected.sum());
    assertEquals(1, expired.sum());
    verify(controller).close(false);
    verify(stateFileStore).delete(stateFile);
  }

  @Test
  void alreadyExpiredRestoredSessionCleansUpImmediately() throws Exception {
    String sessionId = "expired-session";
    String stateFile = "/tmp/expired-session.bin";
    SessionDetails details = mock(SessionDetails.class);
    SubscriptionController controller = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(details.getExpiryTime()).thenReturn(System.currentTimeMillis() - 1);
    when(subscriptionControllerFactory.create(sessionId, details, destinationManager, subscriptions)).thenReturn(controller);

    pipeline.addDisconnectedSession(sessionId, stateFile, details, subscriptions);

    assertNull(pipeline.getIdleSubscriptions(sessionId));
    assertEquals(0, disconnected.sum());
    assertEquals(0, expired.sum());
    assertNull(expiryScheduler.task);
    verify(controller).close(false);
    verify(stateFileStore).delete(stateFile);
  }

  @Test
  void stateFileDeletionFailureDoesNotPreventControllerCleanup() throws Exception {
    String sessionId = "delete-failure";
    String stateFile = "/tmp/delete-failure.bin";
    SessionDetails details = mock(SessionDetails.class);
    SubscriptionController controller = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(details.getExpiryTime()).thenReturn(System.currentTimeMillis() - 1);
    when(subscriptionControllerFactory.create(sessionId, details, destinationManager, subscriptions)).thenReturn(controller);
    doThrow(new IOException("expected")).when(stateFileStore).delete(stateFile);

    assertDoesNotThrow(() -> pipeline.addDisconnectedSession(sessionId, stateFile, details, subscriptions));

    assertNull(pipeline.getIdleSubscriptions(sessionId));
    assertEquals(0, disconnected.sum());
    verify(controller).close(false);
  }

  @Test
  void closingPersistentLifetimeSessionHibernatesAndSchedulesCleanup() {
    SessionImpl session = mock(SessionImpl.class);
    SubscriptionController controller = controller("active-session");

    connected.increment();
    when(session.getName()).thenReturn("active-session");
    when(session.getExpiry()).thenReturn(30L);
    when(session.getSubscriptionController()).thenReturn(controller);

    pipeline.close(session, true);

    assertEquals(0, connected.sum());
    assertEquals(1, disconnected.sum());
    assertSame(expiryScheduler.future, timeoutOf(controller));
    verify(session).close();
    verify(controller).hibernateAll();
    verify(willTaskManager).remove("active-session");

    expiryScheduler.fire();
    executor.runNext();

    assertEquals(0, disconnected.sum());
    assertEquals(1, expired.sum());
    verify(controller).close(false);
  }

  @Test
  void zeroExpirySessionCleansUpSynchronously() {
    SessionImpl session = mock(SessionImpl.class);
    SubscriptionController controller = controller("non-persistent-session");

    connected.increment();
    when(session.getName()).thenReturn("non-persistent-session");
    when(session.getExpiry()).thenReturn(0L);
    when(session.getSubscriptionController()).thenReturn(controller);

    pipeline.close(session, true);

    assertEquals(0, connected.sum());
    assertEquals(0, disconnected.sum());
    assertEquals(0, expired.sum());
    assertNull(expiryScheduler.task);
    verify(controller).close(false);
  }

  @Test
  void submitUsesInjectedPipelineExecutor() throws Exception {
    Future<?> result = pipeline.submit((Callable<String>) () -> "done");

    assertFalse(result.isDone());
    assertEquals(1, executor.queuedTasks());

    executor.runNext();

    assertTrue(result.isDone());
    assertEquals("done", result.get());
  }

  @Test
  void stopShutsDownDisconnectedControllers() {
    String sessionId = "stopped-session";
    SessionDetails details = mock(SessionDetails.class);
    SubscriptionController controller = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(details.getExpiryTime()).thenReturn(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
    when(subscriptionControllerFactory.create(sessionId, details, destinationManager, subscriptions)).thenReturn(controller);

    pipeline.addDisconnectedSession(sessionId, "/tmp/stopped-session.bin", details, subscriptions);
    pipeline.stop();

    verify(controller).shutdown();
  }

  private SubscriptionController controller(String sessionId) {
    SubscriptionController controller = mock(SubscriptionController.class);
    AtomicReference<Future<?>> timeout = new AtomicReference<>();

    when(controller.getSessionId()).thenReturn(sessionId);
    when(controller.getTimeout()).thenAnswer(invocation -> timeout.get());
    doAnswer(invocation -> {
      timeout.set(invocation.getArgument(0));
      return null;
    }).when(controller).setTimeout(any());

    return controller;
  }

  private Future<?> timeoutOf(SubscriptionController controller) {
    return controller.getTimeout();
  }

  private static final class CapturingExpiryScheduler implements SessionExpiryScheduler {

    private Runnable task;
    private FutureTask<Void> future;

    @Override
    public Future<?> schedule(Runnable task, long delay, TimeUnit unit) {
      this.task = task;
      this.future = new FutureTask<>(task, null);
      return future;
    }

    void fire() {
      assertNotNull(future);
      future.run();
    }
  }

  private static final class QueuedExecutorService extends AbstractExecutorService {

    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private boolean shutdown;

    @Override
    public void shutdown() {
      shutdown = true;
    }

    @Override
    public java.util.List<Runnable> shutdownNow() {
      shutdown = true;
      java.util.List<Runnable> remaining = java.util.List.copyOf(tasks);
      tasks.clear();
      return remaining;
    }

    @Override
    public boolean isShutdown() {
      return shutdown;
    }

    @Override
    public boolean isTerminated() {
      return shutdown && tasks.isEmpty();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return isTerminated();
    }

    @Override
    public void execute(Runnable command) {
      tasks.add(command);
    }

    int queuedTasks() {
      return tasks.size();
    }

    void runNext() {
      Runnable task = tasks.poll();
      assertNotNull(task);
      task.run();
    }
  }
}
