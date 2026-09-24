/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.io.impl.serial;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class SerialStreamBufferLifecycleTest {

  @Nested
  class HappyPath {

    @Test
    void inputAppendsNewBytesAfterExistingCompactedStreamTail() throws Exception {
      SimpleStreamHandler handler = new SimpleStreamHandler(16);
      Packet packet = new Packet(16, false);
      packet.put(new byte[]{1, 2});

      int read = handler.parseInput(new ByteArrayInputStream(new byte[]{3, 4}), packet);

      assertEquals(2, read);
      packet.flip();
      assertArrayEquals(new byte[]{1, 2, 3, 4}, remaining(packet),
          "serial is a byte stream, so previously compacted bytes must survive the next read");
    }

    @Test
    void shortInputReadAdvancesPacketPositionByBytesRead() throws Exception {
      SimpleStreamHandler handler = new SimpleStreamHandler(16);
      Packet packet = new Packet(16, false);

      int read = handler.parseInput(new ByteArrayInputStream(new byte[]{10, 11, 12}), packet);

      assertEquals(3, read);
      assertEquals(3, packet.position());
    }

    @Test
    void outputWritesAllRemainingBytesAndConsumesPacket() throws Exception {
      SimpleStreamHandler handler = new SimpleStreamHandler(16);
      Packet packet = new Packet(ByteBuffer.wrap(new byte[]{5, 6, 7, 8}));
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      int wrote = handler.parseOutput(output, packet);

      assertEquals(4, wrote);
      assertArrayEquals(new byte[]{5, 6, 7, 8}, output.toByteArray());
      assertFalse(packet.hasRemaining());
    }
  }

  @Nested
  class SadPath {

    @Test
    void closedInputStreamRaisesIOExceptionInsteadOfPretendingZeroBytes() {
      SimpleStreamHandler handler = new SimpleStreamHandler(16);
      Packet packet = new Packet(16, false);
      InputStream input = new InputStream() {
        @Override
        public int available() {
          return 1;
        }

        @Override
        public int read() {
          return -1;
        }

        @Override
        public int read(byte[] b, int off, int len) {
          return -1;
        }
      };

      IOException exception = assertThrows(IOException.class, () -> handler.parseInput(input, packet));
      assertTrue(exception.getMessage().toLowerCase().contains("closed"));
    }

    @Test
    void inputMustRespectRemainingPacketCapacity() {
      SimpleStreamHandler handler = new SimpleStreamHandler(16);
      Packet packet = new Packet(4, false);
      packet.put(new byte[]{1, 2, 3});

      assertDoesNotThrow(() -> handler.parseInput(new ByteArrayInputStream(new byte[]{4}), packet));
      assertEquals(4, packet.position());
    }
  }

  @Nested
  class Murphy {

    @Test
    void fragmentedSerialFrameAcrossManyReadsIsNeverErased() throws Exception {
      SimpleStreamHandler handler = new SimpleStreamHandler(8);
      Packet packet = new Packet(8, false);

      for (int i = 1; i <= 6; i++) {
        int read = handler.parseInput(new ByteArrayInputStream(new byte[]{(byte) i}), packet);
        assertEquals(1, read);
      }

      packet.flip();
      assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6}, remaining(packet));
    }

    @Test
    void outputLargerThanInternalBufferIsChunkedWithoutOverflow() throws Exception {
      SimpleStreamHandler handler = new SimpleStreamHandler(4);
      byte[] payload = new byte[17];
      for (int i = 0; i < payload.length; i++) {
        payload[i] = (byte) i;
      }
      Packet packet = new Packet(ByteBuffer.wrap(payload));
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      int wrote = assertDoesNotThrow(() -> handler.parseOutput(output, packet));

      assertEquals(payload.length, wrote);
      assertArrayEquals(payload, output.toByteArray());
      assertFalse(packet.hasRemaining());
    }

    @Test
    void exactInternalBufferBoundaryWritesOnceWithoutDuplication() throws Exception {
      SimpleStreamHandler handler = new SimpleStreamHandler(4);
      byte[] payload = new byte[]{1, 2, 3, 4};
      Packet packet = new Packet(ByteBuffer.wrap(payload));
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      int wrote = handler.parseOutput(output, packet);

      assertEquals(4, wrote);
      assertArrayEquals(payload, output.toByteArray());
      assertFalse(packet.hasRemaining());
    }

    @Test
    void zeroAvailableInputDoesNotAlterExistingStreamTail() throws Exception {
      SimpleStreamHandler handler = new SimpleStreamHandler(8);
      Packet packet = new Packet(8, false);
      packet.put(new byte[]{9, 8, 7});

      int read = handler.parseInput(InputStream.nullInputStream(), packet);

      assertEquals(0, read);
      assertEquals(3, packet.position());
      packet.flip();
      assertArrayEquals(new byte[]{9, 8, 7}, remaining(packet));
    }
  }

  private static byte[] remaining(Packet packet) {
    byte[] data = new byte[packet.available()];
    packet.get(data);
    return data;
  }
}
