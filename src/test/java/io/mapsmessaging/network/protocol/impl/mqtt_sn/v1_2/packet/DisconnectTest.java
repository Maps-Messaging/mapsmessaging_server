package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisconnectTest {

  @Test
  void defaultDisconnectHasNoDurationAndPacksTwoByteFrame() {
    Disconnect disconnect = new Disconnect();
    Packet packet = new Packet(8, false);

    assertEquals(0, disconnect.getDuration());
    assertEquals(2, disconnect.packFrame(packet));

    packet.flip();
    assertEquals(2, packet.getByte());
    assertEquals(MQTT_SNPacket.DISCONNECT, packet.getByte());
  }

  @Test
  void parserReadsOptionalDurationWhenLengthIncludesIt() {
    Packet packet = new Packet(2, false);
    packet.putShort(30);
    packet.flip();

    Disconnect disconnect = new Disconnect(packet, 4);

    assertEquals(30, disconnect.getDuration());
  }

  @Test
  void shortFrameLeavesDurationAtZeroWithoutReadingPayload() {
    Packet packet = new Packet(0, false);

    Disconnect disconnect = new Disconnect(packet, 2);

    assertEquals(0, disconnect.getDuration());
  }
}
