package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AdvertiseTest {

  @Test
  void directConstructorPacksAndRoundTripsAdvertiseFrame() throws Exception {
    Advertise advertise = new Advertise((byte) 7, (short) 300);
    Packet packet = new Packet(8, false);

    assertEquals(5, advertise.packFrame(packet));
    assertEquals(5, packet.position());

    packet.flip();
    assertEquals(5, packet.getByte());
    assertEquals(MQTT_SNPacket.ADVERTISE, packet.getByte());

    Advertise restored = new Advertise(packet, 5);
    assertEquals(7, restored.getGatewayId());
    assertEquals(300, restored.getDuration());
  }

  @Test
  void packetConstructorRejectsUndersizedFrame() {
    Packet packet = new Packet(4, false);

    IOException failure =
        assertThrows(IOException.class, () -> new Advertise(packet, 4));

    assertTrue(failure.getMessage().contains("length"));
  }

  @Test
  void gatewayIdPreservesFullWireByte() throws Exception {
    Packet packet = new Packet(3, false);
    packet.put((byte) 0xff);
    packet.putShort(1);
    packet.flip();

    Advertise advertise = new Advertise(packet, 5);

    assertEquals(255, advertise.getGatewayId() & 0xff);
    assertEquals(1, advertise.getDuration());
  }
}
