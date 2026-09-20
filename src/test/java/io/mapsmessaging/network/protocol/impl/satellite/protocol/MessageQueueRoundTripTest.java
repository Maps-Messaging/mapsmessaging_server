package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageQueueRoundTripTest {

  @Test
  void uncompressedRoundTripPreservesMessagesAndUsesDeterministicNamespaceOrder() throws Exception {
    Map<String, List<byte[]>> input = new LinkedHashMap<>();
    input.put("/z", List.of("last".getBytes(StandardCharsets.UTF_8)));
    input.put("/a", List.of("first".getBytes(StandardCharsets.UTF_8), new byte[0]));

    MessageQueuePacker.Packed packed =
        MessageQueuePacker.pack(input, Integer.MAX_VALUE, null, null);
    Map<String, List<byte[]>> restored =
        MessageQueueUnpacker.unpack(packed.data(), packed.compressed(), null);

    assertFalse(packed.compressed());
    assertEquals(List.of("/a", "/z"), restored.keySet().stream().toList());
    assertArrayEquals("first".getBytes(StandardCharsets.UTF_8), restored.get("/a").get(0));
    assertArrayEquals(new byte[0], restored.get("/a").get(1));
    assertArrayEquals("last".getBytes(StandardCharsets.UTF_8), restored.get("/z").getFirst());
    assertEquals(0, packed.transformerNumber());
  }

  @Test
  void compressedEncryptedRoundTripRestoresOriginalPayload() throws Exception {
    byte[] repeated = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        .getBytes(StandardCharsets.UTF_8);
    Map<String, List<byte[]>> input = Map.of("/bulk", List.of(repeated, repeated));
    CipherManager cipher = new CipherManager("queue-key".getBytes(StandardCharsets.UTF_8));

    MessageQueuePacker.Packed packed =
        MessageQueuePacker.pack(input, 1, cipher, null);
    Map<String, List<byte[]>> restored =
        MessageQueueUnpacker.unpack(packed.data(), packed.compressed(), cipher);

    assertTrue(packed.compressed());
    assertEquals(2, restored.get("/bulk").size());
    assertArrayEquals(repeated, restored.get("/bulk").get(0));
    assertArrayEquals(repeated, restored.get("/bulk").get(1));
  }
}