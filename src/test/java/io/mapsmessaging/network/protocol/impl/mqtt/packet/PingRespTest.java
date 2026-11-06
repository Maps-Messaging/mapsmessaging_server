package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PingRespTest {

  @Test
  void validWireHeaderIsAccepted() throws Exception {
    assertDoesNotThrow(() -> new PingResp((byte) 0xD0, 0));
  }

  @Test
  void reservedBitsOrPayloadLengthAreRejected() {
    assertThrows(MalformedException.class, () -> new PingResp((byte) 0xD2, 0));
    assertThrows(MalformedException.class, () -> new PingResp((byte) 0xD0, 2));
  }

  @Test
  void packProducesCanonicalTwoByteFrame() {
    Packet packet = new Packet(4, false);

    assertEquals(2, new PingResp().packFrame(packet));
    packet.flip();

    assertEquals(0xD0, packet.getByte());
    assertEquals(0, packet.getByte());
    assertEquals("MQTT PingResp[]", new PingResp().toString());
  }
}
