package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MavlinkHeartbeatEmitterTest {

  @Test
  void constructorRejectsMissingRequiredDependencies() {
    MavlinkConfigDTO config = config();

    assertThrows(
        NullPointerException.class,
        () -> new MavlinkHeartbeatEmitter(
            new AtomicInteger(), null, config, null)
    );

    assertThrows(
        NullPointerException.class,
        () -> new MavlinkHeartbeatEmitter(
            new AtomicInteger(), mock(EndPoint.class), null, null)
    );
  }

  @Test
  void runEmitsMavlinkV2HeartbeatAndWrapsSequenceCounter() throws Exception {
    EndPoint endpoint = mock(EndPoint.class);
    when(endpoint.sendPacket(any(Packet.class))).thenReturn(1);

    AtomicInteger sequence = new AtomicInteger(255);
    SocketAddress address = new InetSocketAddress("127.0.0.1", 14550);

    MavlinkHeartbeatEmitter emitter =
        new MavlinkHeartbeatEmitter(sequence, endpoint, config(), address);

    emitter.run();
    emitter.run();

    ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
    verify(endpoint, times(2)).sendPacket(captor.capture());

    Packet first = captor.getAllValues().get(0);
    Packet second = captor.getAllValues().get(1);

    assertSame(address, first.getFromAddress());
    assertSame(address, second.getFromAddress());
    assertTrue(first.available() > 10);
    assertTrue(second.available() > 10);

    assertEquals(0xFD, first.get(0) & 0xff);
    assertEquals(255, first.get(4) & 0xff);
    assertEquals(255, first.get(5) & 0xff);
    assertEquals(190, first.get(6) & 0xff);

    assertEquals(0xFD, second.get(0) & 0xff);
    assertEquals(0, second.get(4) & 0xff);
    assertEquals(1, sequence.get());
  }

  @Test
  void sendFailureIsContainedAndSequenceStillAdvances() throws Exception {
    EndPoint endpoint = mock(EndPoint.class);
    when(endpoint.getName()).thenReturn("test-endpoint");
    doThrow(new java.io.IOException("send failed"))
        .when(endpoint).sendPacket(any(Packet.class));

    AtomicInteger sequence = new AtomicInteger(7);
    MavlinkHeartbeatEmitter emitter =
        new MavlinkHeartbeatEmitter(sequence, endpoint, config(), null);

    assertDoesNotThrow(emitter::run);
    assertEquals(8, sequence.get());
  }

  private static MavlinkConfigDTO config() {
    MavlinkConfigDTO config = new MavlinkConfigDTO();
    config.setDialectName("common");
    config.setSystemId(255);
    config.setComponentId(190);
    return config;
  }
}
