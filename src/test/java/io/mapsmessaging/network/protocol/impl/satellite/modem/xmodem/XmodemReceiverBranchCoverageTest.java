package io.mapsmessaging.network.protocol.impl.satellite.modem.xmodem;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class XmodemReceiverBranchCoverageTest {

  @Test
  void earlyEotDuringHandshakeCompletesEmptyTransfer() throws Exception {
    ByteArrayOutputStream linkOut = new ByteArrayOutputStream();
    ByteArrayOutputStream destination = new ByteArrayOutputStream();

    ReceiverResult result = new XmodemReceiver().receive(
        new ByteArrayInputStream(new byte[]{0x04}),
        linkOut,
        destination,
        0,
        -1,
        20);

    assertEquals(0, result.blocks);
    assertEquals(0, result.retries);
    assertArrayEquals(new byte[]{0x43, 0x06}, linkOut.toByteArray());
    assertEquals(0, destination.size());
  }

  @Test
  void invalidBlockComplementRequestsRetryThenRejectsUnexpectedRetryHeader() {
    byte[] wire = new byte[]{0x01, 0x01, 0x01, 0x00};

    IOException failure = assertThrows(
        IOException.class,
        () -> new XmodemReceiver().receive(
            new ByteArrayInputStream(wire),
            new ByteArrayOutputStream(),
            new ByteArrayOutputStream(),
            1,
            -1,
            20));

    assertTrue(failure.getMessage().contains("Expected SOH/STX on retry"));
  }
}