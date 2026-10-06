/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.amqp;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.test.BaseTestConfig;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
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
class Amqp10LifecycleConformanceTest extends BaseTestConfig {

  private static final int PORT = 5672;
  private static final byte[] AMQP_1_0 = new byte[]{'A', 'M', 'Q', 'P', 0, 1, 0, 0};
  private static final String SOURCE =
      "https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-transport-v1.0-os.html";

  @Test
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 sections 2.4.1 and 2.7.1: OPEN is the first connection performative and uses channel 0",
      source = SOURCE)
  void openNegotiationReturnsOpenOnChannelZero() throws Exception {
    try (Socket socket = connectedTransport()) {
      sendFrame(socket, 0, openPerformative("maps-conformance-open"));

      Frame response = readFrame(socket);
      assertEquals(0, response.type());
      assertEquals(0, response.channel());
      assertEquals(0x10, response.descriptorCode());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 section 2.5.1: BEGIN establishes a session after OPEN",
      source = SOURCE)
  void beginEstablishesSession() throws Exception {
    try (Socket socket = connectedTransport()) {
      sendFrame(socket, 0, openPerformative("maps-conformance-begin"));
      assertEquals(0x10, readFrame(socket).descriptorCode());

      sendFrame(socket, 0, beginPerformative());
      Frame response = readFrame(socket);

      assertEquals(0, response.type());
      assertEquals(0, response.channel());
      assertEquals(0x11, response.descriptorCode());
    }
  }

  @Test
  @Disabled("Known conformance gap: MSG-397")
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 sections 2.4.3 and 2.7.9: CLOSE completes the connection shutdown handshake",
      source = SOURCE)
  void closeReceivesPeerClose() throws Exception {
    try (Socket socket = connectedTransport()) {
      sendFrame(socket, 0, openPerformative("maps-conformance-close"));
      assertEquals(0x10, readFrame(socket).descriptorCode());

      sendFrame(socket, 0, closePerformative());
      assertEquals(0x18, readFrame(socket).descriptorCode());
    }
  }

  @Test
  @Disabled("Known conformance gap: MSG-397")
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 sections 2.4.1 and 2.7.1: OPEN must be the first frame and can only use channel 0",
      source = SOURCE)
  void beginBeforeOpenIsRejected() throws Exception {
    try (Socket socket = connectedTransport()) {
      sendFrame(socket, 0, beginPerformative());
      assertEventuallyClosed(socket);
    }
  }

  @Test
  @Disabled("Known conformance gap: MSG-397")
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 section 2.4.1: OPEN can only be sent on channel 0",
      source = SOURCE)
  void openOnNonZeroChannelIsRejected() throws Exception {
    try (Socket socket = connectedTransport()) {
      sendFrame(socket, 1, openPerformative("maps-conformance-bad-channel"));
      assertEventuallyClosed(socket);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 section 2.4.5: peers MUST accept empty frames on valid channels after OPEN",
      source = SOURCE)
  void emptyHeartbeatFrameIsAcceptedAfterOpen() throws Exception {
    try (Socket socket = connectedTransport()) {
      sendFrame(socket, 0, openPerformative("maps-conformance-heartbeat"));
      assertEquals(0x10, readFrame(socket).descriptorCode());

      DataOutputStream output = new DataOutputStream(socket.getOutputStream());
      output.writeInt(8);
      output.writeByte(2);
      output.writeByte(0);
      output.writeShort(0);
      output.flush();

      sendFrame(socket, 0, beginPerformative());
      Frame response = readFrame(socket);
      assertEquals(0x11, response.descriptorCode());
    }
  }

  private void assertEventuallyClosed(Socket socket) throws Exception {
    socket.setSoTimeout(2_000);
    try {
      while (socket.getInputStream().read() != -1) {
        // Peer may emit CLOSE/error before closing the transport.
      }
    } catch (SocketTimeoutException timeout) {
      fail("Server did not close after invalid AMQP connection state");
    }
  }

  private Socket connectedTransport() throws Exception {
    Socket socket = new Socket("localhost", PORT);
    socket.setSoTimeout(3_000);
    socket.getOutputStream().write(AMQP_1_0);
    socket.getOutputStream().flush();
    byte[] header = socket.getInputStream().readNBytes(8);
    assertEquals(8, header.length);
    assertArrayEquals(AMQP_1_0, header);
    return socket;
  }

  private void sendFrame(Socket socket, int channel, byte[] performative) throws Exception {
    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
    output.writeInt(8 + performative.length);
    output.writeByte(2);
    output.writeByte(0);
    output.writeShort(channel);
    output.write(performative);
    output.flush();
  }

  private Frame readFrame(Socket socket) throws Exception {
    DataInputStream input = new DataInputStream(socket.getInputStream());
    int size = input.readInt();
    int dataOffset = input.readUnsignedByte();
    int type = input.readUnsignedByte();
    int channel = input.readUnsignedShort();

    assertTrue(size >= 8);
    assertTrue(dataOffset >= 2);

    byte[] remainder = input.readNBytes(size - 8);
    assertEquals(size - 8, remainder.length);

    int bodyOffset = dataOffset * 4 - 8;
    assertTrue(bodyOffset >= 0 && bodyOffset + 2 < remainder.length);
    assertEquals(0x00, remainder[bodyOffset] & 0xff);
    assertEquals(0x53, remainder[bodyOffset + 1] & 0xff);

    return new Frame(type, channel, remainder[bodyOffset + 2] & 0xff);
  }

  private byte[] openPerformative(String containerId) {
    byte[] id = containerId.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(0x00);
    out.write(0x53);
    out.write(0x10);
    out.write(0xC0);
    out.write(1 + 2 + id.length);
    out.write(1);
    out.write(0xA1);
    out.write(id.length);
    out.writeBytes(id);
    return out.toByteArray();
  }

  private byte[] beginPerformative() {
    return new byte[]{
        0x00, 0x53, 0x11,
        (byte) 0xC0, 0x08, 0x04,
        0x40,
        0x52, 0x01,
        0x52, 0x64,
        0x52, 0x64
    };
  }

  private byte[] closePerformative() {
    return new byte[]{0x00, 0x53, 0x18, 0x45};
  }

  private record Frame(int type, int channel, int descriptorCode) {
  }
}
