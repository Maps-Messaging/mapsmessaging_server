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

class PacketIntegritySecurityTests {

  private static final int PAYLOAD_LENGTH = 1024;
  private static final int PACKET_CAPACITY = 2048;

  @ParameterizedTest
  @MethodSource
  void secure_happyPath_isSecure_andSizeMatches(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    TestContext context = createContext(algorithm, stamper);

    Assertions.assertEquals(PAYLOAD_LENGTH + context.integrity.size(), context.secured.limit());
    Assertions.assertTrue(context.integrity.isSecure(context.secured));
  }

  @ParameterizedTest
  @MethodSource
  void verify_failsWithWrongKey(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    TestContext context = createContext(algorithm, stamper);

    byte[] wrongKey = new byte[context.key.length];
    new Random(9999L).nextBytes(wrongKey);

    PacketIntegrity wrongIntegrity = PacketIntegrityFactory.getInstance()
        .getPacketIntegrity(algorithm, stamper, wrongKey);

    // verify() / isSecure() may UNWRAP on success, so never verify against the shared context packet
    Packet securedForVerification = clonePacket(context.secured);

    boolean verifiesWithWrongKey = wrongIntegrity.isSecure(securedForVerification);
    if (!verifiesWithWrongKey) {
      return;
    }

    Assertions.assertTrue(
        isUnkeyedChecksumAlgorithm(algorithm),
        () -> "Verification succeeded with wrong key for algorithm '" + algorithm + "'. " +
            "If this is intentional, add it to isUnkeyedChecksumAlgorithm()."
    );

    Packet securedWithWrongKey = wrongIntegrity.secure(clonePacket(context.payload));

    // Compare against a non-mutated secured packet
    Assertions.assertTrue(
        packetsEqual(context.secured, securedWithWrongKey),
        () -> "Algorithm '" + algorithm + "' verified with wrong key but produced a different secured packet. " +
            "That suggests verification might not be checking the signature correctly."
    );
  }


  @ParameterizedTest
  @MethodSource
  void verify_failsIfPayloadIsModified(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    TestContext context = createContext(algorithm, stamper);

    Packet tampered = clonePacket(context.secured);
    flipOnePayloadByte(tampered, context.integrity.size(), stamper);

    Assertions.assertFalse(context.integrity.isSecure(tampered));
  }

  @ParameterizedTest
  @MethodSource
  void verify_failsIfSignatureIsModified(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    TestContext context = createContext(algorithm, stamper);

    Packet tampered = clonePacket(context.secured);
    flipOneSignatureByte(tampered, context.integrity.size(), stamper);

    Assertions.assertFalse(context.integrity.isSecure(tampered));
  }

  @ParameterizedTest
  @MethodSource
  void verify_failsIfSignatureIsTruncatedByOneByte(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    TestContext context = createContext(algorithm, stamper);

    int originalLimit = context.secured.limit();
    int signatureSize = context.integrity.size();

    Assertions.assertTrue(signatureSize > 0, "Signature size must be > 0");

    Packet truncated = clonePacket(context.secured);
    truncated.limit(originalLimit - 1);

    Assertions.assertFalse(context.integrity.isSecure(truncated));
  }

  private static Stream<Arguments> secure_happyPath_isSecure_andSizeMatches() {
    return parameters();
  }

  private static Stream<Arguments> verify_failsWithWrongKey() {
    return parameters();
  }

  private static Stream<Arguments> verify_failsIfPayloadIsModified() {
    return parameters();
  }

  private static Stream<Arguments> verify_failsIfSignatureIsModified() {
    return parameters();
  }

  private static Stream<Arguments> verify_failsIfSignatureIsTruncatedByOneByte() {
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

  private static TestContext createContext(String algorithm, SignatureManager stamper)
      throws NoSuchAlgorithmException, InvalidKeyException {

    Random random = new Random(1234567L);

    byte[] key = new byte[100];
    random.nextBytes(key);

    PacketIntegrity packetIntegrity = PacketIntegrityFactory.getInstance().getPacketIntegrity(algorithm, stamper, key);

    Packet payload = createPayloadPacket();
    Packet secured = packetIntegrity.secure(clonePacket(payload));

    return new TestContext(key, payload, packetIntegrity, secured);
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

  private static boolean packetsEqual(Packet a, Packet b) {
    if (a.limit() != b.limit()) {
      return false;
    }
    int limit = a.limit();
    for (int index = 0; index < limit; index++) {
      if (a.get(index) != b.get(index)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isUnkeyedChecksumAlgorithm(String algorithm) {
    if (algorithm == null) {
      return false;
    }
    return algorithm.equalsIgnoreCase("Adler32")
        || algorithm.equalsIgnoreCase("CRC32")
        || algorithm.equalsIgnoreCase("CRC32C");
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
    private final byte[] key;
    private final Packet payload;
    private final PacketIntegrity integrity;
    private final Packet secured;

    private TestContext(byte[] key, Packet payload, PacketIntegrity integrity, Packet secured) {
      this.key = key;
      this.payload = payload;
      this.integrity = integrity;
      this.secured = secured;
    }
  }
}
