/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import io.mapsmessaging.network.protocol.impl.satellite.protocol.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

class SatelliteMessageFactoryRebuilderTest {

  private static byte[] payload(int size, byte seed) {
    byte[] p = new byte[size];
    Arrays.fill(p, seed);
    return p;
  }

  private static List<SatelliteMessage> shuffledCopy(List<SatelliteMessage> input) {
    List<SatelliteMessage> shuffled = new ArrayList<>(input);
    Collections.shuffle(shuffled, new Random(1234567L));
    return shuffled;
  }

  @Test
  void splitAndReassemble_inOrder() {
    byte[] src = payload(10_000, (byte) 0x5A);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(src, 1024, true, (byte) 0);
    int streamId = chunks.get(0).getStreamNumber();
    Assertions.assertTrue(chunks.size() >= 10);
    Assertions.assertTrue(chunks.stream().allMatch(SatelliteMessage::isCompressed));

    int totalPackets = chunks.get(0).getTotalPackets();
    Assertions.assertEquals(chunks.size(), totalPackets);
    Assertions.assertTrue(chunks.stream().allMatch(m -> m.getTotalPackets() == totalPackets));

    boolean[] seen = new boolean[totalPackets + 1];
    for (SatelliteMessage m : chunks) {
      int pn = m.getPacketNumber();
      Assertions.assertTrue(pn >= 0 && pn <= totalPackets);
      seen[pn] = true;
    }
    for (int i = 0; i < totalPackets; i++) {
      Assertions.assertTrue(seen[i], "Missing packetNumber=" + i);
    }

    SatelliteMessage combined = SatelliteMessageFactory.reconstructMessage(chunks);
    Assertions.assertNotNull(combined);
    Assertions.assertEquals(streamId, combined.getStreamNumber());
    Assertions.assertTrue(combined.isCompressed());
    Assertions.assertArrayEquals(src, combined.getMessage());
  }

  @Test
  void splitBoundary_exactMultipleChunkSizes() {
    int chunkSize = 512;
    byte[] src = payload(chunkSize * 8, (byte) 0x33);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(src, chunkSize, false, (byte) 0);

    Assertions.assertEquals(8, chunks.size());
    Assertions.assertFalse(chunks.get(0).isCompressed());

    int totalPackets = chunks.get(0).getTotalPackets();
    Assertions.assertEquals(8, totalPackets);

    for (SatelliteMessage m : chunks) {
      Assertions.assertEquals(chunkSize, m.getMessage().length);
      Assertions.assertTrue(m.getPacketNumber() >= 0 && m.getPacketNumber() < totalPackets);
    }

    SatelliteMessage combined = SatelliteMessageFactory.reconstructMessage(chunks);
    Assertions.assertArrayEquals(src, combined.getMessage());
    Assertions.assertFalse(combined.isCompressed());
  }

  @Test
  void singleUncompressedFastPath() {
    byte[] src = payload(400, (byte) 0x01);
    List<SatelliteMessage> msgs = SatelliteMessageFactory.createMessages(src, 1000, false, (byte) 0);

    Assertions.assertEquals(1, msgs.size());

    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();
    SatelliteMessage out = rb.rebuild(msgs.get(0));

    Assertions.assertNotNull(out);
    Assertions.assertSame(msgs.get(0), out);
    Assertions.assertArrayEquals(src, out.getMessage());
    Assertions.assertFalse(out.isCompressed());
  }

  @Test
  void rebuilder_inOrderArrival() {
    byte[] src = payload(5_000, (byte) 0x22);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(src, 700, true, (byte) 0);

    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();
    SatelliteMessage result = null;

    for (SatelliteMessage m : chunks) {
      SatelliteMessage r = rb.rebuild(m);
      if (r != null) {
        result = r;
      }
    }

    Assertions.assertNotNull(result);
    Assertions.assertArrayEquals(src, result.getMessage());
    Assertions.assertTrue(result.isCompressed());
  }

  @Test
  void rebuilder_outOfOrderArrival() {
    byte[] src = payload(3_000, (byte) 0x6E);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(src, 512, true, (byte) 0);

    List<SatelliteMessage> shuffled = shuffledCopy(chunks);

    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();
    SatelliteMessage finished = null;

    for (SatelliteMessage m : shuffled) {
      SatelliteMessage r = rb.rebuild(m);
      if (r != null) {
        finished = r;
      }
    }

    Assertions.assertNotNull(finished);
    Assertions.assertEquals(src.length, finished.getMessage().length);
    Assertions.assertArrayEquals(src, finished.getMessage());
  }

  @Test
  void reconstructMessage_acceptsShuffledInput() {
    byte[] src = payload(6_000, (byte) 0x4B);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(src, 777, true, (byte) 0);

    List<SatelliteMessage> shuffled = shuffledCopy(chunks);

    SatelliteMessage combined = SatelliteMessageFactory.reconstructMessage(shuffled);
    Assertions.assertNotNull(combined);
    Assertions.assertArrayEquals(src, combined.getMessage());
  }

  @Test
  void rebuilder_missingPacket_neverEmits() {
    byte[] src = payload(12_000, (byte) 0x19);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(src, 900, true, (byte) 0);
    Assertions.assertTrue(chunks.size() >= 3);

    List<SatelliteMessage> working = new ArrayList<>(chunks);
    working.remove(working.size() / 2); // drop a middle packet
    List<SatelliteMessage> shuffled = shuffledCopy(working);

    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();
    for (SatelliteMessage m : shuffled) {
      SatelliteMessage out = rb.rebuild(m);
      Assertions.assertNull(out, "Should not emit when a packet is missing");
    }
  }

  @Test
  void rebuilder_duplicatePacket_sameContent_stillReassembles() {
    byte[] src = payload(8_000, (byte) 0x2C);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(src, 800, true, (byte) 0);

    List<SatelliteMessage> list = new ArrayList<>(chunks);

    SatelliteMessage dup = chunks.get(chunks.size() / 2);
    list.add(dup);

    List<SatelliteMessage> shuffled = shuffledCopy(list);

    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();
    SatelliteMessage finished = null;

    for (SatelliteMessage m : shuffled) {
      SatelliteMessage out = rb.rebuild(m);
      if (out != null) {
        finished = out;
      }
    }

    Assertions.assertNotNull(finished);
    Assertions.assertArrayEquals(src, finished.getMessage());
  }

  @Test
  void rebuilder_multipleStreamsInterleaved_reassemblesBoth() {

    byte[] srcA = payload(7_500, (byte) 0x11);
    byte[] srcB = payload(9_200, (byte) 0x55);

    List<SatelliteMessage> chunksA = SatelliteMessageFactory.createMessages( srcA, 700, true, (byte) 0);
    List<SatelliteMessage> chunksB = SatelliteMessageFactory.createMessages( srcB, 650, true, (byte) 0);

    List<SatelliteMessage> mixed = new ArrayList<>(chunksA.size() + chunksB.size());
    mixed.addAll(chunksA);
    mixed.addAll(chunksB);
    Collections.shuffle(mixed, new Random(7654321L));

    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();

    Map<Integer, SatelliteMessage> outputs = new HashMap<>();
    for (SatelliteMessage m : mixed) {
      SatelliteMessage out = rb.rebuild(m);
      if (out != null) {
        outputs.put(out.getStreamNumber(), out);
      }
    }

    Assertions.assertEquals(2, outputs.size());

  }

  @Test
  void compressedFlagPreservedThroughRebuild() {
    byte[] src = payload(2_048, (byte) 0x13);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(src, 600, true, (byte) 0);

    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();
    SatelliteMessage out = null;
    for (SatelliteMessage m : chunks) {
      SatelliteMessage r = rb.rebuild(m);
      if (r != null) out = r;
    }

    Assertions.assertNotNull(out);
    Assertions.assertTrue(out.isCompressed());
  }

  @Test
  void clearResetsState() {
    byte[] src = payload(1_500, (byte) 0x55);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages( src, 500, true, (byte) 0);

    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();
    rb.rebuild(chunks.get(0));
    rb.clear();

    List<SatelliteMessage> shuffled = shuffledCopy(chunks);
    SatelliteMessage finished = null;
    for (SatelliteMessage m : shuffled) {
      SatelliteMessage r = rb.rebuild(m);
      if (r != null) finished = r;
    }

    Assertions.assertNotNull(finished);
    Assertions.assertArrayEquals(src, finished.getMessage());
  }

  @Test
  void multiStream_packFragmentShuffleRebuildUnpack_validatesPayloads() throws Exception {

    Map<String, List<byte[]>> eventMapA = new LinkedHashMap<>();
    eventMapA.put("/a/one", List.of(payload(1200, (byte) 0x01), payload(800, (byte) 0x02)));
    eventMapA.put("/a/two", List.of(payload(50, (byte) 0x03)));
    eventMapA.put("/a/three", List.of(payload(2048, (byte) 0x04), payload(17, (byte) 0x05), payload(600, (byte) 0x06)));

    Map<String, List<byte[]>> eventMapB = new LinkedHashMap<>();
    eventMapB.put("/b/one", List.of(payload(333, (byte) 0x11), payload(444, (byte) 0x12), payload(555, (byte) 0x13)));
    eventMapB.put("/b/two", List.of(payload(4096, (byte) 0x14)));
    eventMapB.put("/b/three", List.of(payload(1, (byte) 0x15), payload(2, (byte) 0x16)));

    CipherManager cipherManager = null;
    int compressionThreshold = 1;
    int maxFragmentSize = 600;
    byte transformationId = (byte) 7;

    MessageQueuePacker.Packed packedA = MessageQueuePacker.pack(eventMapA, compressionThreshold, cipherManager, null);
    MessageQueuePacker.Packed packedB = MessageQueuePacker.pack(eventMapB, compressionThreshold, cipherManager, null);

    Assertions.assertTrue(packedA.compressed());
    Assertions.assertTrue(packedB.compressed());
    Assertions.assertEquals(transformationId, (byte) transformationId);

    List<SatelliteMessage> messagesA = SatelliteMessageFactory.createMessages(packedA.data(), maxFragmentSize, packedA.compressed(), transformationId);
    List<SatelliteMessage> messagesB = SatelliteMessageFactory.createMessages(packedB.data(), maxFragmentSize, packedB.compressed(), transformationId);

    int streamA = messagesA.get(0).getStreamNumber();
    int streamB = messagesB.get(0).getStreamNumber();
    Assertions.assertNotEquals(streamA, streamB, "Streams must be unique for concurrent messages");

    for (SatelliteMessage m : messagesA) {
      Assertions.assertEquals(streamA, m.getStreamNumber());
      Assertions.assertEquals(messagesA.size(), m.getTotalPackets());
      Assertions.assertEquals(transformationId, m.getTransformationId());
      Assertions.assertTrue(m.isCompressed());
    }

    for (SatelliteMessage m : messagesB) {
      Assertions.assertEquals(streamB, m.getStreamNumber());
      Assertions.assertEquals(messagesB.size(), m.getTotalPackets());
      Assertions.assertEquals(transformationId, m.getTransformationId());
      Assertions.assertTrue(m.isCompressed());
    }

    List<SatelliteMessage> interleaved = new ArrayList<>(messagesA.size() + messagesB.size());
    interleaved.addAll(messagesA);
    interleaved.addAll(messagesB);
    Collections.shuffle(interleaved, new Random(0xBADC0FFEL));

    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();

    Map<Integer, SatelliteMessage> rebuilt = new HashMap<>();
    for (SatelliteMessage m : interleaved) {
      SatelliteMessage out = rb.rebuild(m);
      if (out != null) {
        rebuilt.put(out.getStreamNumber(), out);
      }
    }

    Assertions.assertEquals(2, rebuilt.size());
    SatelliteMessage rebuiltA = rebuilt.get(streamA);
    SatelliteMessage rebuiltB = rebuilt.get(streamB);
    Assertions.assertNotNull(rebuiltA);
    Assertions.assertNotNull(rebuiltB);

    Assertions.assertEquals(transformationId, rebuiltA.getTransformationId());
    Assertions.assertEquals(transformationId, rebuiltB.getTransformationId());
    Assertions.assertTrue(rebuiltA.isCompressed());
    Assertions.assertTrue(rebuiltB.isCompressed());

    Map<String, List<byte[]>> unpackedA = MessageQueueUnpacker.unpack(rebuiltA.getMessage(), rebuiltA.isCompressed(), cipherManager);
    Map<String, List<byte[]>> unpackedB = MessageQueueUnpacker.unpack(rebuiltB.getMessage(), rebuiltB.isCompressed(), cipherManager);

    assertEventMapsEqual(eventMapA, unpackedA);
    assertEventMapsEqual(eventMapB, unpackedB);
  }

  private static void assertEventMapsEqual(Map<String, List<byte[]>> expected, Map<String, List<byte[]>> actual) {
    Assertions.assertEquals(expected.keySet(), actual.keySet(), "Topic set differs");
    for (Map.Entry<String, List<byte[]>> entry : expected.entrySet()) {
      String topic = entry.getKey();
      List<byte[]> expList = entry.getValue();
      List<byte[]> actList = actual.get(topic);
      Assertions.assertNotNull(actList, "Missing topic: " + topic);
      Assertions.assertEquals(expList.size(), actList.size(), "Event count differs for topic: " + topic);
      for (int i = 0; i < expList.size(); i++) {
        Assertions.assertArrayEquals(expList.get(i), actList.get(i), "Payload differs for topic: " + topic + " idx=" + i);
      }
    }
  }

}
