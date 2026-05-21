/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.io.security;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import io.mapsmessaging.network.io.security.impl.signature.PrependerSignatureManager;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PacketIntegrityVerificationTests {

  private static final int PAYLOAD_LENGTH = 1024;
  private static final int PACKET_CAPACITY = 2048;

  @ParameterizedTest
  @MethodSource
  void verify_happyPath_returnsOk(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    TestContext context = createContext(algorithm, stamper);

    int packetLengthBeforeVerify = context.secured.limit();
    int signatureSize = context.integrity.size();

    VerificationResult result = context.integrity.verify(context.secured);

    Assertions.assertTrue(result.isValid());
    Assertions.assertEquals(FailureReason.OK, result.getReason());
    Assertions.assertEquals(algorithm, result.getAlgorithm());
    Assertions.assertEquals(packetLengthBeforeVerify, result.getPacketLength());
    Assertions.assertEquals(signatureSize, result.getSignatureSize());

    // Optional but actually useful: confirm unwrap happened
    Assertions.assertEquals(packetLengthBeforeVerify - signatureSize, context.secured.limit());
  }

  @ParameterizedTest
  @MethodSource
  void verify_nullPacket_returnsPacketNull(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    PacketIntegrity integrity = createIntegrity(algorithm, stamper);

    VerificationResult result = integrity.verify(null);

    Assertions.assertFalse(result.isValid());
    Assertions.assertEquals(FailureReason.PACKET_NULL, result.getReason());
    Assertions.assertEquals(algorithm, result.getAlgorithm());
  }

  @ParameterizedTest
  @MethodSource
  void verify_tamperPayload_returnsSignatureMismatch(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    TestContext context = createContext(algorithm, stamper);

    Packet tampered = clonePacket(context.secured);
    flipOnePayloadByte(tampered, context.integrity.size(), stamper);

    VerificationResult result = context.integrity.verify(tampered);

    Assertions.assertFalse(result.isValid());
    Assertions.assertEquals(FailureReason.SIGNATURE_MISMATCH, result.getReason());
  }

  @ParameterizedTest
  @MethodSource
  void verify_tamperSignature_returnsSignatureMismatch(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    TestContext context = createContext(algorithm, stamper);

    Packet tampered = clonePacket(context.secured);
    flipOneSignatureByte(tampered, context.integrity.size(), stamper);

    VerificationResult result = context.integrity.verify(tampered);

    Assertions.assertFalse(result.isValid());
    Assertions.assertEquals(FailureReason.SIGNATURE_MISMATCH, result.getReason());
  }

  @ParameterizedTest
  @MethodSource
  void verify_truncatedPacket_returnsPacketTooShort(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    TestContext context = createContext(algorithm, stamper);

    int signatureSize = context.integrity.size();
    Assertions.assertTrue(signatureSize > 0);

    Packet truncated = new Packet(PACKET_CAPACITY, false);
    truncated.put((byte) 0x01);
    truncated.flip();
    truncated.limit(signatureSize - 1);

    VerificationResult result = context.integrity.verify(truncated);

    Assertions.assertFalse(result.isValid());
    Assertions.assertEquals(FailureReason.PACKET_TOO_SHORT, result.getReason());
  }

  private static Stream<Arguments> verify_happyPath_returnsOk() {
    return parameters();
  }

  private static Stream<Arguments> verify_nullPacket_returnsPacketNull() {
    return parameters();
  }

  private static Stream<Arguments> verify_tamperPayload_returnsSignatureMismatch() {
    return parameters();
  }

  private static Stream<Arguments> verify_tamperSignature_returnsSignatureMismatch() {
    return parameters();
  }

  private static Stream<Arguments> verify_truncatedPacket_returnsPacketTooShort() {
    return parameters();
  }

  private static Stream<Arguments> parameters() {
    List<Arguments> list = new ArrayList<>();
    for (String algorithm : PacketIntegrityFactory.getInstance().getAlgorithms()) {
      list.add(arguments(algorithm, new PrependerSignatureManager()));
      list.add(arguments(algorithm, new AppenderSignatureManager()));
    }
    return list.stream();
  }

  private static PacketIntegrity createIntegrity(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    Random random = new Random(1234567L);
    byte[] key = new byte[100];
    random.nextBytes(key);

    return PacketIntegrityFactory.getInstance().getPacketIntegrity(algorithm, stamper, key);
  }

  private static TestContext createContext(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    PacketIntegrity integrity = createIntegrity(algorithm, stamper);

    Packet payload = createPayloadPacket();
    Packet secured = integrity.secure(clonePacket(payload));

    return new TestContext(payload, integrity, secured);
  }

  private static Packet createPayloadPacket() {
    Packet packet = new Packet(PACKET_CAPACITY, false);
    for (int index = 0; index < PAYLOAD_LENGTH; index++) {
      packet.put((byte) (index & 0xFF));
    }
    packet.flip();
    return packet;
  }

  private static Packet clonePacket(Packet source) {
    Packet clone = new Packet(source.capacity(), false);

    int limit = source.limit();
    for (int index = 0; index < limit; index++) {
      clone.put(source.get(index));
    }
    clone.flip();
    clone.limit(limit);

    return clone;
  }

  private static void flipOnePayloadByte(Packet packet, int signatureSize, SignatureManager stamper) {
    int payloadStart;
    if (stamper instanceof PrependerSignatureManager) {
      payloadStart = signatureSize;
    } else {
      payloadStart = 0;
    }

    int payloadEnd;
    if (stamper instanceof PrependerSignatureManager) {
      payloadEnd = packet.limit();
    } else {
      payloadEnd = packet.limit() - signatureSize;
    }

    Assertions.assertTrue(payloadEnd > payloadStart, "No payload region available to tamper");

    int indexToFlip = payloadStart + ((payloadEnd - payloadStart) / 2);
    byte value = packet.get(indexToFlip);
    packet.put(indexToFlip, (byte) (value ^ 0x01));
  }

  private static void flipOneSignatureByte(Packet packet, int signatureSize, SignatureManager stamper) {
    Assertions.assertTrue(signatureSize > 0, "Signature size must be > 0");

    int signatureStart;
    if (stamper instanceof PrependerSignatureManager) {
      signatureStart = 0;
    } else {
      signatureStart = packet.limit() - signatureSize;
    }

    int indexToFlip = signatureStart + (signatureSize / 2);
    byte value = packet.get(indexToFlip);
    packet.put(indexToFlip, (byte) (value ^ 0x01));
  }

  private static class TestContext {
    private final Packet payload;
    private final PacketIntegrity integrity;
    private final Packet secured;

    private TestContext(Packet payload, PacketIntegrity integrity, Packet secured) {
      this.payload = payload;
      this.integrity = integrity;
      this.secured = secured;
    }
  }
}
