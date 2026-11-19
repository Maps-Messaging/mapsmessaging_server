/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.api;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.utilities.threads.tasks.ThreadLocalContext;
import io.mapsmessaging.utilities.threads.tasks.ThreadStateContext;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/** A synchronous close from an engine serial domain cannot wait on its own queued cleanup. */
class SessionCloseSchedulerDomainTest {

  @Test
  void destinationSubscriptionCallbackCannotSynchronouslyCloseSession() {
    assertBlockingCloseRejected(DestinationImpl.SUBSCRIPTION_TASK_KEY);
  }

  @Test
  void destinationResourceTaskCannotSynchronouslyCloseSession() {
    assertBlockingCloseRejected(DestinationImpl.RESOURCE_TASK_KEY);
  }

  @Test
  void sessionSchedulerTaskCannotSynchronouslyCloseSession() {
    assertBlockingCloseRejected("SessionManagerPipeLine");
  }

  private static void assertBlockingCloseRejected(String domain) {
    ThreadStateContext previous = ThreadLocalContext.get();
    ThreadStateContext context = new ThreadStateContext();
    context.add("domain", domain);
    ThreadLocalContext.set(context);
    try {
      Session session = mock(Session.class);
      assertThrows(IOException.class, () -> SessionManager.getInstance().close(session, true));
    } finally {
      if (previous == null) {
        ThreadLocalContext.remove();
      } else {
        ThreadLocalContext.set(previous);
      }
    }
  }
}
