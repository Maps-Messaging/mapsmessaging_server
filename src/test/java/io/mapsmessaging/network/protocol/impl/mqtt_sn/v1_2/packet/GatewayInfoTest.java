package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayInfoTest {

  @Test
  void directConstructorAndPackedFramePreserveGatewayId() {
    GatewayInfo info = new GatewayInfo((short) 200);
    Packet packet = new Packet(8, false);

    assertEquals(3, info.packFrame(packet));
    assertEquals(3, packet.position());

    packet.flip();
    assertEquals(3, packet.getByte());
    assertEquals(MQTT_SNPacket.GWINFO, packet.getByte());

    GatewayInfo restored = new GatewayInfo(packet);
    assertEquals(200, restored.getGatewayId() & 0xff);
  }

  @Test
  void packetConstructorTreatsGatewayIdAsUnsignedWireByte() {
    Packet packet = new Packet(1, false);
    packet.put((byte) 0xff);
    packet.flip();

    GatewayInfo info = new GatewayInfo(packet);

    assertEquals(255, info.getGatewayId() & 0xff);
  }
}
