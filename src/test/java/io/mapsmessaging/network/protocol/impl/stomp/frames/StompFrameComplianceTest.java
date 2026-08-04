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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StompFrameComplianceTest {

  @Test
  void acceptsCrLfAndUsesFirstRepeatedHeader() throws Exception {
    Send send = assertInstanceOf(
        Send.class,
        parse(
            "SEND\r\n"
                + "destination:/topic/first\r\n"
                + "destination:/topic/second\r\n"
                + "custom:line\\ncolon\\cslash\\\\return\\r\r\n"
                + "\r\n"
                + "body\0"));

    assertTrue(send.isValid());
    assertEquals("/topic/first", send.getDestination());
    assertEquals("line\ncolon:slash\\return\r", send.getHeader("custom"));
    assertEquals("body", new String(send.getData(), StandardCharsets.UTF_8));
  }

  @Test
  void acceptsMixedLfAndCrLfLines() throws Exception {
    Send send = assertInstanceOf(
        Send.class,
        parse(
            "SEND\r\n"
                + "destination:/topic/mixed\n"
                + "content-type:text/plain\r\n"
                + "\n"
                + "body\0"));

    assertTrue(send.isValid());
    assertEquals("/topic/mixed", send.getDestination());
    assertEquals("text/plain", send.getHeader("content-type"));
  }

  @Test
  void connectHeadersAreNotUnescaped() throws Exception {
    Connect connect = assertInstanceOf(
        Connect.class,
        parse(
            "STOMP\r\n"
                + "accept-version:1.2\r\n"
                + "host:localhost\r\n"
                + "login:user\\cname\r\n"
                + "\r\n"
                + "\0"));

    assertEquals("user\\cname", connect.getLogin());
  }

  @Test
  void rejectsUndefinedHeaderEscape() throws Exception {
    byte[] data =
        ("SEND\n"
                + "destination:/topic/test\n"
                + "custom:bad\\tvalue\n"
                + "\n"
                + "body\0")
            .getBytes(StandardCharsets.UTF_8);
    Packet packet = new Packet(ByteBuffer.wrap(data));
    Frame frame = new FrameFactory(1024, false, false).parseFrame(packet);

    assertThrows(StompProtocolException.class, () -> frame.scanFrame(packet));
  }

  private Frame parse(String value) throws Exception {
    Packet packet = new Packet(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));
    Frame frame = new FrameFactory(1024, false, false).parseFrame(packet);
    frame.scanFrame(packet);
    return frame;
  }
}
