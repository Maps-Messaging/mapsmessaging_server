package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchGatewayTest {

  @Test
  void directConstructorAndPackedFramePreserveRadius() {
    SearchGateway search = new SearchGateway((short) 12);
    Packet packet = new Packet(8, false);

    assertEquals(3, search.packFrame(packet));

    packet.flip();
    assertEquals(3, packet.getByte());
    assertEquals(MQTT_SNPacket.SEARCHGW, packet.getByte());

    SearchGateway restored = new SearchGateway(packet);
    assertEquals(12, restored.getRadius());
  }

  @Test
  void packetConstructorPreservesFullUnsignedRadiusByte() {
    Packet packet = new Packet(1, false);
    packet.put((byte) 0xfe);
    packet.flip();

    SearchGateway search = new SearchGateway(packet);

    assertEquals(254, search.getRadius() & 0xff);
  }
}
