package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PingResp5Test {

  @Test
  void validatesMqtt5PingResponseHeader() throws Exception {
    assertDoesNotThrow(() -> new PingResp5((byte) 0xD0, 0));
    assertThrows(MalformedException.class, () -> new PingResp5((byte) 0xD1, 0));
    assertThrows(MalformedException.class, () -> new PingResp5((byte) 0xD0, 1));
  }

  @Test
  void packsCanonicalMqtt5PingResponse() {
    Packet packet = new Packet(4, false);

    assertEquals(2, new PingResp5().packFrame(packet));
    packet.flip();

    assertEquals(0xD0, packet.getByte());
    assertEquals(0, packet.getByte());
    assertEquals("MQTTv5 PingResp[]", new PingResp5().toString());
  }
}
