/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class NmeaBufferContractTest {

  @Nested
  class HappyPath {

    @Test
    void completeSentenceConsumesExactlyOneSentence() throws Exception {
      byte[] data = "$GPGLL,4916.45,N,12311.12,W,225444,A,*1D\r\n".getBytes();
      Packet packet = new Packet(ByteBuffer.wrap(data));

      NMEAPacket parsed = new NMEAPacket(packet);

      assertEquals("GPGLL", parsed.getName());
      assertFalse(packet.hasRemaining());
    }

    @Test
    void completeSentenceLeavesFollowingBytesForNextFrame() throws Exception {
      byte[] first = "$GPGLL,4916.45,N,12311.12,W,225444,A,*1D\r\n".getBytes();
      byte[] second = "$GP".getBytes();
      ByteBuffer data = ByteBuffer.allocate(first.length + second.length);
      data.put(first).put(second).flip();
      Packet packet = new Packet(data);

      new NMEAPacket(packet);

      assertEquals(second.length, packet.available());
      byte[] tail = new byte[second.length];
      packet.get(tail);
      assertArrayEquals(second, tail);
    }
  }

  @Nested
  class SadPath {

    @Test
    void incompleteSentenceRestoresStartPosition() {
      Packet packet = new Packet(ByteBuffer.wrap("$GPGLL,4916".getBytes()));
      int start = packet.position();

      assertThrows(EndOfBufferException.class, () -> new NMEAPacket(packet));
      assertEquals(start, packet.position());
    }
  }

  @Nested
  class Murphy {

    @Test
    void protocolMustReturnWithoutSpinningOnIncompleteSentence() {
      NMEAProtocol protocol = mock(NMEAProtocol.class, CALLS_REAL_METHODS);
      Packet packet = new Packet(ByteBuffer.wrap("$GPGLL,4916".getBytes()));

      assertTimeoutPreemptively(
          Duration.ofMillis(250),
          () -> {
            boolean complete = protocol.processPacket(packet);
            assertFalse(complete, "incomplete stream frame must be left for ReadTask");
          },
          "NMEA must not rewind and retry the same incomplete bytes forever");
    }

    @Test
    void oneByteIncompleteSentenceMustNotSpin() {
      NMEAProtocol protocol = mock(NMEAProtocol.class, CALLS_REAL_METHODS);
      Packet packet = new Packet(ByteBuffer.wrap(new byte[]{'$'}));

      assertTimeoutPreemptively(
          Duration.ofMillis(250),
          () -> assertFalse(protocol.processPacket(packet)));
    }
  }
}
