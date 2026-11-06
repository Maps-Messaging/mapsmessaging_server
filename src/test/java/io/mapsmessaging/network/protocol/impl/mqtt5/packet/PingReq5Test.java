package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PingReq5Test {

  @Test
  void validatesMqtt5PingRequestHeader() throws Exception {
    assertDoesNotThrow(() -> new PingReq5((byte) 0xC0, 0));
    assertThrows(MalformedException.class, () -> new PingReq5((byte) 0xC1, 0));
    assertThrows(MalformedException.class, () -> new PingReq5((byte) 0xC0, 1));
  }

  @Test
  void packsCanonicalMqtt5PingRequest() {
    Packet packet = new Packet(4, false);

    assertEquals(2, new PingReq5().packFrame(packet));
    packet.flip();

    assertEquals(0xC0, packet.getByte());
    assertEquals(0, packet.getByte());
    assertEquals("MQTTv5 PingReq[]", new PingReq5().toString());
  }
}
