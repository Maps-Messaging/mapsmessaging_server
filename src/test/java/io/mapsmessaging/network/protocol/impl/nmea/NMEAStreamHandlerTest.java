package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class NMEAStreamHandlerTest {

  @Test
  void skipsNoiseAndNormalisesLfTerminationToCrLf() throws Exception {
    NMEAStreamHandler handler = new NMEAStreamHandler();
    ByteArrayInputStream input =
        new ByteArrayInputStream("noise$GPGGA,1*00\n".getBytes(StandardCharsets.US_ASCII));
    Packet packet = new Packet(64, false);

    int written = handler.parseInput(input, packet);

    assertEquals(written, packet.position());
    packet.flip();
    byte[] data = new byte[packet.available()];
    packet.get(data);
    assertEquals("$GPGGA,1*00\r\n", new String(data, StandardCharsets.US_ASCII));
  }

  @Test
  void consumesOptionalLfAfterCr() throws Exception {
    NMEAStreamHandler handler = new NMEAStreamHandler();
    ByteArrayInputStream input =
        new ByteArrayInputStream("$ABC\r\nNEXT".getBytes(StandardCharsets.US_ASCII));
    Packet packet = new Packet(32, false);

    handler.parseInput(input, packet);

    assertEquals('N', input.read());
  }

  @Test
  void preservesByteFollowingBareCrWhenStreamSupportsMark() throws Exception {
    NMEAStreamHandler handler = new NMEAStreamHandler();
    ByteArrayInputStream input =
        new ByteArrayInputStream("$ABC\rX".getBytes(StandardCharsets.US_ASCII));
    Packet packet = new Packet(32, false);

    handler.parseInput(input, packet);

    assertEquals('X', input.read());
  }

  @Test
  void rejectsEndOfStreamBeforeStartOrMidSentence() {
    NMEAStreamHandler handler = new NMEAStreamHandler();

    assertThrows(
        IOException.class,
        () -> handler.parseInput(
            new ByteArrayInputStream("noise only".getBytes(StandardCharsets.US_ASCII)),
            new Packet(32, false)
        )
    );

    assertThrows(
        IOException.class,
        () -> handler.parseInput(
            new ByteArrayInputStream("$ABC".getBytes(StandardCharsets.US_ASCII)),
            new Packet(32, false)
        )
    );
  }

  @Test
  void rejectsSentencesThatExceedInternalSafetyBuffer() {
    NMEAStreamHandler handler = new NMEAStreamHandler();
    String oversized = "$" + "A".repeat(1100) + "\n";

    IOException failure = assertThrows(
        IOException.class,
        () -> handler.parseInput(
            new ByteArrayInputStream(oversized.getBytes(StandardCharsets.US_ASCII)),
            new Packet(2048, false)
        )
    );

    assertTrue(failure.getMessage().contains("Exceeded buffer size"));
  }

  @Test
  void outputIsExplicitlyUnsupportedAndCloseIsNoOp() {
    NMEAStreamHandler handler = new NMEAStreamHandler();

    assertEquals(0, handler.parseOutput(new ByteArrayOutputStream(), new Packet(8, false)));
    assertDoesNotThrow(handler::close);
  }
}
