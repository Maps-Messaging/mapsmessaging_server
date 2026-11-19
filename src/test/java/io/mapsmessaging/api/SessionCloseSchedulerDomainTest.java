/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.api;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.SubSystemManager;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.utilities.threads.tasks.ThreadLocalContext;
import io.mapsmessaging.utilities.threads.tasks.ThreadStateContext;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A protocol callback must never wait for the destination-owned session cleanup to complete. */
class SessionCloseSchedulerDomainTest {

  @Test
  void destinationSubscriptionCallbackDoesNotWaitForSessionCleanup() throws Exception {
    assertCloseReturnsWithoutWaiting(DestinationImpl.SUBSCRIPTION_TASK_KEY);
  }

  @Test
  void destinationResourceCallbackDoesNotWaitForSessionCleanup() throws Exception {
    assertCloseReturnsWithoutWaiting(DestinationImpl.RESOURCE_TASK_KEY);
  }

  @Test
  void sessionSchedulerCallbackDoesNotWaitForSessionCleanup() throws Exception {
    assertCloseReturnsWithoutWaiting("SessionManagerPipeLine");
  }

  @Test
  void selectorStyleCallbackWithoutEngineDomainDoesNotWaitForSessionCleanup() throws Exception {
    assertCloseReturnsWithoutWaiting(null);
  }

  private static void assertCloseReturnsWithoutWaiting(String domain) throws Exception {
    ThreadStateContext previous = ThreadLocalContext.get();
    if (domain == null) {
      ThreadLocalContext.remove();
    } else {
      ThreadStateContext context = new ThreadStateContext();
      context.add("domain", domain);
      ThreadLocalContext.set(context);
    }
    try {
      Session session = mock(Session.class);
      SessionImpl implementation = mock(SessionImpl.class);
      MessageDaemon daemon = mock(MessageDaemon.class);
      SubSystemManager subsystem = mock(SubSystemManager.class);
      io.mapsmessaging.engine.session.SessionManager engine = mock(io.mapsmessaging.engine.session.SessionManager.class);
      CompletableFuture<Void> pendingCleanup = new CompletableFuture<>();
      when(session.getSession()).thenReturn(implementation);
      when(daemon.getSubSystemManager()).thenReturn(subsystem);
      when(subsystem.getSessionManager()).thenReturn(engine);
      when(engine.closeAsync(implementation, true)).thenReturn(pendingCleanup);

      try (MockedStatic<MessageDaemon> singleton = mockStatic(MessageDaemon.class)) {
        singleton.when(MessageDaemon::getInstance).thenReturn(daemon);
        assertTimeout(Duration.ofSeconds(2), () -> SessionManager.getInstance().close(session, true));
        assertFalse(pendingCleanup.isDone(), "The protocol must return before destination cleanup finishes");
        verify(engine).closeAsync(implementation, true);
      }
    } finally {
      if (previous == null) {
        ThreadLocalContext.remove();
      } else {
        ThreadLocalContext.set(previous);
      }
    }
  }
}
