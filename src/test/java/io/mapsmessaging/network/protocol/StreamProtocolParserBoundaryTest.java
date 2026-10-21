/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class StreamProtocolParserBoundaryTest {

  @Nested
  class HappyPath {

    @Test
    void stompFactoryConsumesOnlyCommandLineAndLeavesBodyForFrameParser() throws Exception {
      var factory = new io.mapsmessaging.network.protocol.impl.stomp.frames.FrameFactory(4096, false, false);
      Packet packet = new Packet(ByteBuffer.wrap("CONNECT\naccept-version:1.2\n\n\0".getBytes()));

      var frame = factory.parseFrame(packet);

      assertNotNull(frame);
      assertTrue(packet.position() > 0);
      assertTrue(packet.hasRemaining());
    }

    @Test
    void natsFactoryConsumesOnlyVerbAndLeavesFrameContent() throws Exception {
      var factory = new io.mapsmessaging.network.protocol.impl.nats.frames.FrameFactory(4096, false);
      Packet packet = new Packet(ByteBuffer.wrap("PUB foo 3\r\nabc\r\n".getBytes()));

      var frame = factory.parseFrame(packet);

      assertNotNull(frame);
      assertTrue(packet.hasRemaining());
    }
  }

  @Nested
  class SadPath {

    @Test
    void stompIncompleteCommandRestoresPosition() {
      var factory = new io.mapsmessaging.network.protocol.impl.stomp.frames.FrameFactory(4096, false, false);
      Packet packet = new Packet(ByteBuffer.wrap("CONN".getBytes()));
      int start = packet.position();

      assertThrows(EndOfBufferException.class, () -> factory.parseFrame(packet));
      assertEquals(start, packet.position());
    }

    @Test
    void natsIncompleteCommandRestoresPosition() {
      var factory = new io.mapsmessaging.network.protocol.impl.nats.frames.FrameFactory(4096, false);
      Packet packet = new Packet(ByteBuffer.wrap("PU".getBytes()));
      int start = packet.position();

      assertThrows(EndOfBufferException.class, () -> factory.parseFrame(packet));
      assertEquals(start, packet.position());
    }

    @Test
    void malformedStompCommandRestoresPositionBeforeFailure() {
      var factory = new io.mapsmessaging.network.protocol.impl.stomp.frames.FrameFactory(4096, false, false);
      Packet packet = new Packet(ByteBuffer.wrap("BOGUS\n".getBytes()));
      int start = packet.position();

      assertThrows(StompProtocolException.class, () -> factory.parseFrame(packet));
      assertEquals(start, packet.position());
    }

    @Test
    void malformedNatsCommandRestoresPositionBeforeFailure() {
      var factory = new io.mapsmessaging.network.protocol.impl.nats.frames.FrameFactory(4096, false);
      Packet packet = new Packet(ByteBuffer.wrap("BOGUS \r\n".getBytes()));
      int start = packet.position();

      assertThrows(NatsProtocolException.class, () -> factory.parseFrame(packet));
      assertEquals(start, packet.position());
    }
  }

  @Nested
  class Murphy {

    @Test
    void stompCommandSplitAtEveryByteBoundaryRestoresPositionUntilComplete() throws Exception {
      byte[] full = "CONNECT\n".getBytes();
      var factory = new io.mapsmessaging.network.protocol.impl.stomp.frames.FrameFactory(4096, false, false);

      for (int split = 1; split < full.length; split++) {
        Packet partial = new Packet(ByteBuffer.wrap(java.util.Arrays.copyOf(full, split)));
        int start = partial.position();
        assertThrows(EndOfBufferException.class, () -> factory.parseFrame(partial), "split=" + split);
        assertEquals(start, partial.position(), "split=" + split);
      }

      Packet complete = new Packet(ByteBuffer.wrap(full));
      assertNotNull(factory.parseFrame(complete));
    }

    @Test
    void natsCommandSplitAtEveryByteBoundaryRestoresPositionUntilComplete() throws Exception {
      byte[] full = "PING\r\n".getBytes();
      var factory = new io.mapsmessaging.network.protocol.impl.nats.frames.FrameFactory(4096, false);

      for (int split = 1; split < full.length; split++) {
        Packet partial = new Packet(ByteBuffer.wrap(java.util.Arrays.copyOf(full, split)));
        int start = partial.position();
        assertThrows(EndOfBufferException.class, () -> factory.parseFrame(partial), "split=" + split);
        assertEquals(start, partial.position(), "split=" + split);
      }

      Packet complete = new Packet(ByteBuffer.wrap(full));
      assertNotNull(factory.parseFrame(complete));
    }
  }
}
