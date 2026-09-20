package io.mapsmessaging.network.protocol;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VersionedProtocolImplFactoryTest {

  @Test
  void connectRoundRobinsAcrossVersionFactories() throws Exception {
    ProtocolImplFactory first = mock(ProtocolImplFactory.class);
    ProtocolImplFactory second = mock(ProtocolImplFactory.class);
    when(first.getTransportType()).thenReturn("tcp");
    Protocol firstProtocol = mock(Protocol.class);
    Protocol secondProtocol = mock(Protocol.class);
    when(first.connect(any(), anyString(), anyString(), anyString(), anyMap()))
        .thenReturn(firstProtocol);
    when(second.connect(any(), anyString(), anyString(), anyString(), anyMap()))
        .thenReturn(secondProtocol);

    TestFactory factory = new TestFactory(List.of(first, second));
    EndPoint endpoint = mock(EndPoint.class);

    factory.connect(endpoint, "s", "u", "p", Map.of());
    factory.connect(endpoint, "s", "u", "p", Map.of());
    factory.connect(endpoint, "s", "u", "p", Map.of());
    factory.connect(endpoint, "s", "u", "p", Map.of());

    verify(first, times(2)).connect(endpoint, "s", "u", "p", Map.of());
    verify(second, times(2)).connect(endpoint, "s", "u", "p", Map.of());
    assertEquals("tcp", factory.getTransportType());
  }

  @Test
  void inboundCreateIsExplicitlyUnsupportedForInterServerFactory() {
    ProtocolImplFactory delegate = mock(ProtocolImplFactory.class);
    when(delegate.getTransportType()).thenReturn("tcp");
    TestFactory factory = new TestFactory(List.of(delegate));

    IOException failure =
        assertThrows(
            IOException.class,
            () -> factory.create(mock(EndPoint.class), new Packet(1, false))
        );

    assertTrue(failure.getMessage().contains("inter server"));
  }

  private static final class TestFactory extends VersionedProtocolImplFactory {
    TestFactory(List<ProtocolImplFactory> factories) {
      super("versioned", "test", factories);
    }
  }
}
