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

  @Test
  void splitAndReassemble_inOrder() {
    int streamId = 42;
    byte[] src = payload(10_000, (byte) 0x5A);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 1024, true, (byte)0);

    // basic properties
    Assertions.assertTrue(chunks.size() >= 10);
    Assertions.assertEquals(streamId, chunks.get(0).getStreamNumber());
    Assertions.assertTrue(chunks.stream().allMatch(SatelliteMessage::isCompressed));

    // packet numbers count down to zero
    int expected = chunks.size() - 1;
    for (SatelliteMessage m : chunks) {
      Assertions.assertEquals(expected--, m.getPacketNumber());
    }
    Assertions.assertEquals(0, chunks.get(chunks.size() - 1).getPacketNumber());

    // reconstruct from in-order list
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
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, chunkSize, false, (byte)0);

    Assertions.assertEquals(8, chunks.size());
    Assertions.assertFalse(chunks.get(0).isCompressed());
    for (SatelliteMessage m : chunks) {
      Assertions.assertEquals(chunkSize, m.getMessage().length);
    }
    SatelliteMessage combined = SatelliteMessageFactory.reconstructMessage(chunks);
    Assertions.assertArrayEquals(src, combined.getMessage());
    Assertions.assertFalse(combined.isCompressed());
  }

  @Test
  void singleUncompressedFastPath() {
    int streamId = 9;
    byte[] src = payload(400, (byte) 0x01);
    List<SatelliteMessage> msgs = SatelliteMessageFactory.createMessages(streamId, src, 1000, false, (byte)0);
    Assertions.assertEquals(1, msgs.size());
    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();

    SatelliteMessage out = rb.rebuild(msgs.get(0));
    Assertions.assertNotNull(out);
    Assertions.assertSame(msgs.get(0), out); // fast path returns original
    Assertions.assertArrayEquals(src, out.getMessage());
    Assertions.assertFalse(out.isCompressed());
  }

  @Test
  void rebuilder_inOrderArrival() {
    int streamId = 11;
    byte[] src = payload(5_000, (byte) 0x22);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 700, true, (byte)0);

    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();
    SatelliteMessage result = null;
    for (SatelliteMessage m : chunks) {
      SatelliteMessage r = rb.rebuild(m);
      if (r != null) result = r;
    }
    Assertions.assertNotNull(result);
    Assertions.assertArrayEquals(src, result.getMessage());
    Assertions.assertTrue(result.isCompressed());
  }

  @Test
  void rebuilder_outOfOrderArrival_currentImplLosesData() {
    // Demonstrates current behavior with out-of-order arrival: last chunk first triggers premature reconstruct.
    int streamId = 15;
    byte[] src = payload(3_000, (byte) 0x6E);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 512, true, (byte)0);

    // Shuffle so that packetNumber==0 arrives first
    List<SatelliteMessage> shuffled = new ArrayList<>(chunks);
    shuffled.sort(Comparator.comparingInt(SatelliteMessage::getPacketNumber)); // 0,1,2,...
    Collections.swap(shuffled, 0, shuffled.size() - 1); // move 0 to front

    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();
    SatelliteMessage first = rb.rebuild(shuffled.get(0)); // packetNumber==0
    // Current code returns a prematurely reconstructed message (only last chunk); prove it's not equal.
    Assertions.assertNull(first);

    // Feed the rest; current impl won’t fix the already-returned, but ensure we at least don't throw.
    SatelliteMessage finished = null;
    for (int i = 1; i < shuffled.size(); i++) {
      finished = rb.rebuild(shuffled.get(i));
    }
    Assertions.assertNotNull(finished);
    Assertions.assertEquals(src.length, finished.getMessage().length);
    Assertions.assertArrayEquals(src,finished.getMessage());
  }

  @Test
  void compressedFlagPreservedThroughRebuild() {
    int streamId = 20;
    byte[] src = payload(2_048, (byte) 0x13);
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 600, true,(byte)0);

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
    List<SatelliteMessage> chunks = SatelliteMessageFactory.createMessages(streamId, src, 500, true,(byte)0);

    SatelliteMessageRebuilder rb = new SatelliteMessageRebuilder();
    rb.rebuild(chunks.get(0));
    rb.clear();
    // After clear, feeding last chunk should reconstruct only that chunk (current impl behavior)
    SatelliteMessage only = rb.rebuild(chunks.get(chunks.size() - 1));
    Assertions.assertNotNull(only);
    Assertions.assertNotEquals(src.length, only.getMessage().length);
  }
}
