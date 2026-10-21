package io.mapsmessaging.network.protocol.impl.satellite.modem.xmodem;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class XmodemReceiverTest {

  @Test
  void receivesSingleBlockAndTruncatesPaddingToAnnouncedLength() throws Exception {
    byte[] payload = new byte[]{1, 2, 3};
    byte[] transfer = oneBlockTransfer(payload);
    ByteArrayOutputStream linkOut = new ByteArrayOutputStream();
    ByteArrayOutputStream destination = new ByteArrayOutputStream();

    ReceiverResult result = new XmodemReceiver().receive(
        new ByteArrayInputStream(transfer),
        linkOut,
        destination,
        payload.length,
        Xmodem.crc32Mpeg2(payload, payload.length),
        20
    );

    assertEquals(1, result.blocks);
    assertEquals(0, result.retries);
    assertArrayEquals(payload, destination.toByteArray());
    assertArrayEquals(new byte[]{0x43, 0x06, 0x06}, linkOut.toByteArray());
  }

  @Test
  void senderCancellationDuringHandshakeIsRejected() {
    IOException failure = assertThrows(
        IOException.class,
        () -> new XmodemReceiver().receive(
            new ByteArrayInputStream(new byte[]{0x18}),
            new ByteArrayOutputStream(),
            new ByteArrayOutputStream(),
            1,
            -1,
            20
        )
    );

    assertTrue(failure.getMessage().contains("cancelled"));
  }

  @Test
  void expectedCrc32MismatchIsReportedAfterTransfer() {
    IOException failure = assertThrows(
        IOException.class,
        () -> new XmodemReceiver().receive(
            new ByteArrayInputStream(oneBlockTransfer(new byte[]{9, 8})),
            new ByteArrayOutputStream(),
            new ByteArrayOutputStream(),
            2,
            0L,
            20
        )
    );

    assertTrue(failure.getMessage().contains("CRC32 mismatch"));
  }

  private static byte[] oneBlockTransfer(byte[] payload) throws Exception {
    byte[] block = new byte[128];
    System.arraycopy(payload, 0, block, 0, payload.length);
    for (int i = payload.length; i < block.length; i++) {
      block[i] = 0x1a;
    }
    int crc = new CrcHelper().crc(block);

    ByteArrayOutputStream wire = new ByteArrayOutputStream();
    wire.write(0x01);
    wire.write(0x01);
    wire.write(0xfe);
    wire.write(block);
    wire.write((crc >>> 8) & 0xff);
    wire.write(crc & 0xff);
    wire.write(0x04);
    return wire.toByteArray();
  }

  private static final class CrcHelper extends Xmodem {
    int crc(byte[] data) {
      return crc16Xmodem(data, 0, data.length);
    }
  }
}
