package io.mapsmessaging.network.protocol.impl.satellite.modem.protocol;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class XmodemSendBypassTest {

  @Test
  void nullDataIsRejectedAtConstruction() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new XmodemSendBypass(null, 20, new CompletableFuture<>(), 0)
    );
  }

  @Test
  void parseInputPerformsSingleTransferAndCompletesFuture() throws Exception {
    byte[] data = new byte[]{1, 2, 3};
    CompletableFuture<byte[]> future = new CompletableFuture<>();
    XmodemSendBypass bypass =
        new XmodemSendBypass(data, 20, future, 0x12345678L);
    ByteArrayInputStream linkIn =
        new ByteArrayInputStream(new byte[]{0x43, 0x06, 0x06});
    ByteArrayOutputStream linkOut = new ByteArrayOutputStream();

    assertEquals(0, bypass.parseInput(linkIn, linkOut));
    assertTrue(bypass.isComplete());
    assertSame(data, bypass.result());
    assertArrayEquals(data, future.join());
    assertEquals(134, linkOut.size());

    int sizeAfterFirstSend = linkOut.size();
    assertEquals(0, bypass.parseInput(linkIn, linkOut));
    assertEquals(sizeAfterFirstSend, linkOut.size());
  }

  @Test
  void resetAllowsTransferStateToStartAgain() throws Exception {
    XmodemSendBypass bypass =
        new XmodemSendBypass(new byte[]{9}, 20, null, 0L);

    bypass.parseInput(
        new ByteArrayInputStream(new byte[]{0x43, 0x06, 0x06}),
        new ByteArrayOutputStream()
    );
    assertTrue(bypass.isComplete());

    bypass.reset();

    assertFalse(bypass.isComplete());
    ByteArrayOutputStream second = new ByteArrayOutputStream();
    bypass.parseInput(
        new ByteArrayInputStream(new byte[]{0x43, 0x06, 0x06}),
        second
    );
    assertTrue(bypass.isComplete());
    assertEquals(134, second.size());
  }

  @Test
  void parseOutputIsNoOpAndDoesNotConsumePacket() {
    XmodemSendBypass bypass =
        new XmodemSendBypass(new byte[]{1}, 20, null, 0L);
    Packet packet = new Packet(ByteBuffer.wrap(new byte[]{5, 6, 7}));

    assertEquals(0, bypass.parseOutput(new ByteArrayOutputStream(), packet));
    assertEquals(3, packet.available());
  }
}
