package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageQueueMalformedFrameTest {

  @Test
  void corruptedCrcIsRejected() throws Exception {
    MessageQueuePacker.Packed packed =
        MessageQueuePacker.pack(
            Map.of("/topic", List.of("data".getBytes(StandardCharsets.UTF_8))),
            Integer.MAX_VALUE,
            null,
            null);
    byte[] corrupted = packed.data().clone();
    corrupted[1] ^= 0x01;

    IOException exception =
        assertThrows(IOException.class,
            () -> MessageQueueUnpacker.unpack(corrupted, packed.compressed(), null));

    assertTrue(exception.getMessage().contains("crc"));
  }

  @Test
  void truncatedFrameFailsClosedWithoutLeakingRuntimeException() throws Exception {
    Map<String, List<byte[]>> restored =
        MessageQueueUnpacker.unpack(new byte[]{1, 2, 3}, false, null);

    assertTrue(restored.isEmpty());
  }

  @Test
  void wrongDecryptionKeyIsReportedAsIoFailure() throws Exception {
    CipherManager sender = new CipherManager("sender".getBytes(StandardCharsets.UTF_8));
    CipherManager receiver = new CipherManager("receiver".getBytes(StandardCharsets.UTF_8));
    MessageQueuePacker.Packed packed =
        MessageQueuePacker.pack(
            Map.of("/topic", List.of(new byte[]{1, 2, 3})),
            Integer.MAX_VALUE,
            sender,
            null);

    assertThrows(
        IOException.class,
        () -> MessageQueueUnpacker.unpack(packed.data(), packed.compressed(), receiver));
  }
}