/*
 *
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.mqttsn.MqttSnCodec;
import io.mapsmessaging.mqttsn.protection.ProtectionCodec;
import io.mapsmessaging.mqttsn.protection.ProtectionEnvelope;
import io.mapsmessaging.mqttsn.protection.bc.BouncyCastleProtectionProvider;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2ControlCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection.MqttSn2CryptoProviders;
import io.mapsmessaging.storage.delivery.TargetDataTempDirFactory;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Two independent implementations: mqtt-sn-2-client Bouncy Castle ProtectionCodec
 * and server ServiceLoader protection, in both directions for all CSD01 schemes.
 */
class MqttSn2ReferenceClientProtectionInteropTest {
  @TempDir(factory = TargetDataTempDirFactory.class) Path storage;
  private static final byte[] SENDER = HexFormat.of().parseHex("aabbccddeeff0011");
  private static final byte[] RANDOM = {3, 4, 5, 6};

  private static byte[] keyBytes(int scheme) {
    int length = switch (scheme) {
      case 0, 1 -> 32;
      case 2, 0x40, 0x43, 0x46 -> 16;
      case 3, 0x41, 0x44, 0x47 -> 24;
      case 4, 0x42, 0x45, 0x48, 0x49 -> 32;
      default -> throw new IllegalArgumentException("Unknown scheme");
    };
    byte[] value = new byte[length];
    for (int i = 0; i < length; i++) value[i] = (byte) (i + 1);
    return value;
  }

  private static SecretKey secretKey(int scheme) {
    String algorithm = switch (scheme) {
      case 0 -> "HmacSHA256";
      case 1 -> "HmacSHA3-256";
      case 0x49 -> "ChaCha20";
      default -> "AES";
    };
    return new SecretKeySpec(keyBytes(scheme), algorithm);
  }

  private static byte[] bytes(ByteBuffer value) {
    ByteBuffer copy = value.asReadOnlyBuffer();
    byte[] out = new byte[copy.remaining()];
    copy.get(out);
    return out;
  }

  @Test
  void referenceClientTruncatedHmacAndCmacTagsAuthenticateAndRejectTampering() throws Exception {
    for (int scheme : new int[] {0, 1, 2, 3, 4}) {
      var provider = new BouncyCastleProtectionProvider(ctx -> keyBytes(ctx.scheme()));
      byte[] inner = bytes(MqttSn2ControlCodec.encodePingResponse(31, null));
      byte[] wire = ProtectionCodec.encode(new ProtectionEnvelope(
          scheme, 4, SENDER, RANDOM, new byte[0],
          new byte[]{0, 0, 0, 1}, inner), provider);
      try (var store = new MqttSn2IndexedCounterStore(storage.resolve("truncate-" + scheme))) {
        var policy = new MqttSn2ProtectionSession(
            new MqttSn2CryptoProviders(), (sender, requestedScheme) ->
            secretKey(requestedScheme), store, Set.of(scheme), scheme, SENDER);
        byte[] corrupted = wire.clone();
        corrupted[corrupted.length - 1] ^= 1;
        assertThrows(java.io.IOException.class,
            () -> policy.receive(ByteBuffer.wrap(corrupted)),
            "Corrupted reference client tag must fail before replay tracking");
        assertArrayEquals(inner, bytes(policy.receive(ByteBuffer.wrap(wire))));
      }
    }
  }

  @Test
  void independentClientAndServerCanProtectAndUnprotectEveryStandardScheme() throws Exception {
    for (int scheme : new int[] {0, 1, 2, 3, 4,
        0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49}) {
      var clientProvider = new BouncyCastleProtectionProvider(ctx -> keyBytes(ctx.scheme()));
      byte[] inner = bytes(MqttSn2ControlCodec.encodePingResponse(23, null));
      // First verify the reference client accepts the server's plain control framing.
      assertEquals(inner.length, MqttSnCodec.decode(ByteBuffer.wrap(inner)).packetLength());
      byte[] fromClient = ProtectionCodec.encode(new ProtectionEnvelope(
          scheme, 1, SENDER, RANDOM, new byte[0],
          new byte[] {0, 0, 0, 1}, inner), clientProvider);
      try (var store = new MqttSn2IndexedCounterStore(storage.resolve("scheme-" + scheme))) {
        var server = new MqttSn2ProtectionSession(
            new MqttSn2CryptoProviders(),
            (sender, requestedScheme) -> {
              assertArrayEquals(SENDER, sender);
              assertEquals(scheme, requestedScheme);
              return secretKey(requestedScheme);
            }, store, Set.of(scheme), scheme, SENDER);
        assertArrayEquals(inner, bytes(server.receive(ByteBuffer.wrap(fromClient))),
            "Client-to-server protection scheme " + scheme);
        byte[] fromServer = bytes(server.send(ByteBuffer.wrap(inner)));
        assertArrayEquals(inner, ProtectionCodec.decode(fromServer, clientProvider).mqttSnPacket(),
            "Server-to-client protection scheme " + scheme);
        assertThrows(Exception.class, () -> server.receive(ByteBuffer.wrap(fromClient)),
            "Client packet replay must fail");
      }
    }
  }
}
