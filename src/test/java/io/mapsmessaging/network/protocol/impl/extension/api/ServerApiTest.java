package io.mapsmessaging.network.protocol.impl.extension.api;

import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerApiTest {

  @Test
  void getSubjectReturnsSubjectFromAnActiveSession() throws Exception {
    ServerApi api = new ServerApi();
    SessionContext session = mock(SessionContext.class);
    Subject subject = new Subject();
    when(session.getSubject()).thenReturn(subject);
    sessions(api).put("s1", session);

    assertSame(
        subject,
        api.getSubject(),
        "Server API subject should reflect an active session rather than always returning null"
    );
  }

  @Test
  void closeSessionRemovesItAndClosesUnderlyingSession() throws Exception {
    ServerApi api = new ServerApi();
    SessionContext session = mock(SessionContext.class);
    when(session.getSessionId()).thenReturn("s1");
    sessions(api).put("s1", session);

    api.closeSession(session);

    assertTrue(sessions(api).isEmpty());
    verify(session).close();
  }

  @Test
  void closeAttemptsEverySessionEvenWhenOneCloseFails() throws Exception {
    ServerApi api = new ServerApi();
    SessionContext first = mock(SessionContext.class);
    SessionContext second = mock(SessionContext.class);
    doThrow(new IOException("boom")).when(first).close();
    sessions(api).put("a", first);
    sessions(api).put("b", second);

    assertDoesNotThrow(api::close);

    verify(first).close();
    verify(second).close();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, SessionContext> sessions(ServerApi api) throws Exception {
    Field field = ServerApi.class.getDeclaredField("sessions");
    field.setAccessible(true);
    return (Map<String, SessionContext>) field.get(api);
  }
}
