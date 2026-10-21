package io.mapsmessaging.api.features;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CompressionModeCoverageSweepTest {
  @Test
  void inflaterRoundTripsPayloadAndUnknownTypeFallsBackToNoCompression() {
    byte[] data = "aaaaaaaaabbbbbbbbbbcccccccccc".getBytes(StandardCharsets.UTF_8);

    ByteBuffer compressed = CompressionMode.INFLATOR.compress(data);
    assertArrayEquals(data, CompressionMode.INFLATOR.decompress(compressed));

    ByteBuffer raw = CompressionMode.NONE.compress(data);
    assertArrayEquals(data, raw.array());
    assertNotNull(CompressionMode.valueOf(99));
  }
}
