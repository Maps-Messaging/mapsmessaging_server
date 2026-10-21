package io.mapsmessaging.network.protocol.impl.extension;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.network.io.EndPointServerStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtensionEndPointTest {

  @Test
  void exposesConfiguredProtocolAndAuthenticationRealm() throws Exception {
    EndPointServerStatus server = mock(EndPointServerStatus.class);
    EndPointServerConfigDTO config = mock(EndPointServerConfigDTO.class);
    ProtocolConfigDTO protocolConfig = new ProtocolConfigDTO("extension");
    when(server.getConfig()).thenReturn(config);
    when(config.getProtocolConfigs()).thenReturn(List.of(protocolConfig));
    when(config.getAuthenticationRealm()).thenReturn("extensions");

    ExtensionEndPoint endpoint = new ExtensionEndPoint(1L, server);
    try {
      assertSame(protocolConfig, endpoint.config());
      assertEquals("extensions", endpoint.getAuthenticationConfig());
      assertEquals("extension", endpoint.getProtocol());
      assertEquals("extension", endpoint.getName());
    } finally {
      endpoint.close();
    }
  }

  @Test
  void missingProtocolConfigurationReturnsNull() throws Exception {
    EndPointServerStatus server = mock(EndPointServerStatus.class);
    EndPointServerConfigDTO config = mock(EndPointServerConfigDTO.class);
    when(server.getConfig()).thenReturn(config);
    when(config.getProtocolConfigs()).thenReturn(null);

    ExtensionEndPoint endpoint = new ExtensionEndPoint(2L, server);
    try {
      assertNull(endpoint.config());
    } finally {
      endpoint.close();
    }
  }
}
