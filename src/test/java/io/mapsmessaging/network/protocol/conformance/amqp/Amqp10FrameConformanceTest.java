/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.amqp;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.test.BaseTestConfig;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("conformance")
@Tag("conformance-core")
@Tag("amqp10")
class Amqp10FrameConformanceTest extends BaseTestConfig {

  private static final byte[] AMQP_1_0 = new byte[]{'A', 'M', 'Q', 'P', 0, 1, 0, 0};
  private static final String SOURCE =
      "https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-transport-v1.0-os.html";

  @Test
  @Disabled("Known conformance gap: MSG-397")
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 section 2.3.1: frame SIZE must be at least 8 octets",
      source = SOURCE)
  void frameSizeBelowMinimumIsRejected() throws Exception {
    try (Socket socket = connectedTransport()) {
      DataOutputStream output = new DataOutputStream(socket.getOutputStream());
      output.writeInt(7);
      output.writeByte(2);
      output.writeByte(0);
      output.writeShort(0);
      output.flush();

      assertEventuallyClosed(socket);
    }
  }

  @Test
  @Disabled("Known conformance gap: MSG-397")
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 section 2.3.1: data offset must describe a valid frame header",
      source = SOURCE)
  void dataOffsetBelowMinimumIsRejected() throws Exception {
    try (Socket socket = connectedTransport()) {
      DataOutputStream output = new DataOutputStream(socket.getOutputStream());
      output.writeInt(8);
      output.writeByte(1);
      output.writeByte(0);
      output.writeShort(0);
      output.flush();

      assertEventuallyClosed(socket);
    }
  }


  @Test
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 section 2.3.1: frame body begins at DOFF*4 and DOFF must describe a position within SIZE",
      source = SOURCE)
  void dataOffsetBeyondFrameSizeIsRejected() throws Exception {
    try (Socket socket = connectedTransport()) {
      DataOutputStream output = new DataOutputStream(socket.getOutputStream());
      output.writeInt(8);
      output.writeByte(3);
      output.writeByte(0);
      output.writeShort(0);
      output.flush();

      assertEventuallyClosed(socket);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 section 2.3.1: AMQP frame type is 0x00; SASL frame type 0x01 is defined only for SASL framing",
      source = SOURCE)
  void unknownFrameTypeIsRejected() throws Exception {
    try (Socket socket = connectedTransport()) {
      DataOutputStream output = new DataOutputStream(socket.getOutputStream());
      output.writeInt(8);
      output.writeByte(2);
      output.writeByte(0x7f);
      output.writeShort(0);
      output.flush();

      assertEventuallyClosed(socket);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 section 2.4.1: prior to negotiation max-frame-size is 512 octets",
      source = SOURCE)
  void preOpenFrameLargerThanInitialMaximumIsRejected() throws Exception {
    try (Socket socket = connectedTransport()) {
      DataOutputStream output = new DataOutputStream(socket.getOutputStream());
      output.writeInt(513);
      output.writeByte(2);
      output.writeByte(0);
      output.writeShort(0);
      output.write(new byte[505]);
      output.flush();

      assertEventuallyClosed(socket);
    }
  }

  private Socket connectedTransport() throws Exception {
    Socket socket = new Socket("localhost", 5672);
    socket.setSoTimeout(3_000);
    socket.getOutputStream().write(AMQP_1_0);
    socket.getOutputStream().flush();

    byte[] header = socket.getInputStream().readNBytes(8);
    assertEquals(8, header.length);
    assertArrayEquals(AMQP_1_0, header);
    return socket;
  }

  private void assertEventuallyClosed(Socket socket) throws Exception {
    InputStream input = socket.getInputStream();
    try {
      while (input.read() != -1) {
        // A peer may send a final error frame before closing.
      }
    } catch (SocketTimeoutException timeout) {
      fail("Server did not close after receiving a malformed AMQP frame header");
    }
  }
}
