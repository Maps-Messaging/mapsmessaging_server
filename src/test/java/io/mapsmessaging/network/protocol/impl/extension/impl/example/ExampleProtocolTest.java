package io.mapsmessaging.network.protocol.impl.extension.impl.example;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.dto.rest.config.protocol.impl.ExtensionConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.extension.ExtensionProtocol;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExampleProtocolTest {

  @Test
  void exposesExampleProtocolContractAndLifecycle() throws Exception {
    TestExampleProtocol protocol =
        new TestExampleProtocol(mock(EndPoint.class), new ExtensionConfigDTO());

    assertEquals("Example", protocol.getName());
    assertEquals("1.0", protocol.getVersion());
    assertFalse(protocol.supportsRemoteFiltering());
    assertDoesNotThrow(protocol::initializeExtension);
    assertThrows(IllegalStateException.class, protocol::initializeExtension);
    assertDoesNotThrow(() -> protocol.registerRemoteLink("/remote", null, Map.of()));
    assertDoesNotThrow(() -> protocol.registerLocalLink("/local", Map.of()));
  }

  @Test
  void outboundEchoRequiresAttachedExtensionProtocol() {
    TestExampleProtocol protocol =
        new TestExampleProtocol(mock(EndPoint.class), new ExtensionConfigDTO());

    assertThrows(
        IllegalStateException.class,
        () -> protocol.outbound(
            "/echo",
            new MessageBuilder().setOpaqueData(new byte[]{1}).build()
        )
    );

    protocol.attach(mock(ExtensionProtocol.class));
    assertDoesNotThrow(
        () -> protocol.outbound(
            "/echo",
            new MessageBuilder().setOpaqueData(new byte[]{1}).build()
        )
    );
  }

  private static final class TestExampleProtocol extends ExampleProtocol {
    TestExampleProtocol(EndPoint endPoint, ExtensionConfigDTO config) {
      super(endPoint, config);
    }

    void attach(ExtensionProtocol protocol) {
      setExtensionProtocol(protocol);
    }
  }
}
