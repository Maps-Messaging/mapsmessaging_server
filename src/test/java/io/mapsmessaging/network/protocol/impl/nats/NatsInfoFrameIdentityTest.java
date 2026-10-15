package io.mapsmessaging.network.protocol.impl.nats;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.NatsConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.nats.frames.FrameFactory;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.nats.frames.InfoFrame;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NatsInfoFrameIdentityTest {

  @Test
  void frameFactoryConstructionDoesNotRequireMessageDaemon() {
    try (MockedStatic<MessageDaemon> daemon = mockStatic(MessageDaemon.class)) {
      daemon.when(MessageDaemon::getInstance)
          .thenThrow(new AssertionError("Frame parser must not request server identity"));

      assertDoesNotThrow(() -> new FrameFactory(4096, false));
      daemon.verifyNoInteractions();
    }
  }


  @Test
  void infoFrameUsesSingleLineCompactJsonOnWire() {
    InfoFrame frame = new InfoFrame(1024, "server-id", "server-name");
    Packet packet = new Packet(2048, false);

    frame.packFrame(packet);
    packet.flip();
    byte[] bytes = new byte[packet.available()];
    packet.get(bytes);
    String wire = new String(bytes, StandardCharsets.US_ASCII);

    assertTrue(wire.startsWith("INFO {"));
    assertTrue(wire.endsWith("\r\n"));
    assertFalse(wire.substring(0, wire.length() - 2).contains("\r"));
    assertFalse(wire.substring(0, wire.length() - 2).contains("\n"));
  }

  @Test
  void outboundInitialInfoUsesRunningServerIdentity() {
    UUID uuid = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
    MessageDaemon messageDaemon = mock(MessageDaemon.class);
    when(messageDaemon.getUuid()).thenReturn(uuid);
    when(messageDaemon.getId()).thenReturn("maps-test");

    try (MockedStatic<MessageDaemon> daemon = mockStatic(MessageDaemon.class)) {
      daemon.when(MessageDaemon::getInstance).thenReturn(messageDaemon);

      EndPoint endPoint = mock(EndPoint.class);
      EndPointServerConfigDTO endPointConfig = mock(EndPointServerConfigDTO.class);
      NatsConfigDTO natsConfig = new NatsConfigDTO();
      natsConfig.setMaxBufferSize(123456);
      when(endPoint.getConfig()).thenReturn(endPointConfig);
      when(endPointConfig.getProtocolConfig("nats")).thenReturn(natsConfig);

      InfoFrame frame = assertInstanceOf(
          InfoFrame.class,
          new NatsProtocolFactory().getInitialPacket(endPoint));

      assertEquals(uuid.toString(), frame.getInfoData().getServerId());
      assertEquals("maps-test", frame.getInfoData().getServerName());
      assertEquals(123456, frame.getInfoData().getMaxPayloadLength());
    }
  }
}
