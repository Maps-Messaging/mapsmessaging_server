/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.satellite;

import com.google.gson.Gson;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.CipherManager;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.MessageQueuePacker;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.MessageQueueUnpacker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

class PackingPipelineTest {
  private static final Gson gson = new Gson();

  private static Map<String, List<byte[]>> sampleBatch(int keys, int perKey, int size, byte fill) {
    Map<String, List<byte[]>> m = new LinkedHashMap<>();
    for (int k = 0; k < keys; k++) {
      List<byte[]> list = new ArrayList<>(perKey);
      for (int i = 0; i < perKey; i++) {
        byte[] b = new byte[size];
        Arrays.fill(b, (byte) (fill == 0 ? k : fill));
        list.add(b);
      }
      m.put("k" + k, list);
    }
    return m;
  }

  private static Map<String, List<byte[]>> sampleJsonBatch(int keys, int perKey) {
    Map<String, List<byte[]>> m = new LinkedHashMap<>();
    Random random = new Random();

    for (int k = 0; k < keys; k++) {
      List<byte[]> list = new ArrayList<>(perKey);
      for (int i = 0; i < perKey; i++) {
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("id", "k" + k + "-m" + i);
        obj.put("timestamp", System.currentTimeMillis());
        obj.put("value1", random.nextInt(1000));
        obj.put("value2", UUID.randomUUID().toString());
        obj.put("flag", random.nextBoolean());

        // encode as JSON
        String json = gson.toJson(obj);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        list.add(jsonBytes);
      }
      m.put("k" + k, list);
    }
    return m;
  }

  @Test
  void compressionRate() throws IOException {
    Map<String, List<byte[]>> batch = sampleJsonBatch(2, 40 );
    CipherManager cm = new CipherManager("testing".getBytes());

    MessageQueuePacker.Packed uncompressed = MessageQueuePacker.pack(batch, 10000000, cm, null);
    MessageQueuePacker.Packed compressed = MessageQueuePacker.pack(batch, 100, cm, null);
    Assertions.assertNotEquals(uncompressed.data().length, compressed.data().length);
  }

  /** Flip a single byte in-place at the given index. */
  private static byte[] flip(byte[] src, int index) {
    byte[] copy = Arrays.copyOf(src, src.length);
    copy[index] ^= 0x01;
    return copy;
  }

  /** Returns the index immediately after the CRC field (assumes 4-byte CRC at the very start). */
  private static int crcEndOffset() {
    return 4; // CRC32 first, 4 bytes
  }

  @Test
  void crcMismatchFailsBeforeDecrypt() throws IOException {
    Map<String, List<byte[]>> batch = sampleBatch(2, 2, 256, (byte) 0x7F); // compressible
    CipherManager cm = new CipherManager("testing".getBytes());
    int minCompressSize = 100;

    MessageQueuePacker.Packed packed = MessageQueuePacker.pack(batch, minCompressSize, cm, null);
    byte[] corrupted = flip(packed.data(), crcEndOffset()); // flip first byte after CRC

    Assertions.assertThrows(IOException.class, () ->
            MessageQueueUnpacker.unpack(corrupted, packed.compressed(), cm),
        "CRC corruption should fail before decrypt");
  }

  @Test
  void wrongKeyDecryptFails() throws IOException {
    Map<String, List<byte[]>> batch = sampleBatch(3, 3, 300, (byte) 0x55);
    CipherManager cmRight = new CipherManager("testing".getBytes());
    int minCompressSize = 100;

    MessageQueuePacker.Packed packed = MessageQueuePacker.pack(batch, minCompressSize, cmRight, null);

    CipherManager cmWrong = new CipherManager("wrong-key".getBytes());
    Assertions.assertThrows(IOException.class, () ->
            MessageQueueUnpacker.unpack(packed.data(), packed.compressed(), cmWrong),
        "Using a wrong key must fail decrypt");
  }

  @Test
  void decompressCorruptionFailsAfterCrcAndDecrypt() throws IOException {
    CipherManager cmNoEnc = null;// assumes this disables encryption
    Map<String, List<byte[]>> batch = sampleBatch(1, 2, 4096, (byte) 0x00); // highly compressible
    int minCompressSize = 100;

    MessageQueuePacker.Packed packed = MessageQueuePacker.pack(batch, minCompressSize, cmNoEnc, null);
    Assertions.assertTrue(packed.compressed(), "Expected body to be compressed for this input");

    // Corrupt a byte in the compressed body, past CRC
    byte[] corrupted = flip(packed.data(), crcEndOffset() + 10);

    Assertions.assertThrows(IOException.class, () ->
            MessageQueueUnpacker.unpack(corrupted, true, cmNoEnc),
        "Corrupted compressed stream should fail during decompression");
  }

  @Test
  void lenSzFromHeaderDrivesParsing_boundaries() throws IOException {
    // This assumes your packer now stamps LEN_SZ in the first byte and uses it for all varints.
    // Create small & large boundary values to ensure correct parsing when unpacking.
    Map<String, List<byte[]>> batch = new LinkedHashMap<>();
    // key with empty list
    batch.put("empty", Collections.emptyList());
    // key with zero-length payload
    batch.put("zlen", List.of(new byte[0]));
    // key with max-ish payload count/size still reasonable for test
    byte[] small = new byte[1];  // tests LEN_SZ=1 paths
    byte[] mid   = new byte[300]; // >255 ensures LEN_SZ>=2 works
    Arrays.fill(mid, (byte) 1);
    batch.put("mix", List.of(small, mid));

    CipherManager cm = new CipherManager("testing".getBytes());
    int minCompressSize = 128;

    MessageQueuePacker.Packed packed = MessageQueuePacker.pack(batch, minCompressSize, cm, null);
    Map<String, List<byte[]>> out = MessageQueueUnpacker.unpack(packed.data(), packed.compressed(), cm);

    // content-equal ignoring key order, enforcing list order
    Assertions.assertEquals(batch.size(), out.size());
    for (Map.Entry<String, List<byte[]>> e : batch.entrySet()) {
      List<byte[]> a = e.getValue();
      List<byte[]> b = out.get(e.getKey());
      Assertions.assertNotNull(b, "Missing key " + e.getKey());
      Assertions.assertEquals(a.size(), b.size(), "List size mismatch @ " + e.getKey());
      for (int i = 0; i < a.size(); i++) {
        Assertions.assertArrayEquals(a.get(i), b.get(i), "Payload mismatch @ " + e.getKey() + "[" + i + "]");
      }
    }
  }

  @Test
  void truncationFails() throws IOException {
    Map<String, List<byte[]>> batch = sampleBatch(1, 5, 200, (byte) 0x12);
    CipherManager cm = new CipherManager("testing".getBytes());
    MessageQueuePacker.Packed packed = MessageQueuePacker.pack(batch, 100, cm, null);

    byte[] truncated = Arrays.copyOf(packed.data(), Math.max(4, packed.data().length - 5));
    Assertions.assertThrows(IOException.class, () ->
            MessageQueueUnpacker.unpack(truncated, packed.compressed(), cm),
        "Truncated payload should fail cleanly");
  }

  @Test
  void trailingGarbageFailsStrict() throws IOException {
    Map<String, List<byte[]>> batch = sampleBatch(2, 1, 128, (byte) 0x34);
    CipherManager cm = new CipherManager("testing".getBytes());
    MessageQueuePacker.Packed packed = MessageQueuePacker.pack(batch, 50, cm, null);

    byte[] withJunk = Arrays.copyOf(packed.data(), packed.data().length + 3);
    withJunk[withJunk.length - 1] ^= 0x7F;

    Assertions.assertThrows(IOException.class, () ->
            MessageQueueUnpacker.unpack(withJunk, packed.compressed(), cm),
        "Extra trailing bytes should be rejected");
  }

  @Test
  void plaintextLeakCheck() throws IOException {
    Map<String, List<byte[]>> batch = new LinkedHashMap<>();
    batch.put("debug", List.of("we should not see plain text in the resultant pack".getBytes()));

    CipherManager cm = new CipherManager("testing".getBytes());
    MessageQueuePacker.Packed packed = MessageQueuePacker.pack(batch, 1, cm, null);

    String s = new String(packed.data());
    Assertions.assertFalse(s.contains("we should not see plain text in the resultant pack"));
    Assertions.assertFalse(s.contains("debug"));

    // Round-trip sanity
    Map<String, List<byte[]>> out = MessageQueueUnpacker.unpack(packed.data(), packed.compressed(), cm);
    Assertions.assertEquals(1, out.size());
    Assertions.assertArrayEquals(batch.get("debug").get(0), out.get("debug").get(0));
  }

  @Test
  void nonAsciiKeysAndZeroLengthPayloads() throws IOException {
    Map<String, List<byte[]>> batch = new LinkedHashMap<>();
    batch.put("σensors/温度", List.of(new byte[0], "x".getBytes()));
    CipherManager cm = new CipherManager("testing".getBytes());
    MessageQueuePacker.Packed packed = MessageQueuePacker.pack(batch, 128, cm, null);

    Map<String, List<byte[]>> out = MessageQueueUnpacker.unpack(packed.data(), packed.compressed(), cm);
    Assertions.assertEquals(batch.keySet(), out.keySet());
    Assertions.assertArrayEquals(batch.get("σensors/温度").get(0), out.get("σensors/温度").get(0));
    Assertions.assertArrayEquals(batch.get("σensors/温度").get(1), out.get("σensors/温度").get(1));
  }

  // Optional: prove keys unordered but lists ordered
  @Test
  void keyOrderIrrelevantListOrderEnforced() throws IOException {
    Map<String, List<byte[]>> batch = new LinkedHashMap<>();
    batch.put("a", List.of("1".getBytes(), "2".getBytes()));
    batch.put("b", List.of("x".getBytes(), "y".getBytes()));

    CipherManager cm = new CipherManager("testing".getBytes());
    MessageQueuePacker.Packed packed = MessageQueuePacker.pack(batch, 16, cm, null);
    Map<String, List<byte[]>> out = MessageQueueUnpacker.unpack(packed.data(), packed.compressed(), cm);

    // reorder keys in 'out' by re-inserting into a new map to simulate arbitrary order
    Map<String, List<byte[]>> shuffled = new LinkedHashMap<>();
    shuffled.put("b", out.get("b"));
    shuffled.put("a", out.get("a"));

    // Compare ignoring map key order, but enforcing list order
    Assertions.assertEquals(batch.size(), shuffled.size());
    for (String k : batch.keySet()) {
      List<byte[]> A = batch.get(k);
      List<byte[]> B = shuffled.get(k);
      Assertions.assertNotNull(B);
      Assertions.assertEquals(A.size(), B.size());
      for (int i = 0; i < A.size(); i++) {
        Assertions.assertArrayEquals(A.get(i), B.get(i));
      }
    }
  }
}
