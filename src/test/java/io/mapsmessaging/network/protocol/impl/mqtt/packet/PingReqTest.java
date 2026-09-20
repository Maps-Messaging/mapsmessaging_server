package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PingReqTest {

  @Test
  void validWireHeaderIsAccepted() throws Exception {
    assertDoesNotThrow(() -> new PingReq((byte) 0xC0, 0));
  }

  @Test
  void reservedBitsOrPayloadLengthAreRejected() {
    assertThrows(MalformedException.class, () -> new PingReq((byte) 0xC1, 0));
    assertThrows(MalformedException.class, () -> new PingReq((byte) 0xC0, 1));
  }

  @Test
  void packProducesCanonicalTwoByteFrame() {
    Packet packet = new Packet(4, false);

    assertEquals(2, new PingReq().packFrame(packet));
    packet.flip();

    assertEquals(0xC0, packet.getByte());
    assertEquals(0, packet.getByte());
    assertEquals("MQTT PingReq[]", new PingReq().toString());
  }
}
