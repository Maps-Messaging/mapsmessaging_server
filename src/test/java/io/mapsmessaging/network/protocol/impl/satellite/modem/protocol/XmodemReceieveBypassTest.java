package io.mapsmessaging.network.protocol.impl.satellite.modem.protocol;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class XmodemReceieveBypassTest {

  @Test
  void parseOutputStreamsLargePacketsInBoundedChunks() throws Exception {
    byte[] data = new byte[20_000];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i & 0xff);
    }

    XmodemReceieveBypass bypass =
        new XmodemReceieveBypass(data.length, -1, 1000, new CompletableFuture<>());
    Packet packet = new Packet(ByteBuffer.wrap(data));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    assertEquals(data.length, bypass.parseOutput(output, packet));
    assertArrayEquals(data, output.toByteArray());
    assertEquals(0, packet.available());
  }

  @Test
  void emptyOutputIsNoOp() throws Exception {
    XmodemReceieveBypass bypass =
        new XmodemReceieveBypass(0, -1, 1000, new CompletableFuture<>());

    assertEquals(0, bypass.parseOutput(new ByteArrayOutputStream(), new Packet(0, false)));
  }

  @Test
  void resetClearsAccumulatedResultAndCompletionState() {
    XmodemReceieveBypass bypass =
        new XmodemReceieveBypass(10, -1, 1000, new CompletableFuture<>());

    assertFalse(bypass.isComplete());
    assertArrayEquals(new byte[0], bypass.result());

    bypass.reset();

    assertFalse(bypass.isComplete());
    assertArrayEquals(new byte[0], bypass.result());
  }
}
