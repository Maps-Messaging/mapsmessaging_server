package io.mapsmessaging.network.protocol.impl.satellite.modem.xmodem;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class XmodemSenderTest {

  @Test
  void sendsSingleBlockAndEotAfterCrcHandshake() throws Exception {
    XmodemSender sender = new XmodemSender();
    ByteArrayOutputStream linkOut = new ByteArrayOutputStream();

    SenderResult result = sender.send(
        new ByteArrayInputStream(new byte[]{1, 2, 3}),
        linkOut,
        new ByteArrayInputStream(new byte[]{0x43, 0x06, 0x06}),
        3,
        20,
        0x12345678L
    );

    byte[] wire = linkOut.toByteArray();
    assertEquals(1, result.blocks);
    assertEquals(0, result.retries);
    assertEquals(134, result.bytesOnWire);
    assertEquals(0x12345678L, result.crc32Mpeg2);
    assertEquals(0x01, wire[0] & 0xff);
    assertEquals(1, wire[1] & 0xff);
    assertEquals(0xfe, wire[2] & 0xff);
    assertEquals(0x04, wire[wire.length - 1] & 0xff);
  }

  @Test
  void nakRetransmitsCurrentBlockAndCountsRetry() throws Exception {
    XmodemSender sender = new XmodemSender();
    ByteArrayOutputStream linkOut = new ByteArrayOutputStream();

    SenderResult result = sender.send(
        new ByteArrayInputStream(new byte[]{7}),
        linkOut,
        new ByteArrayInputStream(new byte[]{0x43, 0x15, 0x06, 0x06}),
        1,
        20,
        0L
    );

    assertEquals(1, result.blocks);
    assertEquals(1, result.retries);
    assertEquals(267, result.bytesOnWire);
  }

  @Test
  void receiverCancellationDuringHandshakeIsRejected() {
    XmodemSender sender = new XmodemSender();

    IOException failure = assertThrows(
        IOException.class,
        () -> sender.send(
            new ByteArrayInputStream(new byte[]{1}),
            new ByteArrayOutputStream(),
            new ByteArrayInputStream(new byte[]{0x18}),
            1,
            20,
            0L
        )
    );

    assertTrue(failure.getMessage().contains("cancelled"));
  }

  @Test
  void crcComputationRequiresMarkSupportWhenNoPrecomputedValueProvided() {
    InputStream nonMarkable = new InputStream() {
      private boolean read;

      @Override
      public int read() {
        if (read) {
          return -1;
        }
        read = true;
        return 1;
      }

      @Override
      public boolean markSupported() {
        return false;
      }
    };

    IOException failure = assertThrows(
        IOException.class,
        () -> new XmodemSender().send(
            nonMarkable,
            new ByteArrayOutputStream(),
            new ByteArrayInputStream(new byte[]{0x43}),
            1,
            20
        )
    );

    assertTrue(failure.getMessage().contains("mark/reset"));
  }
}
