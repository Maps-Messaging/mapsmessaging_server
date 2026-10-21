package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class PatchTest {

  @Test
  void constructorParsesCommonCoapHeaderAndUsesPatchPacketId() {
    Patch packet = new Patch(wire(PacketFactory.PATCH));

    assertEquals(PacketFactory.PATCH, packet.getId());
    assertEquals(1, packet.getVersion());
    assertEquals(TYPE.CON, packet.getType());
    assertEquals(0x1234, packet.getMessageId());
    assertEquals(0, packet.getToken().length);
    assertNotNull(packet.toString());
  }

  private static Packet wire(int code) {
    return new Packet(ByteBuffer.wrap(new byte[]{0x40, (byte) code, 0x12, 0x34}));
  }
}