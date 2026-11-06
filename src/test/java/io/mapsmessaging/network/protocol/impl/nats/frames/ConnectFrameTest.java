package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ConnectFrameTest {

  @Test
  void packedConnectIncludesConfiguredFlagsAndCredentials() {
    ConnectFrame frame = new ConnectFrame();
    frame.setVerbose(true);
    frame.setPedantic(true);
    frame.setTlsRequired(true);
    frame.setUser("alice");
    frame.setPass("secret");

    Packet packet = new Packet(256, false);
    int written = frame.packFrame(packet);

    assertEquals(packet.position(), written);
    packet.flip();
    byte[] bytes = new byte[packet.available()];
    packet.get(bytes);
    String wire = new String(bytes, StandardCharsets.US_ASCII);

    assertTrue(wire.startsWith("CONNECT {"));
    assertTrue(wire.contains("\"verbose\":true"));
    assertTrue(wire.contains("\"pedantic\":true"));
    assertTrue(wire.contains("\"tls_required\":true"));
    assertTrue(wire.contains("\"user\":\"alice\""));
    assertTrue(wire.contains("\"pass\":\"secret\""));
    assertTrue(wire.endsWith("\r\n"));
  }

  @Test
  void parserReadsSupportedBooleanAndCredentialFields() throws Exception {
    String line =
        "{\"echo\":true,\"headers\":true,\"verbose\":false,"
            + "\"pedantic\":true,\"tls_required\":true,"
            + "\"user\":\"bob\",\"pass\":\"pw\"}\r\n";

    ConnectFrame frame = new ConnectFrame();
    frame.parseFrame(
        new Packet(ByteBuffer.wrap(line.getBytes(StandardCharsets.US_ASCII)))
    );

    assertTrue(frame.isEcho());
    assertTrue(frame.isHeaders());
    assertFalse(frame.isVerbose());
    assertTrue(frame.isPedantic());
    assertTrue(frame.isTlsRequired());
    assertEquals("bob", frame.getUser());
    assertEquals("pw", frame.getPass());
  }

  @Test
  void absentCredentialsAreNotFabricatedAndInstanceIsValid() {
    ConnectFrame frame = new ConnectFrame();

    assertNull(frame.getUser());
    assertNull(frame.getPass());
    assertTrue(frame.isValid());
    assertInstanceOf(ConnectFrame.class, frame.instance());
  }
}
