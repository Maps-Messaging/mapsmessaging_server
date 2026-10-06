/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.amqp;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.test.BaseTestConfig;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("amqp10")
class Amqp10HeaderConformanceTest extends BaseTestConfig {

  private static final String HOST = "localhost";
  private static final int PORT = 5672;
  private static final byte[] AMQP_1_0 = new byte[]{'A', 'M', 'Q', 'P', 0, 1, 0, 0};
  private static final String SOURCE =
      "https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-transport-v1.0-os.html";

  @Test
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 section 2.2 Version Negotiation: supported version header is echoed",
      source = SOURCE)
  void supportedProtocolVersionIsAccepted() throws Exception {
    try (Socket socket = connect()) {
      socket.getOutputStream().write(AMQP_1_0);
      socket.getOutputStream().flush();

      assertArrayEquals(AMQP_1_0, readHeader(socket.getInputStream()));
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 section 2.2 Version Negotiation: unsupported version returns supported header then closes",
      source = SOURCE)
  void unsupportedProtocolVersionReturnsSupportedVersionAndCloses() throws Exception {
    byte[] unsupported = new byte[]{'A', 'M', 'Q', 'P', 0, 1, 1, 0};

    try (Socket socket = connect()) {
      socket.getOutputStream().write(unsupported);
      socket.getOutputStream().flush();

      assertArrayEquals(AMQP_1_0, readHeader(socket.getInputStream()));
      assertConnectionCloses(socket);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 2 section 2.2 Version Negotiation: unacceptable protocol id returns acceptable header then closes",
      source = SOURCE)
  void unsupportedProtocolIdReturnsSupportedProtocolAndCloses() throws Exception {
    byte[] saslHeader = new byte[]{'A', 'M', 'Q', 'P', 3, 1, 0, 0};

    try (Socket socket = connect()) {
      socket.getOutputStream().write(saslHeader);
      socket.getOutputStream().flush();

      assertArrayEquals(AMQP_1_0, readHeader(socket.getInputStream()));
      assertConnectionCloses(socket);
    }
  }

  private Socket connect() throws Exception {
    Socket socket = new Socket(HOST, PORT);
    socket.setSoTimeout(3_000);
    return socket;
  }

  private byte[] readHeader(InputStream input) throws Exception {
    byte[] header = input.readNBytes(8);
    assertEquals(8, header.length, "Expected complete 8-octet AMQP protocol header, got " + Arrays.toString(header));
    return header;
  }

  private void assertConnectionCloses(Socket socket) throws Exception {
    try {
      assertEquals(-1, socket.getInputStream().read(), "Server must close after rejecting the protocol header");
    } catch (SocketTimeoutException timeout) {
      fail("Server did not close after rejecting the protocol header");
    }
  }
}
