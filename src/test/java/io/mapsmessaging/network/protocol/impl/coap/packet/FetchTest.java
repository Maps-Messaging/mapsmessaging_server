package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class FetchTest {

  @Test
  void constructorParsesCommonCoapHeaderAndUsesFetchPacketId() {
    Fetch packet = new Fetch(wire(PacketFactory.FETCH));

    assertEquals(PacketFactory.FETCH, packet.getId());
    assertEquals(1, packet.getVersion());
    assertEquals(TYPE.CON, packet.getType());
    assertEquals(0x1234, packet.getMessageId());
    assertNotNull(packet.toString());
  }

  private static Packet wire(int code) {
    return new Packet(ByteBuffer.wrap(new byte[]{0x40, (byte) code, 0x12, 0x34}));
  }
}