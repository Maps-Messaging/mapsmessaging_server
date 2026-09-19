package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GatewayInfoTest {

  @Test
  void constructorReadsGatewayIdAndRemainingAddressBytes() {
    Packet packet = new Packet(4, false);
    packet.put((byte) 7);
    packet.put(new byte[]{1, 2, 3});
    packet.flip();

    GatewayInfo info = new GatewayInfo(packet, 6);

    assertEquals(7, info.getGatewayId());
    assertArrayEquals(new byte[]{1, 2, 3}, info.getGatewayAddress());
  }

  @Test
  void packedFrameRoundTripsThroughWireRepresentation() {
    Packet source = new Packet(4, false);
    source.put((byte) 9);
    source.put(new byte[]{10, 20, 30});
    source.flip();
    GatewayInfo info = new GatewayInfo(source, 6);

    Packet packed = new Packet(16, false);
    assertEquals(6, info.packFrame(packed));
    assertEquals(6, packed.position());

    packed.flip();
    assertEquals(6, packed.getByte());
    assertEquals(MQTT_SN_2_Packet.GWINFO, packed.getByte());

    GatewayInfo restored = new GatewayInfo(packed, 6);
    assertEquals(9, restored.getGatewayId());
    assertArrayEquals(new byte[]{10, 20, 30}, restored.getGatewayAddress());
  }

  @Test
  void minimumLengthFrameHasEmptyAddress() {
    Packet packet = new Packet(1, false);
    packet.put((byte) 1);
    packet.flip();

    GatewayInfo info = new GatewayInfo(packet, 3);

    assertEquals(1, info.getGatewayId());
    assertArrayEquals(new byte[0], info.getGatewayAddress());

    Packet packed = new Packet(8, false);
    assertEquals(3, info.packFrame(packed));
  }
}
