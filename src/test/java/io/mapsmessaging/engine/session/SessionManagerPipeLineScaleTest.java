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
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.engine.session.will.WillTaskImpl;
import io.mapsmessaging.engine.session.will.WillTaskManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessionManagerPipeLineScaleTest {

  @Test
  void thousandsOfIndependentTransientSessionsConnectAndDisconnectWithoutStateLeak() throws Exception {
    final int sessionCount = 2048;
    TestHarness harness = new TestHarness();

    Map<String, SessionImpl> sessions = new LinkedHashMap<>();
    Map<String, SubscriptionController> controllers = new LinkedHashMap<>();

    when(harness.persistentSessionManager.getSessionDetails(any(SessionContext.class))).thenAnswer(invocation -> {
      SessionContext context = invocation.getArgument(0);
      SessionDetails details = mock(SessionDetails.class);
      when(details.getUniqueId()).thenReturn("unique-" + context.getId());
      when(details.getInternalUnqueId()).thenReturn((long) context.getId().hashCode());
      when(details.getSubscriptionContextMap()).thenReturn(new LinkedHashMap<>());
      return details;
    });

    when(harness.subscriptionControllerFactory.create(any(SessionContext.class), eq(harness.destinationManager), anyMap())).thenAnswer(invocation -> {
      SessionContext context = invocation.getArgument(0);
      SubscriptionController controller = controller(context.getId(), false);
      controllers.put(context.getId(), controller);
      return controller;
    });

    when(harness.sessionFactory.create(any(SessionContext.class), any(SecurityContext.class), eq(harness.destinationManager), any(SubscriptionController.class),
        eq(harness.persistentSessionManager))).thenAnswer(invocation -> {
      SessionContext context = invocation.getArgument(0);
      SubscriptionController controller = invocation.getArgument(3);
      SessionImpl session = mock(SessionImpl.class);
      when(session.getName()).thenReturn(context.getId());
      when(session.getExpiry()).thenReturn(0L);
      when(session.getSubscriptionController()).thenReturn(controller);
      sessions.put(context.getId(), session);
      return session;
    });

    for (int i = 0; i < sessionCount; i++) {
      String sessionId = "transient-" + i;
      SessionContext context = context(sessionId, false);
      harness.pipeline.create(context);
    }

    assertEquals(sessionCount, harness.connected.sum());
    assertEquals(sessionCount, harness.pipeline.getSessions().size());
    assertEquals(0, harness.disconnected.sum());
    assertFalse(harness.pipeline.hasSubscriptions());

    for (SessionImpl session : sessions.values()) {
      harness.pipeline.close(session, true);
    }

    assertEquals(0, harness.connected.sum());
    assertEquals(0, harness.disconnected.sum());
    assertEquals(0, harness.expired.sum());
    assertFalse(harness.pipeline.hasSessions());
    assertFalse(harness.pipeline.hasSubscriptions());
    assertEquals(sessionCount, controllers.size());

    for (SubscriptionController controller : controllers.values()) {
      verify(controller, times(1)).close(false);
    }
  }

  @Test
  void concurrentProducersCanQueueThousandsOfLifecycleOperationsWithoutLosingSessions() throws Exception {
    final int sessionCount = 2048;
    DestinationManager destinationManager = mock(DestinationManager.class);
    PersistentSessionManager persistentSessionManager = mock(PersistentSessionManager.class);
    SubscriptionControllerFactory subscriptionControllerFactory = mock(SubscriptionControllerFactory.class);
    SessionFactory sessionFactory = mock(SessionFactory.class);
    SessionStateFileStore stateFileStore = mock(SessionStateFileStore.class);
    WillTaskManager willTaskManager = mock(WillTaskManager.class);
    LongAdder connected = new LongAdder();
    LongAdder disconnected = new LongAdder();
    LongAdder expired = new LongAdder();
    ExecutorService pipelineExecutor = Executors.newSingleThreadExecutor();
    BulkExpiryScheduler expiryScheduler = new BulkExpiryScheduler();
    SessionManagerPipeLine pipeline = new SessionManagerPipeLine(destinationManager, persistentSessionManager, connected, disconnected, expired, pipelineExecutor,
        expiryScheduler, stateFileStore, subscriptionControllerFactory, sessionFactory, willTaskManager);

    Map<String, SessionImpl> sessions = new java.util.concurrent.ConcurrentHashMap<>();

    when(persistentSessionManager.getSessionDetails(any(SessionContext.class))).thenAnswer(invocation -> {
      SessionContext context = invocation.getArgument(0);
      SessionDetails details = mock(SessionDetails.class);
      when(details.getUniqueId()).thenReturn("unique-" + context.getId());
      when(details.getInternalUnqueId()).thenReturn((long) context.getId().hashCode());
      when(details.getSubscriptionContextMap()).thenReturn(new LinkedHashMap<>());
      return details;
    });

    when(subscriptionControllerFactory.create(any(SessionContext.class), eq(destinationManager), anyMap())).thenAnswer(invocation -> {
      SessionContext context = invocation.getArgument(0);
      return controller(context.getId(), false);
    });

    when(sessionFactory.create(any(SessionContext.class), any(SecurityContext.class), eq(destinationManager), any(SubscriptionController.class),
        eq(persistentSessionManager))).thenAnswer(invocation -> {
      SessionContext context = invocation.getArgument(0);
      SubscriptionController controller = invocation.getArgument(3);
      SessionImpl session = mock(SessionImpl.class);
      when(session.getName()).thenReturn(context.getId());
      when(session.getExpiry()).thenReturn(0L);
      when(session.getSubscriptionController()).thenReturn(controller);
      sessions.put(context.getId(), session);
      return session;
    });

    ExecutorService producers = Executors.newFixedThreadPool(16);
    List<Future<?>> createFutures = new ArrayList<>();
    for (int i = 0; i < sessionCount; i++) {
      String sessionId = "concurrent-" + i;
      createFutures.add(producers.submit(() -> pipeline.submit(() -> pipeline.create(context(sessionId, false))).get()));
    }
    for (Future<?> future : createFutures) {
      future.get();
    }

    assertEquals(sessionCount, connected.sum());
    assertEquals(sessionCount, pipeline.getSessions().size());

    List<Future<?>> closeFutures = new ArrayList<>();
    for (SessionImpl session : sessions.values()) {
      closeFutures.add(producers.submit(() -> pipeline.submit(() -> {
        pipeline.close(session, true);
        return null;
      }).get()));
    }
    for (Future<?> future : closeFutures) {
      future.get();
    }

    producers.shutdown();
    assertTrue(producers.awaitTermination(30, TimeUnit.SECONDS));
    pipeline.stop();

    assertEquals(0, connected.sum());
    assertEquals(0, disconnected.sum());
    assertEquals(0, expired.sum());
    assertFalse(pipeline.hasSessions());
    assertFalse(pipeline.hasSubscriptions());
    assertTrue(pipelineExecutor.isShutdown());
  }

  @Test
  void massPersistentExpiryFinalisesEachControllerAndWillExactlyOnce() throws Exception {
    final int sessionCount = 1024;
    TestHarness harness = new TestHarness();
    Map<String, SubscriptionController> controllers = new LinkedHashMap<>();
    Map<String, WillTaskImpl> willTasks = new LinkedHashMap<>();

    when(harness.persistentSessionManager.getDataPath()).thenReturn("/tmp/sessions");

    when(harness.subscriptionControllerFactory.create(anyString(), any(SessionDetails.class), eq(harness.destinationManager), anyMap())).thenAnswer(invocation -> {
      String sessionId = invocation.getArgument(0);
      SubscriptionController controller = controller(sessionId, true);
      controllers.put(sessionId, controller);
      return controller;
    });

    when(harness.persistentSessionManager.removeSessionDetails(anyString())).thenAnswer(invocation -> {
      String sessionId = invocation.getArgument(0);
      SessionDetails details = mock(SessionDetails.class);
      when(details.getUniqueId()).thenReturn("unique-" + sessionId);
      return details;
    });

    when(harness.willTaskManager.remove(anyString())).thenAnswer(invocation -> willTasks.get(invocation.getArgument(0)));

    for (int i = 0; i < sessionCount; i++) {
      String sessionId = "persistent-" + i;
      SessionDetails details = mock(SessionDetails.class);
      when(details.getExpiryTime()).thenReturn(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
      WillTaskImpl willTask = mock(WillTaskImpl.class);
      willTasks.put(sessionId, willTask);
      harness.pipeline.addDisconnectedSession(sessionId, details, new LinkedHashMap<>());
    }

    assertEquals(sessionCount, harness.disconnected.sum());
    assertEquals(sessionCount, harness.pipeline.getSessionIds().size());
    assertEquals(sessionCount, harness.expiryScheduler.pendingTasks());

    harness.expiryScheduler.fireAll();
    harness.executor.runAll();

    assertEquals(0, harness.connected.sum());
    assertEquals(0, harness.disconnected.sum());
    assertEquals(sessionCount, harness.expired.sum());
    assertFalse(harness.pipeline.hasSessions());
    assertFalse(harness.pipeline.hasSubscriptions());
    assertTrue(harness.pipeline.getSessionIds().isEmpty());

    for (int i = 0; i < sessionCount; i++) {
      String sessionId = "persistent-" + i;
      SubscriptionController controller = controllers.get(sessionId);
      WillTaskImpl willTask = willTasks.get(sessionId);
      verify(controller, times(1)).close(false);
      verify(willTask, times(1)).cancel();
      verify(willTask, times(1)).run();
      verify(harness.persistentSessionManager, times(1)).removeSessionDetails(sessionId);
      verify(harness.stateFileStore, times(1)).delete("/tmp/sessions/unique-" + sessionId + ".bin");
    }
  }

  @Test
  void repeatedReconnectDisconnectOnSamePersistentSessionDoesNotDriftCountersOrExpireController() throws Exception {
    final int cycles = 1000;
    String sessionId = "persistent-churn";
    TestHarness harness = new TestHarness();
    SubscriptionController controller = controller(sessionId, true);
    SessionDetails details = mock(SessionDetails.class);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(details.getUniqueId()).thenReturn("unique-persistent-churn");
    when(details.getInternalUnqueId()).thenReturn(91L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(harness.persistentSessionManager.getSessionDetails(any(SessionContext.class))).thenReturn(details);
    when(harness.subscriptionControllerFactory.create(any(SessionContext.class), eq(harness.destinationManager), eq(subscriptions))).thenReturn(controller);

    AtomicReference<SessionImpl> latestSession = new AtomicReference<>();
    when(harness.sessionFactory.create(any(SessionContext.class), any(SecurityContext.class), eq(harness.destinationManager), eq(controller),
        eq(harness.persistentSessionManager))).thenAnswer(invocation -> {
      SessionImpl session = mock(PersistentSession.class);
      when(session.getName()).thenReturn(sessionId);
      when(session.getExpiry()).thenReturn(30L);
      when(session.getSubscriptionController()).thenReturn(controller);
      latestSession.set(session);
      return session;
    });

    SessionContext firstContext = context(sessionId, true);
    harness.pipeline.create(firstContext);

    for (int i = 0; i < cycles; i++) {
      SessionImpl current = latestSession.get();
      harness.pipeline.close(current, true);

      assertEquals(0, harness.connected.sum());
      assertEquals(1, harness.disconnected.sum());
      assertNotNull(controller.getTimeout());

      SessionContext reconnect = context(sessionId, true);
      harness.pipeline.create(reconnect);

      assertEquals(1, harness.connected.sum());
      assertEquals(0, harness.disconnected.sum());
      assertNull(controller.getTimeout());
      assertEquals(0, harness.expired.sum());
      assertSame(controller, harness.pipeline.getSessions().get(0).getSubscriptionController());
    }

    harness.pipeline.close(latestSession.get(), true);

    assertEquals(0, harness.connected.sum());
    assertEquals(1, harness.disconnected.sum());
    assertEquals(0, harness.expired.sum());

    harness.pipeline.close(sessionId, true);

    assertEquals(0, harness.connected.sum());
    assertEquals(0, harness.disconnected.sum());
    assertEquals(0, harness.expired.sum());
    assertFalse(harness.pipeline.hasSessions());
    assertFalse(harness.pipeline.hasSubscriptions());
    verify(controller, times(1)).close(false);

    harness.expiryScheduler.fireAll();
    harness.executor.runAll();

    assertEquals(0, harness.connected.sum());
    assertEquals(0, harness.disconnected.sum());
    assertEquals(0, harness.expired.sum());
    verify(controller, times(1)).close(false);
  }

  private static SessionContext context(String sessionId, boolean persistent) {
    SessionContext context = mock(SessionContext.class);
    SecurityContext securityContext = mock(SecurityContext.class);
    when(context.getId()).thenReturn(sessionId);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(context.isPersistentSession()).thenReturn(persistent);
    return context;
  }

  private static SubscriptionController controller(String sessionId, boolean persistent) {
    SubscriptionController controller = mock(SubscriptionController.class);
    AtomicReference<Future<?>> timeout = new AtomicReference<>();
    when(controller.getSessionId()).thenReturn(sessionId);
    when(controller.isPersistent()).thenReturn(persistent);
    when(controller.getTimeout()).thenAnswer(invocation -> timeout.get());
    doAnswer(invocation -> {
      timeout.set(invocation.getArgument(0));
      return null;
    }).when(controller).setTimeout(any());
    return controller;
  }

  private static final class TestHarness {

    private final DestinationManager destinationManager = mock(DestinationManager.class);
    private final PersistentSessionManager persistentSessionManager = mock(PersistentSessionManager.class);
    private final SubscriptionControllerFactory subscriptionControllerFactory = mock(SubscriptionControllerFactory.class);
    private final SessionFactory sessionFactory = mock(SessionFactory.class);
    private final SessionStateFileStore stateFileStore = mock(SessionStateFileStore.class);
    private final WillTaskManager willTaskManager = mock(WillTaskManager.class);
    private final QueuedExecutorService executor = new QueuedExecutorService();
    private final BulkExpiryScheduler expiryScheduler = new BulkExpiryScheduler();
    private final LongAdder connected = new LongAdder();
    private final LongAdder disconnected = new LongAdder();
    private final LongAdder expired = new LongAdder();
    private final SessionManagerPipeLine pipeline =
        new SessionManagerPipeLine(destinationManager, persistentSessionManager, connected, disconnected, expired, executor, expiryScheduler, stateFileStore,
            subscriptionControllerFactory, sessionFactory, willTaskManager);
  }

  private static final class BulkExpiryScheduler implements SessionExpiryScheduler {

    private final Queue<FutureTask<Void>> tasks = new ConcurrentLinkedQueue<>();

    @Override
    public Future<?> schedule(Runnable task, long delay, TimeUnit unit) {
      FutureTask<Void> future = new FutureTask<>(task, null);
      tasks.add(future);
      return future;
    }

    int pendingTasks() {
      return tasks.size();
    }

    void fireAll() {
      FutureTask<Void> task;
      while ((task = tasks.poll()) != null) {
        task.run();
      }
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
    public List<Runnable> shutdownNow() {
      shutdown = true;
      List<Runnable> remaining = new ArrayList<>(tasks);
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

    void runAll() {
      Runnable task;
      while ((task = tasks.poll()) != null) {
        task.run();
      }
    }
  }
}
