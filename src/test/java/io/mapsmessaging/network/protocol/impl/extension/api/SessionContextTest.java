package io.mapsmessaging.network.protocol.impl.extension.api;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.network.protocol.impl.extension.ExtensionProtocol;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.security.auth.Subject;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionContextTest {

  @Test
  void sessionIdentitySubjectAndCloseDelegateToMapsSession() throws Exception {
    SessionManager manager = mock(SessionManager.class);
    Session session = mock(Session.class);
    ExtensionProtocol protocol = mock(ExtensionProtocol.class);
    SecurityContext securityContext = mock(SecurityContext.class);
    Subject subject = new Subject();

    when(manager.create(any(), eq(protocol))).thenReturn(session);
    when(session.getName()).thenReturn("extension-session");
    when(session.getSecurityContext()).thenReturn(securityContext);
    when(securityContext.getSubject()).thenReturn(subject);

    try (MockedStatic<SessionManager> mocked = mockStatic(SessionManager.class)) {
      mocked.when(SessionManager::getInstance).thenReturn(manager);

      SessionContext context =
          new SessionContext(protocol, "extension-session", null, null);

      assertEquals("extension-session", context.getSessionId());
      assertSame(subject, context.getSubject());

      context.close();
      verify(manager).close(session, true);
    }
  }

  @Test
  void destinationsAreResolvedOnceAndThenCached() throws Exception {
    SessionManager manager = mock(SessionManager.class);
    Session session = mock(Session.class);
    ExtensionProtocol protocol = mock(ExtensionProtocol.class);
    Destination destination = mock(Destination.class);

    when(manager.create(any(), eq(protocol))).thenReturn(session);
    when(session.findDestination("/topic/a", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));

    try (MockedStatic<SessionManager> mocked = mockStatic(SessionManager.class)) {
      mocked.when(SessionManager::getInstance).thenReturn(manager);

      SessionContext context =
          new SessionContext(protocol, "s1", "user", "pass");

      DestinationContext first =
          context.getDestination("/topic/a", DestinationType.TOPIC);
      DestinationContext second =
          context.getDestination("/topic/a", DestinationType.TOPIC);

      assertSame(first, second);
      verify(session, times(1))
          .findDestination("/topic/a", DestinationType.TOPIC);
    }
  }

  @Test
  void subscribeAddsAtMostOnceSubscriptionAndResumesSession() throws Exception {
    SessionManager manager = mock(SessionManager.class);
    Session session = mock(Session.class);
    ExtensionProtocol protocol = mock(ExtensionProtocol.class);
    when(manager.create(any(), eq(protocol))).thenReturn(session);

    try (MockedStatic<SessionManager> mocked = mockStatic(SessionManager.class)) {
      mocked.when(SessionManager::getInstance).thenReturn(manager);

      SessionContext context =
          new SessionContext(protocol, "s2", "user", "pass");
      context.subscribe("/input/#", "temperature > 10");

      verify(session).addSubscription(any());
      verify(session).resumeState();
    }
  }
}
