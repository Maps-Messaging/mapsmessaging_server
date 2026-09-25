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
import io.mapsmessaging.engine.session.will.WillTaskImpl;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SessionManagerPipeLineTest {

  private DestinationManager destinationManager;
  private PersistentSessionManager persistentSessionManager;
  private SubscriptionControllerFactory subscriptionControllerFactory;
  private SessionFactory sessionFactory;
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
    sessionFactory = mock(SessionFactory.class);
    stateFileStore = mock(SessionStateFileStore.class);
    willTaskManager = mock(WillTaskManager.class);
    executor = new QueuedExecutorService();
    expiryScheduler = new CapturingExpiryScheduler();
    connected = new LongAdder();
    disconnected = new LongAdder();
    expired = new LongAdder();

    pipeline = new SessionManagerPipeLine(destinationManager, persistentSessionManager, connected, disconnected, expired,
        executor, expiryScheduler, stateFileStore, subscriptionControllerFactory, sessionFactory, willTaskManager);
  }



  @Test
  void subscriptionAccessorsReportIdleControllerStateDirectly() {
    String sessionId = "idle-session";
    SessionDetails details = mock(SessionDetails.class);
    SubscriptionController controller = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    assertFalse(pipeline.hasSubscriptions());
    assertTrue(pipeline.getSessionIds().isEmpty());

    when(details.getExpiryTime()).thenReturn(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
    when(subscriptionControllerFactory.create(sessionId, details, destinationManager, subscriptions)).thenReturn(controller);

    pipeline.addDisconnectedSession(sessionId, "/tmp/idle-session.bin", details, subscriptions);

    assertTrue(pipeline.hasSubscriptions());
    assertEquals(java.util.Set.of(sessionId), pipeline.getSessionIds());
    assertThrows(UnsupportedOperationException.class, () -> pipeline.getSessionIds().add("other"));
  }

  @Test
  void createNewSessionBuildsControllerAndRecordsSession() throws Exception {
    String sessionId = "new-session";
    SessionContext context = mock(SessionContext.class);
    SessionDetails details = mock(SessionDetails.class);
    io.mapsmessaging.engine.session.security.SecurityContext securityContext =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    SubscriptionController controller = controller(sessionId);
    SessionImpl session = mock(SessionImpl.class);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(context.getId()).thenReturn(sessionId);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(details.getUniqueId()).thenReturn("unique-new-session");
    when(details.getInternalUnqueId()).thenReturn(17L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(persistentSessionManager.getSessionDetails(context)).thenReturn(details);
    when(subscriptionControllerFactory.create(context, destinationManager, subscriptions)).thenReturn(controller);
    when(sessionFactory.create(context, securityContext, destinationManager, controller, persistentSessionManager)).thenReturn(session);
    when(session.getName()).thenReturn(sessionId);

    SessionImpl created = pipeline.create(context);

    assertSame(session, created);
    assertTrue(pipeline.hasSessions());
    assertEquals(1, pipeline.getSessions().size());
    assertSame(session, pipeline.getSessions().get(0));
    assertEquals(1, connected.sum());
    verify(context).setRestored(false);
    verify(context).setUniqueId("unique-new-session");
    verify(context).setInternalSessionId(17L);
  }



  @Test
  void administrativeCloseFullyTerminatesActivePersistentSession() throws Exception {
    String sessionId = "active-controller-close";
    String uniqueId = "unique-active-controller-close";
    String stateFile = "/tmp/sessions/" + uniqueId + ".bin";
    SessionContext context = mock(SessionContext.class);
    SessionDetails details = mock(SessionDetails.class);
    io.mapsmessaging.engine.session.security.SecurityContext securityContext =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    PersistentSession session = mock(PersistentSession.class);
    SubscriptionController controller = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(context.getId()).thenReturn(sessionId);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(context.isPersistentSession()).thenReturn(true);
    when(details.getUniqueId()).thenReturn(uniqueId);
    when(details.getInternalUnqueId()).thenReturn(67L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(persistentSessionManager.getSessionDetails(context)).thenReturn(details);
    when(persistentSessionManager.getSessionDetails(sessionId)).thenReturn(details);
    when(persistentSessionManager.getDataPath()).thenReturn("/tmp/sessions");
    when(subscriptionControllerFactory.create(context, destinationManager, subscriptions)).thenReturn(controller);
    when(sessionFactory.create(context, securityContext, destinationManager, controller, persistentSessionManager)).thenReturn(session);
    when(session.getName()).thenReturn(sessionId);
    when(session.getStoreName()).thenReturn(stateFile);
    when(session.getExpiry()).thenReturn(30L);
    when(session.getSubscriptionController()).thenReturn(controller);

    pipeline.create(context);
    pipeline.close(sessionId, false);

    assertEquals(0, connected.sum());
    assertEquals(0, disconnected.sum());
    assertFalse(pipeline.hasSessions());
    assertFalse(pipeline.hasSubscriptions());
    assertNull(pipeline.getIdleSubscriptions(sessionId));
    verify(session).close();
    verify(controller).hibernateAll();
    verify(controller).close(false);
    verify(stateFileStore).delete(stateFile);
  }

  @Test
  void administrativeCloseFullyTerminatesDisconnectedPersistentSession() throws Exception {
    String sessionId = "inactive-controller-close";
    String uniqueId = "unique-inactive-controller-close";
    String stateFile = "/tmp/sessions/" + uniqueId + ".bin";
    SessionContext context = mock(SessionContext.class);
    SessionDetails details = mock(SessionDetails.class);
    io.mapsmessaging.engine.session.security.SecurityContext securityContext =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    PersistentSession session = mock(PersistentSession.class);
    SubscriptionController controller = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(context.getId()).thenReturn(sessionId);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(context.isPersistentSession()).thenReturn(true);
    when(details.getUniqueId()).thenReturn(uniqueId);
    when(details.getInternalUnqueId()).thenReturn(71L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(persistentSessionManager.getSessionDetails(context)).thenReturn(details);
    when(persistentSessionManager.getSessionDetails(sessionId)).thenReturn(details);
    when(persistentSessionManager.getDataPath()).thenReturn("/tmp/sessions");
    when(subscriptionControllerFactory.create(context, destinationManager, subscriptions)).thenReturn(controller);
    when(sessionFactory.create(context, securityContext, destinationManager, controller, persistentSessionManager)).thenReturn(session);
    when(session.getName()).thenReturn(sessionId);
    when(session.getStoreName()).thenReturn(stateFile);
    when(session.getExpiry()).thenReturn(30L);
    when(session.getSubscriptionController()).thenReturn(controller);

    pipeline.create(context);
    pipeline.close(session, true);

    assertFalse(pipeline.hasSessions());
    assertEquals(1, disconnected.sum());

    pipeline.close(sessionId, false);

    assertEquals(0, disconnected.sum());
    assertFalse(pipeline.hasSubscriptions());
    assertNull(pipeline.getIdleSubscriptions(sessionId));
    verify(controller).close(false);
    verify(stateFileStore).delete(stateFile);
  }

  @Test
  void staleCloseCannotRemoveReplacementSessionOrChangeCounters() throws Exception {
    String sessionId = "stale-close";
    SessionContext firstContext = mock(SessionContext.class);
    SessionContext secondContext = mock(SessionContext.class);
    SessionDetails details = mock(SessionDetails.class);
    io.mapsmessaging.engine.session.security.SecurityContext firstSecurity =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    io.mapsmessaging.engine.session.security.SecurityContext secondSecurity =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    SubscriptionController controller = controller(sessionId, false);
    SessionImpl firstSession = mock(SessionImpl.class);
    SessionImpl secondSession = mock(SessionImpl.class);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(firstContext.getId()).thenReturn(sessionId);
    when(firstContext.getSecurityContext()).thenReturn(firstSecurity);
    when(secondContext.getId()).thenReturn(sessionId);
    when(secondContext.getSecurityContext()).thenReturn(secondSecurity);
    when(details.getUniqueId()).thenReturn("unique-stale-close");
    when(details.getInternalUnqueId()).thenReturn(47L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(persistentSessionManager.getSessionDetails(any(SessionContext.class))).thenReturn(details);
    when(subscriptionControllerFactory.create(any(SessionContext.class), eq(destinationManager), eq(subscriptions))).thenReturn(controller);
    when(sessionFactory.create(firstContext, firstSecurity, destinationManager, controller, persistentSessionManager)).thenReturn(firstSession);
    when(sessionFactory.create(secondContext, secondSecurity, destinationManager, controller, persistentSessionManager)).thenReturn(secondSession);
    when(firstSession.getName()).thenReturn(sessionId);
    when(secondSession.getName()).thenReturn(sessionId);

    pipeline.create(firstContext);
    pipeline.create(secondContext);

    assertEquals(1, connected.sum());
    assertSame(secondSession, pipeline.getSessions().get(0));
    verify(firstSession).close();

    pipeline.close(firstSession, true);

    assertEquals(1, connected.sum());
    assertSame(secondSession, pipeline.getSessions().get(0));
    verify(firstSession, times(1)).close();
    verify(secondSession, never()).close();
  }

  @Test
  void duplicateCreateReplacesActiveSessionWithoutInflatingConnectedCount() throws Exception {
    String sessionId = "duplicate-create";
    SessionContext firstContext = mock(SessionContext.class);
    SessionContext secondContext = mock(SessionContext.class);
    SessionDetails details = mock(SessionDetails.class);
    io.mapsmessaging.engine.session.security.SecurityContext firstSecurity =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    io.mapsmessaging.engine.session.security.SecurityContext secondSecurity =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    SubscriptionController controller = controller(sessionId, false);
    SessionImpl firstSession = mock(SessionImpl.class);
    SessionImpl secondSession = mock(SessionImpl.class);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(firstContext.getId()).thenReturn(sessionId);
    when(firstContext.getSecurityContext()).thenReturn(firstSecurity);
    when(secondContext.getId()).thenReturn(sessionId);
    when(secondContext.getSecurityContext()).thenReturn(secondSecurity);
    when(details.getUniqueId()).thenReturn("unique-duplicate");
    when(details.getInternalUnqueId()).thenReturn(53L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(persistentSessionManager.getSessionDetails(any(SessionContext.class))).thenReturn(details);
    when(subscriptionControllerFactory.create(any(SessionContext.class), eq(destinationManager), eq(subscriptions))).thenReturn(controller);
    when(sessionFactory.create(firstContext, firstSecurity, destinationManager, controller, persistentSessionManager)).thenReturn(firstSession);
    when(sessionFactory.create(secondContext, secondSecurity, destinationManager, controller, persistentSessionManager)).thenReturn(secondSession);
    when(firstSession.getName()).thenReturn(sessionId);
    when(secondSession.getName()).thenReturn(sessionId);

    pipeline.create(firstContext);
    pipeline.create(secondContext);

    assertEquals(1, connected.sum());
    assertEquals(1, pipeline.getSessions().size());
    assertSame(secondSession, pipeline.getSessions().get(0));
    verify(firstSession).close();
    verify(secondSession, never()).close();
  }

  @Test
  void reconnectBeforeExpiryRestoresExistingControllerAndCancelsTimeout() throws Exception {
    String sessionId = "reconnect-session";
    SessionDetails details = mock(SessionDetails.class);
    SubscriptionController controller = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(details.getExpiryTime()).thenReturn(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
    when(details.getUniqueId()).thenReturn("unique-reconnect");
    when(details.getInternalUnqueId()).thenReturn(23L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(subscriptionControllerFactory.create(sessionId, details, destinationManager, subscriptions)).thenReturn(controller);

    pipeline.addDisconnectedSession(sessionId, "/tmp/reconnect.bin", details, subscriptions);
    Future<?> timeout = controller.getTimeout();

    SessionContext context = mock(SessionContext.class);
    io.mapsmessaging.engine.session.security.SecurityContext securityContext =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    SessionImpl session = mock(SessionImpl.class);

    when(context.getId()).thenReturn(sessionId);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(persistentSessionManager.getSessionDetails(context)).thenReturn(details);
    when(sessionFactory.create(context, securityContext, destinationManager, controller, persistentSessionManager)).thenReturn(session);
    when(session.getName()).thenReturn(sessionId);

    SessionImpl created = pipeline.create(context);

    assertSame(session, created);
    assertTrue(timeout.isCancelled());
    assertNull(controller.getTimeout());
    assertEquals(0, disconnected.sum());
    assertEquals(1, connected.sum());
    verify(context).setRestored(true);
    verify(controller, never()).close(false);
  }

  @Test
  void resetStateReconnectReplacesExistingController() throws Exception {
    String sessionId = "reset-session";
    SessionDetails details = mock(SessionDetails.class);
    SubscriptionController oldController = controller(sessionId);
    SubscriptionController newController = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(details.getExpiryTime()).thenReturn(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
    when(details.getUniqueId()).thenReturn("unique-reset");
    when(details.getInternalUnqueId()).thenReturn(29L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(subscriptionControllerFactory.create(sessionId, details, destinationManager, subscriptions)).thenReturn(oldController);

    pipeline.addDisconnectedSession(sessionId, "/tmp/reset.bin", details, subscriptions);

    SessionContext context = mock(SessionContext.class);
    io.mapsmessaging.engine.session.security.SecurityContext securityContext =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    SessionImpl session = mock(SessionImpl.class);

    when(context.getId()).thenReturn(sessionId);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(context.isResetState()).thenReturn(true);
    when(context.isPersistentSession()).thenReturn(true);
    when(persistentSessionManager.getSessionDetails(context)).thenReturn(details);
    when(subscriptionControllerFactory.create(eq(context), eq(destinationManager), anyMap())).thenReturn(newController);
    when(sessionFactory.create(context, securityContext, destinationManager, newController, persistentSessionManager)).thenReturn(session);
    when(session.getName()).thenReturn(sessionId);

    pipeline.create(context);

    assertNull(pipeline.getIdleSubscriptions(sessionId));
    assertFalse(pipeline.hasSubscriptions());
    assertEquals(0, disconnected.sum());
    assertEquals(1, connected.sum());
    verify(oldController).close(false);
    verify(details).clearSubscriptions();
    verify(context, never()).setRestored(true);
  }

  @Test
  void timerFiredWhileReconnectRunsCannotLetQueuedCleanupDeleteReplacement() throws Exception {
    String sessionId = "racing-session";
    String stateFile = "/tmp/racing-session.bin";
    SessionDetails details = mock(SessionDetails.class);
    SubscriptionController oldController = controller(sessionId);
    SubscriptionController replacementController = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(details.getExpiryTime()).thenReturn(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
    when(details.getUniqueId()).thenReturn("unique-racing");
    when(details.getInternalUnqueId()).thenReturn(31L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(subscriptionControllerFactory.create(sessionId, details, destinationManager, subscriptions)).thenReturn(oldController);

    pipeline.addDisconnectedSession(sessionId, stateFile, details, subscriptions);
    expiryScheduler.fire();
    assertEquals(1, executor.queuedTasks());

    SessionContext context = mock(SessionContext.class);
    io.mapsmessaging.engine.session.security.SecurityContext securityContext =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    SessionImpl session = mock(SessionImpl.class);

    when(context.getId()).thenReturn(sessionId);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(context.isPersistentSession()).thenReturn(true);
    when(persistentSessionManager.getSessionDetails(context)).thenReturn(details);
    when(sessionFactory.create(context, securityContext, destinationManager, oldController, persistentSessionManager)).thenReturn(session);
    when(session.getName()).thenReturn(sessionId);

    pipeline.create(context);
    assertNull(pipeline.getIdleSubscriptions(sessionId));
    assertFalse(pipeline.hasSubscriptions());
    assertEquals(0, disconnected.sum());
    assertEquals(0, expired.sum());

    executor.runNext();

    assertNull(pipeline.getIdleSubscriptions(sessionId));
    assertFalse(pipeline.hasSubscriptions());
    verify(oldController, never()).close(false);
    verify(replacementController, never()).close(false);
    verify(stateFileStore, never()).delete(stateFile);
  }


  @Test
  void expiryCleanupRunningBeforeReconnectWinsLifecycleOrdering() throws Exception {
    String sessionId = "expired-before-reconnect";
    String stateFile = "/tmp/expired-before-reconnect.bin";
    SessionDetails details = mock(SessionDetails.class);
    SubscriptionController expiredController = controller(sessionId);
    SubscriptionController replacementController = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(details.getExpiryTime()).thenReturn(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
    when(details.getUniqueId()).thenReturn("unique-expired-before-reconnect");
    when(details.getInternalUnqueId()).thenReturn(37L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(subscriptionControllerFactory.create(sessionId, details, destinationManager, subscriptions)).thenReturn(expiredController);

    pipeline.addDisconnectedSession(sessionId, stateFile, details, subscriptions);
    expiryScheduler.fire();
    executor.runNext();

    assertNull(pipeline.getIdleSubscriptions(sessionId));
    assertEquals(0, disconnected.sum());
    assertEquals(1, expired.sum());
    verify(expiredController).close(false);
    verify(stateFileStore).delete(stateFile);

    SessionContext context = mock(SessionContext.class);
    io.mapsmessaging.engine.session.security.SecurityContext securityContext =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    SessionImpl session = mock(SessionImpl.class);

    when(context.getId()).thenReturn(sessionId);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(context.isPersistentSession()).thenReturn(true);
    when(persistentSessionManager.getSessionDetails(context)).thenReturn(details);
    when(subscriptionControllerFactory.create(eq(context), eq(destinationManager), anyMap())).thenReturn(replacementController);
    when(sessionFactory.create(context, securityContext, destinationManager, replacementController, persistentSessionManager)).thenReturn(session);
    when(session.getName()).thenReturn(sessionId);

    pipeline.create(context);

    assertNull(pipeline.getIdleSubscriptions(sessionId));
    assertFalse(pipeline.hasSubscriptions());
    assertEquals(1, connected.sum());
    assertEquals(0, disconnected.sum());
    assertEquals(1, expired.sum());
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
    assertEquals(1, expired.sum());
    assertNull(expiryScheduler.task);
    verify(controller).close(false);
    verify(stateFileStore).delete(stateFile);
  }


  @Test
  void duplicatePersistentCleanupIsIdempotent() throws Exception {
    String sessionId = "idempotent-session";
    String stateFile = "/tmp/idempotent-session.bin";
    SessionDetails details = mock(SessionDetails.class);
    SubscriptionController controller = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(details.getExpiryTime()).thenReturn(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
    when(subscriptionControllerFactory.create(sessionId, details, destinationManager, subscriptions)).thenReturn(controller);

    pipeline.addDisconnectedSession(sessionId, stateFile, details, subscriptions);
    expiryScheduler.fire();
    executor.runNext();

    assertEquals(0, disconnected.sum());
    assertEquals(1, expired.sum());

    pipeline.closeAndDeleteSubscriptionController(stateFile, controller);

    assertEquals(0, disconnected.sum());
    assertEquals(1, expired.sum());
    verify(controller, times(1)).close(false);
    verify(stateFileStore, times(1)).delete(stateFile);
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
  void closingPersistentLifetimeSessionHibernatesAndSchedulesCleanup() throws Exception {
    String sessionId = "active-session";
    SessionContext context = mock(SessionContext.class);
    SessionDetails details = mock(SessionDetails.class);
    io.mapsmessaging.engine.session.security.SecurityContext securityContext =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    PersistentSession session = mock(PersistentSession.class);
    SubscriptionController controller = controller(sessionId);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(context.getId()).thenReturn(sessionId);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(context.isPersistentSession()).thenReturn(true);
    when(details.getUniqueId()).thenReturn("unique-active-session");
    when(details.getInternalUnqueId()).thenReturn(41L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(persistentSessionManager.getSessionDetails(context)).thenReturn(details);
    when(subscriptionControllerFactory.create(context, destinationManager, subscriptions)).thenReturn(controller);
    when(sessionFactory.create(context, securityContext, destinationManager, controller, persistentSessionManager)).thenReturn(session);
    when(session.getName()).thenReturn(sessionId);
    when(session.getStoreName()).thenReturn("/tmp/active-session.bin");
    when(session.getExpiry()).thenReturn(30L);
    when(session.getSubscriptionController()).thenReturn(controller);

    pipeline.create(context);
    assertEquals(1, connected.sum());

    pipeline.close(session, true);

    assertEquals(0, connected.sum());
    assertEquals(1, disconnected.sum());
    assertNotNull(timeoutOf(controller));
    verify(session).close();
    verify(controller).hibernateAll();
    verify(willTaskManager).remove(sessionId);

    expiryScheduler.fire();
    executor.runNext();

    assertEquals(0, disconnected.sum());
    assertEquals(1, expired.sum());
    verify(controller).close(false);
  }



  @Test
  void persistentExpirySchedulesThenForcesPendingWillOnFinalCleanup() throws Exception {
    String sessionId = "persistent-will";
    String stateFile = "/tmp/persistent-will.bin";
    SessionContext context = mock(SessionContext.class);
    SessionDetails details = mock(SessionDetails.class);
    io.mapsmessaging.engine.session.security.SecurityContext securityContext =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    PersistentSession session = mock(PersistentSession.class);
    SubscriptionController controller = controller(sessionId);
    WillTaskImpl willTask = mock(WillTaskImpl.class);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(context.getId()).thenReturn(sessionId);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(context.isPersistentSession()).thenReturn(true);
    when(details.getUniqueId()).thenReturn("unique-persistent-will");
    when(details.getInternalUnqueId()).thenReturn(61L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(persistentSessionManager.getSessionDetails(context)).thenReturn(details);
    when(subscriptionControllerFactory.create(context, destinationManager, subscriptions)).thenReturn(controller);
    when(sessionFactory.create(context, securityContext, destinationManager, controller, persistentSessionManager)).thenReturn(session);
    when(session.getName()).thenReturn(sessionId);
    when(session.getStoreName()).thenReturn(stateFile);
    when(session.getExpiry()).thenReturn(30L);
    when(session.getSubscriptionController()).thenReturn(controller);
    when(willTaskManager.get(sessionId)).thenReturn(willTask);
    when(willTaskManager.remove(sessionId)).thenReturn(willTask);

    pipeline.create(context);
    pipeline.close(session, false);

    verify(willTask).schedule();
    verify(willTask, never()).cancel();
    verify(willTask, never()).run();

    expiryScheduler.fire();
    executor.runNext();

    verify(willTask).cancel();
    verify(willTask).run();
    verify(stateFileStore).delete(stateFile);
    assertEquals(0, connected.sum());
    assertEquals(0, disconnected.sum());
    assertEquals(1, expired.sum());
  }

  @Test
  void positiveExpiryNonPersistentSessionStillClosesImmediately() throws Exception {
    String sessionId = "non-persistent-positive-expiry";
    SessionContext context = mock(SessionContext.class);
    SessionDetails details = mock(SessionDetails.class);
    io.mapsmessaging.engine.session.security.SecurityContext securityContext =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    SessionImpl session = mock(SessionImpl.class);
    SubscriptionController controller = controller(sessionId, false);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(context.getId()).thenReturn(sessionId);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(details.getUniqueId()).thenReturn("unique-non-persistent-positive");
    when(details.getInternalUnqueId()).thenReturn(59L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(persistentSessionManager.getSessionDetails(context)).thenReturn(details);
    when(subscriptionControllerFactory.create(context, destinationManager, subscriptions)).thenReturn(controller);
    when(sessionFactory.create(context, securityContext, destinationManager, controller, persistentSessionManager)).thenReturn(session);
    when(session.getName()).thenReturn(sessionId);
    when(session.getExpiry()).thenReturn(100L);
    when(session.getSubscriptionController()).thenReturn(controller);

    pipeline.create(context);
    pipeline.close(session, true);

    assertEquals(0, connected.sum());
    assertEquals(0, disconnected.sum());
    assertEquals(0, expired.sum());
    assertNull(expiryScheduler.task);
    verify(controller, never()).hibernateAll();
    verify(controller).close(false);
    verifyNoInteractions(stateFileStore);
  }

  @Test
  void zeroExpirySessionCleansUpSynchronously() throws Exception {
    String sessionId = "non-persistent-session";
    SessionContext context = mock(SessionContext.class);
    SessionDetails details = mock(SessionDetails.class);
    io.mapsmessaging.engine.session.security.SecurityContext securityContext =
        mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    SessionImpl session = mock(SessionImpl.class);
    SubscriptionController controller = controller(sessionId, false);
    Map<String, SubscriptionContext> subscriptions = new LinkedHashMap<>();

    when(context.getId()).thenReturn(sessionId);
    when(context.getSecurityContext()).thenReturn(securityContext);
    when(details.getUniqueId()).thenReturn("unique-non-persistent");
    when(details.getInternalUnqueId()).thenReturn(43L);
    when(details.getSubscriptionContextMap()).thenReturn(subscriptions);
    when(persistentSessionManager.getSessionDetails(context)).thenReturn(details);
    when(subscriptionControllerFactory.create(context, destinationManager, subscriptions)).thenReturn(controller);
    when(sessionFactory.create(context, securityContext, destinationManager, controller, persistentSessionManager)).thenReturn(session);
    when(session.getName()).thenReturn(sessionId);
    when(session.getExpiry()).thenReturn(0L);
    when(session.getSubscriptionController()).thenReturn(controller);

    pipeline.create(context);
    pipeline.close(session, true);

    assertEquals(0, connected.sum());
    assertEquals(0, disconnected.sum());
    assertEquals(0, expired.sum());
    assertNull(expiryScheduler.task);
    verify(controller).close(false);
    verifyNoInteractions(stateFileStore);
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
    Future<?> timeout = controller.getTimeout();

    pipeline.stop();

    assertTrue(timeout.isCancelled());
    assertNull(controller.getTimeout());
    assertTrue(executor.isShutdown());
    verify(controller).shutdown();
  }

  private SubscriptionController controller(String sessionId) {
    return controller(sessionId, true);
  }

  private SubscriptionController controller(String sessionId, boolean persistent) {
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
