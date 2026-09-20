package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnSubAckTest {

  @Test
  void packsMessageIdAndReasonCodeInNetworkOrder() {
    UnSubAck ack = new UnSubAck(0x1234, ReasonCodes.SUCCESS);
    Packet packet = new Packet(8, false);

    assertEquals(5, ack.packFrame(packet));

    packet.flip();
    assertEquals(5, packet.getByte());
    assertEquals(MQTT_SNPacket.UNSUBACK, packet.getByte());
    assertEquals(0x1234, packet.getShort());
    assertEquals(ReasonCodes.SUCCESS.getValue(), packet.getByte());
  }

  @Test
  void nonSuccessReasonCodeIsPreservedOnWire() {
    Packet packet = new Packet(8, false);
    new UnSubAck(7, ReasonCodes.NO_AUTH).packFrame(packet);

    packet.flip();
    packet.getByte();
    packet.getByte();
    packet.getShort();
    assertEquals(ReasonCodes.NO_AUTH.getValue(), packet.getByte());
  }
}
