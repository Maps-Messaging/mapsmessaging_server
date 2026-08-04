/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 */

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.network.io.Packet;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StompErrorFrameTest {

  @Test
  void contentLengthExcludesTheNullTerminator() {
    Error error = new Error();
    error.setContentType("text/plain");
    error.setContent("bad".getBytes(StandardCharsets.UTF_8));
    Packet packet = new Packet(256, false);

    error.packFrame(packet);
    packet.flip();
    byte[] bytes = new byte[packet.available()];
    packet.get(bytes);
    String frame = new String(bytes, StandardCharsets.UTF_8);

    assertTrue(frame.contains("content-length:3\n"), frame);
    assertTrue(frame.endsWith("\n\nbad\0"), frame);
    assertEquals(1, count(bytes, (byte) 0));
  }

  @Test
  void parsesContentLengthErrorBody() throws Exception {
    Error error = parse("ERROR\ncontent-length:3\n\nbad\0");

    assertArrayEquals("bad".getBytes(StandardCharsets.UTF_8), error.getContent());
  }

  @Test
  void parsesNullTerminatedErrorBody() throws Exception {
    Error error = parse("ERROR\ncontent-type:text/plain\n\nfailed\0");

    assertArrayEquals("failed".getBytes(StandardCharsets.UTF_8), error.getContent());
  }

  private Error parse(String frameText) throws Exception {
    Packet packet =
        new Packet(ByteBuffer.wrap(frameText.getBytes(StandardCharsets.UTF_8)));
    Error error =
        assertInstanceOf(
            Error.class, new FrameFactory(1024, true, false).parseFrame(packet));
    error.scanFrame(packet);
    return error;
  }

  private int count(byte[] bytes, byte expected) {
    int result = 0;
    for (byte value : bytes) {
      if (value == expected) {
        result++;
      }
    }
    return result;
  }
}
