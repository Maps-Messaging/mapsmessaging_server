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

package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SatelliteGatewayHardeningTest {

  @Test
  void returnsNullForInvalidTotalPacketsZero() {
    SatelliteMessageRebuilder rebuilder = new SatelliteMessageRebuilder();

    SatelliteMessage fragment = fragment((byte) 1, 0, 0, false, (byte) 1, bytes(1, 2, 3));

    SatelliteMessage rebuilt = rebuilder.rebuild(fragment);
    assertNull(rebuilt, "totalPackets=0 must be rejected");
  }

  @Test
  void returnsNullForPacketNumberEqualToTotalPackets() {
    SatelliteMessageRebuilder rebuilder = new SatelliteMessageRebuilder();

    SatelliteMessage fragment = fragment((byte) 2, 2, 2, false, (byte) 1, bytes(1));

    SatelliteMessage rebuilt = rebuilder.rebuild(fragment);
    assertNull(rebuilt, "packetNumber==totalPackets must be rejected");
  }

  @Test
  void returnsNullForNegativePacketNumber() {
    SatelliteMessageRebuilder rebuilder = new SatelliteMessageRebuilder();

    SatelliteMessage fragment = fragment((byte) 3, -1, 2, false, (byte) 1, bytes(1));

    SatelliteMessage rebuilt = rebuilder.rebuild(fragment);
    assertNull(rebuilt, "negative packetNumber must be rejected");
  }

  @Test
  void doesNotBreakOtherStreamsWhenGarbageArrives() {
    SatelliteMessageRebuilder rebuilder = new SatelliteMessageRebuilder();

    // Valid stream A: 2 fragments
    SatelliteMessage a0 = fragment((byte) 10, 0, 2, false, (byte) 1, bytes(10));
    SatelliteMessage a1 = fragment((byte) 10, 1, 2, false, (byte) 1, bytes(11));

    // Garbage: bad packetNumber
    SatelliteMessage bad = fragment((byte) 11, 999, 2, false, (byte) 1, bytes(99));

    assertNull(rebuilder.rebuild(a0));
    assertNull(rebuilder.rebuild(bad));

    SatelliteMessage rebuilt = rebuilder.rebuild(a1);
    assertNotNull(rebuilt, "valid stream must rebuild even with interleaved garbage");
    assertEquals(10, rebuilt.getStreamNumber());
  }

  @Test
  void rejectsStreamCollision_totalPacketsMismatch() {
    SatelliteMessageRebuilder rebuilder = new SatelliteMessageRebuilder();

    byte stream = 20;

    SatelliteMessage a0 = fragment(stream, 0, 2, false, (byte) 1, bytes(1));
    SatelliteMessage b0 = fragment(stream, 0, 3, false, (byte) 1, bytes(2)); // mismatch totalPackets
    SatelliteMessage a1 = fragment(stream, 1, 2, false, (byte) 1, bytes(3));

    assertNull(rebuilder.rebuild(a0));
    assertNull(rebuilder.rebuild(b0), "collision must be rejected and state cleared");

    // After collision, even providing remaining valid fragments must not rebuild the old message.
    assertNull(rebuilder.rebuild(a1), "stream state should have been cleared on mismatch");
  }

  // ----------------------------
  // Wiring: your API is rebuild() returning null if incomplete/invalid.
  // ----------------------------

  private static Optional<SatelliteMessage> offer(SatelliteMessageRebuilder rebuilder, SatelliteMessage fragment) {
    return Optional.ofNullable(rebuilder.rebuild(fragment));
  }

  // ----------------------------
  // Helpers: build a fragment with the fields your rebuilder uses.
  // This assumes SatelliteMessage has setters used elsewhere in your tests.
  // If your SatelliteMessage is immutable, replace with the right constructor/builder.
  // ----------------------------

  private static SatelliteMessage fragment(
      byte streamNumber,
      int packetNumber,
      int totalPackets,
      boolean compressed,
      byte transformationId,
      byte[] payload
  ) {
    SatelliteMessage message = new SatelliteMessage();
    message.setStreamNumber(streamNumber);
    message.setPacketNumber(packetNumber);
    message.setTotalPackets(totalPackets);
    message.setCompressed(compressed);
    message.setTransformationId(transformationId);

    // The current factory uses getMessage() to concatenate.
    message.setMessage(payload);

    return message;
  }

  private static byte[] bytes(int... values) {
    byte[] out = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = (byte) values[i];
    }
    return out;
  }
}