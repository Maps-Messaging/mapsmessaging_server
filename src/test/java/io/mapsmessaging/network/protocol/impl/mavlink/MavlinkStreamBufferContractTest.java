/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class MavlinkStreamBufferContractTest {

  @Nested
  class HappyPath {

    @Test
    void v1FrameCanArriveOneByteAtATime() throws Exception {
      MavlinkStreamHandler handler = new MavlinkStreamHandler(500);
      byte[] frame = mavlinkV1Frame(3);
      Packet packet = new Packet(frame.length, false);

      int read = handler.parseInput(oneByteInput(frame), packet);

      assertEquals(frame.length, read);
      packet.flip();
      assertArrayEquals(frame, remaining(packet));
    }

    @Test
    void v2SignedFrameCanArriveOneByteAtATime() throws Exception {
      MavlinkStreamHandler handler = new MavlinkStreamHandler(500);
      byte[] frame = mavlinkV2Frame(4, true);
      Packet packet = new Packet(frame.length, false);

      int read = handler.parseInput(oneByteInput(frame), packet);

      assertEquals(frame.length, read);
      packet.flip();
      assertArrayEquals(frame, remaining(packet));
    }

    @Test
    void noiseBeforeMagicIsDiscardedWithoutCorruptingFrame() throws Exception {
      MavlinkStreamHandler handler = new MavlinkStreamHandler(500);
      byte[] frame = mavlinkV1Frame(2);
      byte[] input = new byte[frame.length + 3];
      input[0] = 0x00;
      input[1] = 0x01;
      input[2] = 0x02;
      System.arraycopy(frame, 0, input, 3, frame.length);
      Packet packet = new Packet(frame.length, false);

      int read = handler.parseInput(new ByteArrayInputStream(input), packet);

      assertEquals(frame.length, read);
      packet.flip();
      assertArrayEquals(frame, remaining(packet));
    }
  }

  @Nested
  class SadPath {

    @Test
    void eofMidFrameFailsInsteadOfPublishingPartialFrame() {
      MavlinkStreamHandler handler = new MavlinkStreamHandler(100);
      byte[] frame = mavlinkV2Frame(5, false);
      byte[] truncated = java.util.Arrays.copyOf(frame, frame.length - 2);
      Packet packet = new Packet(frame.length, false);

      assertThrows(IOException.class, () -> handler.parseInput(new ByteArrayInputStream(truncated), packet));
    }

    @Test
    void destinationSmallerThanFrameIsRejected() {
      MavlinkStreamHandler handler = new MavlinkStreamHandler(100);
      byte[] frame = mavlinkV1Frame(10);
      Packet packet = new Packet(frame.length - 1, false);

      assertThrows(IOException.class, () -> handler.parseInput(new ByteArrayInputStream(frame), packet));
    }
  }

  @Nested
  class Murphy {

    @Test
    void successfulOutputMustConsumeOriginalPacket() throws Exception {
      MavlinkStreamHandler handler = new MavlinkStreamHandler(100);
      byte[] frame = mavlinkV2Frame(20, false);
      Packet packet = new Packet(ByteBuffer.wrap(frame));
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      int written = handler.parseOutput(output, packet);

      assertEquals(frame.length, written);
      assertArrayEquals(frame, output.toByteArray());
      assertFalse(packet.hasRemaining(),
          "StreamHandler must advance the original Packet so FrameHandler sees the write as complete");
    }

    @Test
    void largeOutputIsChunkedButStillConsumesOriginalPacket() throws Exception {
      MavlinkStreamHandler handler = new MavlinkStreamHandler(100);
      byte[] payload = new byte[257];
      for (int i = 0; i < payload.length; i++) {
        payload[i] = (byte) i;
      }
      Packet packet = new Packet(ByteBuffer.wrap(payload));
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      int written = handler.parseOutput(output, packet);

      assertEquals(payload.length, written);
      assertArrayEquals(payload, output.toByteArray());
      assertFalse(packet.hasRemaining());
    }

    @Test
    void zeroReadsEventuallyTimeoutInsteadOfSpinningForever() {
      MavlinkStreamHandler handler = new MavlinkStreamHandler(25);
      byte[] frame = mavlinkV1Frame(1);
      InputStream input = new InputStream() {
        private int offset;

        @Override
        public int read() {
          if (offset < 2) {
            return frame[offset++] & 0xff;
          }
          return 0;
        }

        @Override
        public int read(byte[] b, int off, int len) {
          return 0;
        }
      };

      Packet packet = new Packet(frame.length, false);
      assertTimeoutPreemptively(Duration.ofSeconds(1),
          () -> assertThrows(IOException.class, () -> handler.parseInput(input, packet)));
    }
  }

  private static InputStream oneByteInput(byte[] data) {
    return new InputStream() {
      private int offset;

      @Override
      public int read() {
        if (offset >= data.length) {
          return -1;
        }
        return data[offset++] & 0xff;
      }

      @Override
      public int read(byte[] b, int off, int len) {
        if (offset >= data.length) {
          return -1;
        }
        b[off] = data[offset++];
        return 1;
      }
    };
  }

  private static byte[] mavlinkV1Frame(int payloadLength) {
    byte[] frame = new byte[2 + 4 + payloadLength + 2];
    frame[0] = (byte) 0xFE;
    frame[1] = (byte) payloadLength;
    for (int i = 2; i < frame.length; i++) {
      frame[i] = (byte) i;
    }
    return frame;
  }

  private static byte[] mavlinkV2Frame(int payloadLength, boolean signed) {
    int signature = signed ? 13 : 0;
    byte[] frame = new byte[2 + 8 + payloadLength + 2 + signature];
    frame[0] = (byte) 0xFD;
    frame[1] = (byte) payloadLength;
    frame[2] = signed ? (byte) 0x01 : 0;
    for (int i = 3; i < frame.length; i++) {
      frame[i] = (byte) i;
    }
    return frame;
  }

  private static byte[] remaining(Packet packet) {
    byte[] data = new byte[packet.available()];
    packet.get(data);
    return data;
  }
}
