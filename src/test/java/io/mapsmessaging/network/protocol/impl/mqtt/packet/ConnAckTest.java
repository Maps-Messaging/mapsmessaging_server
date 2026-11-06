package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnAckTest {

  @Test
  void parserReadsSessionPresentAndResponseCode() {
    Packet payload = new Packet(2, false);
    payload.put((byte) 1);
    payload.put(ConnAck.SERVER_UNAVAILABLE);
    payload.flip();

    ConnAck connAck = new ConnAck(payload);

    assertTrue(connAck.isSessionPresent());
    assertEquals(ConnAck.SERVER_UNAVAILABLE, connAck.getResponseCode());
  }

  @Test
  void packReportsAndWritesCompleteFourByteMqttFrame() {
    ConnAck connAck = new ConnAck();
    connAck.setRestoredFlag(true);
    connAck.setResponseCode(ConnAck.SUCCESS);
    Packet packet = new Packet(8, false);

    int written = connAck.packFrame(packet);

    assertEquals(
        4,
        written,
        "packFrame should report the total number of bytes written"
    );
    assertEquals(4, packet.position());

    packet.flip();
    assertEquals(0x20, packet.getByte());
    assertEquals(2, packet.getByte());
    assertEquals(1, packet.getByte());
    assertEquals(ConnAck.SUCCESS, packet.getByte());
  }

  @Test
  void textualResponseCoversDefinedAndUnknownCodes() {
    byte[] codes = {
        ConnAck.SUCCESS,
        ConnAck.INVALID_PROTOCOL,
        ConnAck.IDENTIFIER_REJECTED,
        ConnAck.SERVER_UNAVAILABLE,
        ConnAck.BAD_USERNAME_PASSWORD,
        ConnAck.NOT_AUTHORISED,
        99
    };

    String[] expected = {
        "Success",
        "Invalid Protocol",
        "Identifier Rejected",
        "Server Unavailable",
        "Bad Username or Password",
        "Not Authorized on server",
        "Unknown"
    };

    for (int i = 0; i < codes.length; i++) {
      ConnAck connAck = new ConnAck();
      connAck.setResponseCode(codes[i]);
      assertTrue(connAck.toString().contains(expected[i]));
    }
  }
}
