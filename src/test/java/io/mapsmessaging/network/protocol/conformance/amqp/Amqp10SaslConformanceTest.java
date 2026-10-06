/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.amqp;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.test.BaseTestConfig;
import java.io.DataInputStream;
import java.net.Socket;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("conformance")
@Tag("conformance-core")
@Tag("amqp10")
class Amqp10SaslConformanceTest extends BaseTestConfig {

  private static final int SASL_PORT = 5674;
  private static final byte[] SASL_1_0 = new byte[]{'A', 'M', 'Q', 'P', 3, 1, 0, 0};
  private static final String SECURITY_SOURCE =
      "https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-security-v1.0-os.html";

  @Test
  @ProtocolRequirement(
      specification = "AMQP-1.0",
      value = "Part 5 sections 5.3, 5.3.1 and 5.3.2: negotiate SASL 1.0 and advertise mechanisms using a SASL frame",
      source = SECURITY_SOURCE)
  void authenticatedListenerNegotiatesSaslAndAdvertisesMechanisms() throws Exception {
    try (Socket socket = new Socket("localhost", SASL_PORT)) {
      socket.setSoTimeout(3_000);
      socket.getOutputStream().write(SASL_1_0);
      socket.getOutputStream().flush();

      byte[] returnedHeader = socket.getInputStream().readNBytes(8);
      assertEquals(8, returnedHeader.length,
          "Expected complete SASL protocol header, got " + Arrays.toString(returnedHeader));
      assertArrayEquals(SASL_1_0, returnedHeader);

      DataInputStream input = new DataInputStream(socket.getInputStream());
      int frameSize = input.readInt();
      int dataOffset = input.readUnsignedByte();
      int frameType = input.readUnsignedByte();
      input.readUnsignedShort();

      assertTrue(frameSize >= 11, "SASL-MECHANISMS frame is too small");
      assertTrue(dataOffset >= 2, "SASL frame data offset must include the 8-octet frame header");
      assertEquals(1, frameType, "SASL frames must use frame type 0x01");

      int remaining = frameSize - 8;
      byte[] frameRemainder = input.readNBytes(remaining);
      assertEquals(remaining, frameRemainder.length, "Expected complete SASL frame");

      int bodyOffset = dataOffset * 4 - 8;
      assertTrue(bodyOffset >= 0 && bodyOffset + 3 <= frameRemainder.length,
          "SASL frame does not contain a performative descriptor");
      assertEquals(0x00, frameRemainder[bodyOffset] & 0xff, "Expected AMQP described type");
      assertEquals(0x53, frameRemainder[bodyOffset + 1] & 0xff, "Expected smallulong descriptor");
      assertEquals(0x40, frameRemainder[bodyOffset + 2] & 0xff, "Expected SASL-MECHANISMS performative");
    }
  }
}
