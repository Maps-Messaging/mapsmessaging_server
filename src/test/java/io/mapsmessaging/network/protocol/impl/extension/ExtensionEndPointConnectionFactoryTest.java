package io.mapsmessaging.network.protocol.impl.extension;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectedCallback;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtensionEndPointConnectionFactoryTest {

  @Test
  void connectCreatesEndpointAndNotifiesCallbackAndServer() throws Exception {
    ExtensionEndPointConnectionFactory factory = new ExtensionEndPointConnectionFactory();
    EndPointConnectedCallback callback = mock(EndPointConnectedCallback.class);
    EndPointServerStatus server = mock(EndPointServerStatus.class);

    EndPoint endpoint = factory.connect(
        new EndPointURL("extension://local"),
        mock(SelectorLoadManager.class),
        callback,
        server,
        List.of("test")
    );

    assertInstanceOf(ExtensionEndPoint.class, endpoint);
    assertEquals("extension", endpoint.getProtocol());
    verify(callback).connected(endpoint);
    verify(server).handleNewEndPoint(endpoint);
  }

  @Test
  void factoryMetadataIdentifiesDummyExtensionTransport() {
    ExtensionEndPointConnectionFactory factory = new ExtensionEndPointConnectionFactory();

    assertEquals("extension", factory.getName());
    assertTrue(factory.getDescription().toLowerCase().contains("extension"));
  }
}
