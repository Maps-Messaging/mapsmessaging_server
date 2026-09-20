package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.CotProtocolConfigDTO;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.security.auth.Subject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CotProtocolTest {

  @Test
  void protocolIdentityInboundContractAndCloseUseInternalSession() throws Exception {
    boolean original = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    SessionManager manager = mock(SessionManager.class);
    Session session = mock(Session.class);
    SecurityContext security = mock(SecurityContext.class);
    Subject subject = new Subject();
    when(session.getSecurityContext()).thenReturn(security);
    when(security.getSubject()).thenReturn(subject);
    when(manager.create(any(), any())).thenReturn(session);

    try (MockedStatic<SessionManager> mocked = mockStatic(SessionManager.class)) {
      mocked.when(SessionManager::getInstance).thenReturn(manager);
      EndPoint endpoint = endpoint();
      CotProtocol protocol =
          new CotProtocol(endpoint, new Packet(0, false), new CotProtocolConfigDTO());

      assertEquals("CoT", protocol.getName());
      assertEquals("1.0", protocol.getVersion());
      assertTrue(protocol.getSessionId().startsWith("cot-"));
      assertSame(subject, protocol.getSubject());
      assertFalse(protocol.processPacket(new Packet(0, false)));

      MessageEvent event = mock(MessageEvent.class);
      Runnable completion = mock(Runnable.class);
      when(event.getCompletionTask()).thenReturn(completion);
      protocol.sendMessage(event);
      verify(completion).run();

      protocol.close();
      verify(manager).close(session, false);
    } finally {
      JMXManager.setEnableJMX(original);
    }
  }

  @Test
  void staticFrameSearchHelpersHandleBoundsAndWhitespace() throws Exception {
    byte[] data = "xx<?xml?>  <event>1</event>tail".getBytes();

    java.lang.reflect.Method index = CotProtocol.class.getDeclaredMethod(
        "indexOf", byte[].class, byte[].class, int.class);
    java.lang.reflect.Method last = CotProtocol.class.getDeclaredMethod(
        "lastIndexOf", byte[].class, byte[].class, int.class, int.class);
    java.lang.reflect.Method whitespace = CotProtocol.class.getDeclaredMethod(
        "isWhitespaceOnly", byte[].class, int.class, int.class);
    index.setAccessible(true);
    last.setAccessible(true);
    whitespace.setAccessible(true);

    assertEquals(12, index.invoke(null, data, "<event".getBytes(), 0));
    assertEquals(2, last.invoke(null, data, "<?xml".getBytes(), 0, 12));
    assertEquals(true, whitespace.invoke(null, data, 9, 12));
    assertEquals(-1, index.invoke(null, data, "missing".getBytes(), 0));
  }

  private static EndPoint endpoint() {
    EndPoint endpoint = mock(EndPoint.class);
    EndPointServerConfigDTO config = mock(EndPointServerConfigDTO.class);
    when(endpoint.getConfig()).thenReturn(config);
    when(config.getUrl()).thenReturn("tcp://localhost:8087");
    when(endpoint.getJMXTypePath()).thenReturn(List.of());
    return endpoint;
  }
}
