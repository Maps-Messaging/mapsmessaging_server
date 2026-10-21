package io.mapsmessaging.network.protocol.impl.rest;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.ExtensionConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestProtocolTest {

  @Test
  void exposesRestExtensionIdentityAndCapabilities() {
    RestProtocol protocol =
        new RestProtocol(endpoint("http://localhost:8080"), new ExtensionConfigDTO());

    assertEquals("RestProtocol", protocol.getName());
    assertEquals("1.0", protocol.getVersion());
    assertFalse(protocol.supportsRemoteFiltering());
  }

  @Test
  void registrationAndOutboundHooksAreSafeNoOps() {
    RestProtocol protocol =
        new RestProtocol(endpoint("http://localhost:8080"), new ExtensionConfigDTO());

    assertDoesNotThrow(protocol::initialise);
    assertDoesNotThrow(() -> protocol.registerRemoteLink("/remote", "x = 1", Map.of()));
    assertDoesNotThrow(() -> protocol.registerLocalLink("/local", Map.of()));
    assertDoesNotThrow(() -> protocol.outbound("/remote", mock(Message.class)));
  }

  @Test
  void closeBeforeProtocolAttachmentIsSafe() {
    RestProtocol protocol =
        new RestProtocol(endpoint("http://localhost:8080"), new ExtensionConfigDTO());

    assertDoesNotThrow(protocol::close);
  }

  private static EndPoint endpoint(String url) {
    EndPoint endpoint = mock(EndPoint.class);
    EndPointServerConfigDTO config = mock(EndPointServerConfigDTO.class);
    when(endpoint.getConfig()).thenReturn(config);
    when(config.getUrl()).thenReturn(url);
    return endpoint;
  }
}
