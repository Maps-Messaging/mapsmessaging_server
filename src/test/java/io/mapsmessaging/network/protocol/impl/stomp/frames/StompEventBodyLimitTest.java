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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StompEventBodyLimitTest {

  @Test
  void rejectsOversizedBodyWithoutContentLength() throws Exception {
    Packet packet = packet("SEND\ndestination:/topic/test\n\n123456789\0");
    Frame frame = new FrameFactory(8, false, false).parseFrame(packet);

    assertThrows(StompProtocolException.class, () -> frame.scanFrame(packet));
  }

  @Test
  void rejectsOversizedBodyWithContentLength() throws Exception {
    Packet packet =
        packet("SEND\ndestination:/topic/test\ncontent-length:9\n\n123456789\0");
    Frame frame = new FrameFactory(8, false, false).parseFrame(packet);

    assertThrows(StompProtocolException.class, () -> frame.scanFrame(packet));
  }

  @Test
  void rejectsNegativeAndNonNumericContentLength() throws Exception {
    Packet negative =
        packet("SEND\ndestination:/topic/test\ncontent-length:-1\n\n\0");
    Frame negativeFrame = new FrameFactory(8, false, false).parseFrame(negative);
    assertThrows(StompProtocolException.class, () -> negativeFrame.scanFrame(negative));

    Packet text =
        packet("SEND\ndestination:/topic/test\ncontent-length:nope\n\n\0");
    Frame textFrame = new FrameFactory(8, false, false).parseFrame(text);
    assertThrows(StompProtocolException.class, () -> textFrame.scanFrame(text));
  }

  @Test
  void contentLengthAllowsEmbeddedNullBytes() throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.writeBytes(
        "SEND\ndestination:/topic/test\ncontent-length:3\n\n"
            .getBytes(StandardCharsets.UTF_8));
    output.write('a');
    output.write(0);
    output.write('b');
    output.write(0);

    Packet packet = new Packet(ByteBuffer.wrap(output.toByteArray()));
    Send send = (Send) new FrameFactory(8, false, false).parseFrame(packet);
    send.scanFrame(packet);

    assertTrue(send.isValid());
    assertArrayEquals(new byte[]{'a', 0, 'b'}, send.getData());
  }

  private Packet packet(String value) {
    return new Packet(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));
  }
}
