package io.mapsmessaging.network.protocol.impl.nats;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.protocol.impl.nats.frames.FrameFactory;
import io.mapsmessaging.network.protocol.impl.nats.frames.InfoFrame;
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
  void outboundInitialInfoUsesRunningServerIdentity() {
    UUID uuid = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
    MessageDaemon messageDaemon = mock(MessageDaemon.class);
    when(messageDaemon.getUuid()).thenReturn(uuid);
    when(messageDaemon.getId()).thenReturn("maps-test");

    try (MockedStatic<MessageDaemon> daemon = mockStatic(MessageDaemon.class)) {
      daemon.when(MessageDaemon::getInstance).thenReturn(messageDaemon);

      InfoFrame frame = assertInstanceOf(
          InfoFrame.class,
          new NatsProtocolFactory().getInitialPacket());

      assertEquals(uuid.toString(), frame.getInfoData().getServerId());
      assertEquals("maps-test", frame.getInfoData().getServerName());
    }
  }
}
