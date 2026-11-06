package io.mapsmessaging.network.protocol.impl.extension;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtensionProtocolTest {

  @Test
  void constructorBindsExtensionAndExposesLoopbackIdentity() throws Exception {
    boolean original = JMXManager.isEnableJMX();
    try {
      JMXManager.setEnableJMX(false);
      EndPoint endpoint = endpoint();
      TestExtension extension = new TestExtension();

      ExtensionProtocol protocol = new ExtensionProtocol(endpoint, extension);

      assertSame(protocol, extension.getExtensionProtocol());
      assertEquals("LocalLoop", protocol.getName());
      assertEquals("1.2", protocol.getVersion());
      assertEquals("extension", protocol.getProtocolName());
      assertEquals("loop", protocol.getRemoteIp());
      assertNull(protocol.getSubject());
      assertFalse(protocol.processPacket(new Packet(0, false)));
      assertEquals("TestExtension", protocol.getInformation().getType());
      assertEquals("realm", protocol.getAuthenticationConfig());
      verify(endpoint).setBoundProtocol(protocol);
    } finally {
      JMXManager.setEnableJMX(original);
    }
  }

  private static EndPoint endpoint() {
    EndPoint endpoint = mock(EndPoint.class);
    EndPointServerConfigDTO config = mock(EndPointServerConfigDTO.class);
    when(endpoint.getConfig()).thenReturn(config);
    when(config.getUrl()).thenReturn("extension://localhost/test");
    when(endpoint.getAuthenticationConfig()).thenReturn("realm");
    return endpoint;
  }

  private static final class TestExtension extends Extension {
    @Override
    public void initialise() {}

    @Override
    public String getName() {
      return "TestExtension";
    }

    @Override
    public String getVersion() {
      return "1.2";
    }

    @Override
    public boolean supportsRemoteFiltering() {
      return false;
    }

    @Override
    public void outbound(String destinationName, Message message) {}

    @Override
    public void registerRemoteLink(
        String destination, String filter, Map<String, Object> linkProperties) {}

    @Override
    public void registerLocalLink(
        String destination, Map<String, Object> linkProperties) {}

    @Override
    public void close() throws IOException {
      // avoid delegating back into the protocol during this isolated test
    }
  }
}
