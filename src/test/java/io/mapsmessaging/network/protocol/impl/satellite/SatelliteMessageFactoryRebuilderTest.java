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

import io.mapsmessaging.network.protocol.impl.satellite.protocol.SatelliteMessage;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.SatelliteMessageFactory;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.SatelliteMessageRebuilder;
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
    int streamId = 42;
    byte[] src = payload(10_000, (byte) 0x5A);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 1024, true, (byte) 0);

    Assertions.assertTrue(chunks.size() >= 10);
    Assertions.assertEquals(streamId, chunks.get(0).getStreamNumber());
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
    int streamId = 7;
    int chunkSize = 512;
    byte[] src = payload(chunkSize * 8, (byte) 0x33);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, chunkSize, false, (byte) 0);

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
    int streamId = 9;
    byte[] src = payload(400, (byte) 0x01);
    List<SatelliteMessage> msgs = SatelliteMessageFactory.createMessages(streamId, src, 1000, false, (byte) 0);

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
    int streamId = 11;
    byte[] src = payload(5_000, (byte) 0x22);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 700, true, (byte) 0);

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
    int streamId = 15;
    byte[] src = payload(3_000, (byte) 0x6E);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 512, true, (byte) 0);

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
    int streamId = 99;
    byte[] src = payload(6_000, (byte) 0x4B);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 777, true, (byte) 0);

    List<SatelliteMessage> shuffled = shuffledCopy(chunks);

    SatelliteMessage combined = SatelliteMessageFactory.reconstructMessage(shuffled);
    Assertions.assertNotNull(combined);
    Assertions.assertEquals(streamId, combined.getStreamNumber());
    Assertions.assertArrayEquals(src, combined.getMessage());
  }

  @Test
  void rebuilder_missingPacket_neverEmits() {
    int streamId = 101;
    byte[] src = payload(12_000, (byte) 0x19);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 900, true, (byte) 0);
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
    int streamId = 102;
    byte[] src = payload(8_000, (byte) 0x2C);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 800, true, (byte) 0);

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
    int streamA = 201;
    int streamB = 202;

    byte[] srcA = payload(7_500, (byte) 0x11);
    byte[] srcB = payload(9_200, (byte) 0x55);

    List<SatelliteMessage> chunksA = SatelliteMessageFactory.createMessages(streamA, srcA, 700, true, (byte) 0);
    List<SatelliteMessage> chunksB = SatelliteMessageFactory.createMessages(streamB, srcB, 650, true, (byte) 0);

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
    Assertions.assertTrue(outputs.containsKey(streamA));
    Assertions.assertTrue(outputs.containsKey(streamB));

    Assertions.assertArrayEquals(srcA, outputs.get(streamA).getMessage());
    Assertions.assertArrayEquals(srcB, outputs.get(streamB).getMessage());
  }

  @Test
  void compressedFlagPreservedThroughRebuild() {
    int streamId = 20;
    byte[] src = payload(2_048, (byte) 0x13);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 600, true, (byte) 0);

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
    int streamId = 25;
    byte[] src = payload(1_500, (byte) 0x55);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 500, true, (byte) 0);

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
}
