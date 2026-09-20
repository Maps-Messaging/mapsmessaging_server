package io.mapsmessaging.network.protocol.impl.satellite.modem.protocol;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ModemStreamHandlerTest {

  @Test
  void lineModeSkipsBlankLinesAndNormalisesTermination() throws Exception {
    ModemStreamHandler handler = new ModemStreamHandler();
    Packet packet = new Packet(64, false);

    int read = handler.parseInput(
        new ByteArrayInputStream("\r\nREADY\n".getBytes(StandardCharsets.US_ASCII)),
        packet
    );

    assertEquals(7, read);
    packet.flip();
    byte[] bytes = new byte[packet.available()];
    packet.get(bytes);
    assertEquals("READY\r\n", new String(bytes, StandardCharsets.US_ASCII));
  }

  @Test
  void largeOutputIsChunkedWithoutDataLoss() throws Exception {
    ModemStreamHandler handler = new ModemStreamHandler();
    byte[] payload = new byte[25_000];
    for (int i = 0; i < payload.length; i++) {
      payload[i] = (byte) i;
    }
    Packet packet = new Packet(ByteBuffer.wrap(payload));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertEquals(payload.length, handler.parseOutput(output, packet));
    assertArrayEquals(payload, output.toByteArray());
    assertEquals(0, packet.available());
  }

  @Test
  void bypassCannotConsumeInputBeforeOutputStreamIsKnown() {
    ModemStreamHandler handler = new ModemStreamHandler();
    handler.startXModemTransmit(
        new byte[]{1},
        0,
        new CompletableFuture<>()
    );

    IOException failure = assertThrows(
        IOException.class,
        () -> handler.parseInput(
            new ByteArrayInputStream(new byte[0]),
            new Packet(8, false)
        )
    );

    assertTrue(failure.getMessage().contains("output stream"));
  }

  @Test
  void emptyInputProducesNoPacket() throws Exception {
    ModemStreamHandler handler = new ModemStreamHandler();
    assertEquals(
        0,
        handler.parseInput(new ByteArrayInputStream(new byte[0]), new Packet(8, false))
    );
    assertDoesNotThrow(handler::close);
  }
}
