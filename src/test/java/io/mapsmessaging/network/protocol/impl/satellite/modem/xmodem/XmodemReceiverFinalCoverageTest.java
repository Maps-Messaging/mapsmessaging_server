package io.mapsmessaging.network.protocol.impl.satellite.modem.xmodem;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class XmodemReceiverFinalCoverageTest {

  @Test
  void handshakeFailsAfterMaximumNumberOfNonStartBytes() {
    byte[] junk = new byte[15];

    IOException failure = assertThrows(
        IOException.class,
        () -> new XmodemReceiver().receive(
            new ByteArrayInputStream(junk),
            new ByteArrayOutputStream(),
            new ByteArrayOutputStream(),
            1,
            -1,
            20));

    assertTrue(failure.getMessage().contains("handshake failed"));
  }

  @Test
  void senderCancellationAfterFirstBlockIsReported() throws Exception {
    ByteArrayOutputStream wire = new ByteArrayOutputStream();
    wire.write(block(0x01, 1, new byte[]{7}));
    wire.write(0x18);

    IOException failure = assertThrows(
        IOException.class,
        () -> new XmodemReceiver().receive(
            new ByteArrayInputStream(wire.toByteArray()),
            new ByteArrayOutputStream(),
            new ByteArrayOutputStream(),
            1,
            -1,
            20));

    assertTrue(failure.getMessage().contains("cancelled"));
  }

  @Test
  void oneKilobyteStxBlockIsAcceptedAndTruncatedToAnnouncedLength() throws Exception {
    ByteArrayOutputStream wire = new ByteArrayOutputStream();
    wire.write(block(0x02, 1, new byte[]{1, 2, 3}));
    wire.write(0x04);
    ByteArrayOutputStream destination = new ByteArrayOutputStream();

    ReceiverResult result = new XmodemReceiver().receive(
        new ByteArrayInputStream(wire.toByteArray()),
        new ByteArrayOutputStream(),
        destination,
        3,
        -1,
        20);

    assertEquals(1, result.blocks);
    assertArrayEquals(new byte[]{1, 2, 3}, destination.toByteArray());
  }

  @Test
  void duplicatePreviousBlockIsAckedWithoutAppendingPayloadTwice() throws Exception {
    byte[] firstPayload = new byte[128];
    java.util.Arrays.fill(firstPayload, (byte) 1);

    ByteArrayOutputStream wire = new ByteArrayOutputStream();
    wire.write(block(0x01, 1, firstPayload));
    wire.write(block(0x01, 1, firstPayload));
    wire.write(block(0x01, 2, new byte[]{9}));
    wire.write(0x04);
    ByteArrayOutputStream destination = new ByteArrayOutputStream();

    ReceiverResult result = new XmodemReceiver().receive(
        new ByteArrayInputStream(wire.toByteArray()),
        new ByteArrayOutputStream(),
        destination,
        129,
        -1,
        20);

    assertEquals(2, result.blocks);
    assertEquals(0, result.retries);
    assertEquals(129, destination.size());
    assertEquals(9, destination.toByteArray()[128]);
  }

  private static byte[] block(int header, int number, byte[] payload) throws Exception {
    int size = header == 0x02 ? 1024 : 128;
    byte[] data = new byte[size];
    System.arraycopy(payload, 0, data, 0, Math.min(payload.length, data.length));
    for (int i = Math.min(payload.length, data.length); i < data.length; i++) {
      data[i] = 0x1a;
    }
    int crc = new CrcHelper().crc(data);

    ByteArrayOutputStream block = new ByteArrayOutputStream();
    block.write(header);
    block.write(number & 0xff);
    block.write((0xff - number) & 0xff);
    block.write(data);
    block.write((crc >>> 8) & 0xff);
    block.write(crc & 0xff);
    return block.toByteArray();
  }

  private static final class CrcHelper extends Xmodem {
    int crc(byte[] data) {
      return crc16Xmodem(data, 0, data.length);
    }
  }
}