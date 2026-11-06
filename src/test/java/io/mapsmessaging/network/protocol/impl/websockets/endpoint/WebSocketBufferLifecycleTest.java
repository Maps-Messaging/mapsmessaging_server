/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.websockets.endpoint;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketBufferLifecycleTest {

  @Nested
  class HappyPath {

    @Test
    void completeMaskedBinaryFrameDecodesExactlyOnce() throws Exception {
      WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
      Packet source = packet(maskedFrame(true, WebSocketFrameDecoder.BINARY, new byte[]{1, 2, 3, 4}));
      Packet destination = new Packet(16, false);

      int decoded = decoder.decode(source, destination, null);

      assertEquals(4, decoded);
      destination.flip();
      assertArrayEquals(new byte[]{1, 2, 3, 4}, remaining(destination));
      assertFalse(decoder.hasPendingOutput());
    }

    @Test
    void frameSplitAcrossManyReadsPreservesDecoderState() throws Exception {
      WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
      byte[] frame = maskedFrame(true, WebSocketFrameDecoder.BINARY, new byte[]{5, 6, 7, 8});
      Packet destination = new Packet(16, false);

      for (byte value : frame) {
        Packet singleByte = packet(new byte[]{value});
        decoder.decode(singleByte, destination, null);
      }

      destination.flip();
      assertArrayEquals(new byte[]{5, 6, 7, 8}, remaining(destination));
    }

    @Test
    void decodedPayloadLargerThanDestinationCanBeDrainedWithoutMoreNetworkData() throws Exception {
      WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
      Packet source = packet(maskedFrame(true, WebSocketFrameDecoder.BINARY, new byte[]{1, 2, 3, 4, 5, 6}));
      Packet first = new Packet(3, false);

      int firstDecoded = decoder.decode(source, first, null);

      assertEquals(3, firstDecoded);
      assertTrue(decoder.hasPendingOutput());

      Packet second = new Packet(3, false);
      int secondDecoded = decoder.drain(second);

      assertEquals(3, secondDecoded);
      assertFalse(decoder.hasPendingOutput());

      first.flip();
      second.flip();
      assertArrayEquals(new byte[]{1, 2, 3}, remaining(first));
      assertArrayEquals(new byte[]{4, 5, 6}, remaining(second));
    }
  }

  @Nested
  class SadPath {

    @Test
    void unmaskedClientFrameIsRejected() {
      WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
      Packet source = packet(new byte[]{
          (byte) (0x80 | WebSocketFrameDecoder.BINARY),
          0x01,
          0x01
      });
      Packet destination = new Packet(8, false);

      assertThrows(WebSocketProtocolException.class, () -> decoder.decode(source, destination, null));
    }

    @Test
    void fragmentedControlFrameIsRejected() {
      WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
      Packet source = packet(maskedFrame(false, WebSocketFrameDecoder.PING, new byte[]{1}));
      Packet destination = new Packet(8, false);

      assertThrows(WebSocketProtocolException.class, () -> decoder.decode(source, destination, null));
    }

    @Test
    void invalidUtf8TextFrameIsRejected() {
      WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
      Packet source = packet(maskedFrame(true, WebSocketFrameDecoder.TEXT, new byte[]{(byte) 0xC3, 0x28}));
      Packet destination = new Packet(8, false);

      assertThrows(WebSocketProtocolException.class, () -> decoder.decode(source, destination, null));
    }
  }

  @Nested
  class Murphy {

    @Test
    void fragmentedMessageWithInterleavedPingPreservesApplicationPayload() throws Exception {
      WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
      List<byte[]> pings = new ArrayList<>();
      WebSocketFrameDecoder.Listener listener = new WebSocketFrameDecoder.Listener() {
        @Override
        public void onPing(byte[] payload) {
          pings.add(payload);
        }

        @Override
        public void onPong(byte[] payload) {
        }

        @Override
        public void onClose(byte[] payload) {
        }
      };

      Packet destination = new Packet(16, false);
      decoder.decode(packet(maskedFrame(false, WebSocketFrameDecoder.BINARY, new byte[]{1, 2})), destination, listener);
      decoder.decode(packet(maskedFrame(true, WebSocketFrameDecoder.PING, new byte[]{9})), destination, listener);
      decoder.decode(packet(maskedFrame(true, WebSocketFrameDecoder.CONTINUATION, new byte[]{3, 4})), destination, listener);

      destination.flip();
      assertArrayEquals(new byte[]{1, 2, 3, 4}, remaining(destination));
      assertEquals(1, pings.size());
      assertArrayEquals(new byte[]{9}, pings.getFirst());
    }

    @Test
    void writerSurvivesPartialAndZeroProgressTransportWrites() throws Exception {
      AtomicInteger calls = new AtomicInteger();
      List<Byte> network = new ArrayList<>();
      WebSocketFrameWriter writer = new WebSocketFrameWriter(packet -> {
        int call = calls.getAndIncrement();
        if (call == 0) {
          int sent = Math.min(2, packet.available());
          for (int i = 0; i < sent; i++) {
            network.add(packet.get());
          }
          return sent;
        }
        if (call == 1) {
          return 0;
        }
        int sent = packet.available();
        while (packet.hasRemaining()) {
          network.add(packet.get());
        }
        return sent;
      });

      Packet application = packet(new byte[]{10, 11, 12, 13});

      assertEquals(0, writer.writeBinary(application));
      assertTrue(writer.hasPendingApplicationData());
      assertEquals(4, writer.writeBinary(application));
      assertFalse(writer.hasPendingApplicationData());
      assertEquals(application.limit(), application.position());
      assertTrue(network.size() >= 6, "encoded websocket frame must include header and payload");
    }

    @Test
    void oneByteDestinationRepeatedDrainNeverDuplicatesOrDropsPayload() throws Exception {
      WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
      Packet source = packet(maskedFrame(true, WebSocketFrameDecoder.BINARY, new byte[]{1, 2, 3, 4, 5}));
      List<Byte> output = new ArrayList<>();

      assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
        Packet destination = new Packet(1, false);
        decoder.decode(source, destination, null);
        destination.flip();
        while (destination.hasRemaining()) {
          output.add(destination.get());
        }

        while (decoder.hasPendingOutput()) {
          Packet next = new Packet(1, false);
          decoder.drain(next);
          next.flip();
          while (next.hasRemaining()) {
            output.add(next.get());
          }
        }
      });

      assertEquals(List.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5), output);
    }
  }

  private static byte[] maskedFrame(boolean fin, int opcode, byte[] payload) {
    byte[] mask = new byte[]{0x11, 0x22, 0x33, 0x44};
    if (payload.length >= 126) {
      throw new IllegalArgumentException("test helper only supports short payloads");
    }
    byte[] frame = new byte[2 + 4 + payload.length];
    frame[0] = (byte) ((fin ? 0x80 : 0x00) | opcode);
    frame[1] = (byte) (0x80 | payload.length);
    System.arraycopy(mask, 0, frame, 2, mask.length);
    for (int i = 0; i < payload.length; i++) {
      frame[6 + i] = (byte) (payload[i] ^ mask[i & 3]);
    }
    return frame;
  }

  private static Packet packet(byte[] data) {
    return new Packet(ByteBuffer.wrap(data));
  }

  private static byte[] remaining(Packet packet) {
    byte[] data = new byte[packet.available()];
    packet.get(data);
    return data;
  }
}
