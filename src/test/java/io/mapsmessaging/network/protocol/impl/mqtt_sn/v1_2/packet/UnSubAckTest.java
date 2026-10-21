package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnSubAckTest {

  @Test
  void packFrameWritesLengthTypeAndMessageIdInNetworkOrder() {
    UnSubAck ack = new UnSubAck(0x1234);
    Packet packet = new Packet(8, false);

    assertEquals(4, ack.packFrame(packet));

    packet.flip();
    assertEquals(4, packet.getByte());
    assertEquals(MQTT_SNPacket.UNSUBACK, packet.getByte());
    assertEquals(0x1234, packet.getShort());
    assertEquals(0, packet.available());
  }

  @Test
  void messageIdIsMaskedToTheWireWidth() {
    Packet packet = new Packet(8, false);
    new UnSubAck(0x12345).packFrame(packet);

    packet.flip();
    packet.getByte();
    packet.getByte();
    assertEquals(0x2345, packet.getShort());
  }
}
