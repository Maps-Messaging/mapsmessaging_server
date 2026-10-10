/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2ControlCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection.MqttSn2CryptoProviders;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MqttSn2PluggableProtectionSessionTest {
  @TempDir Path root;
  private static final byte[] SENDER = {1, 2, 3, 4, 5, 6, 7, 8};

  private static SecretKey key(byte[] sender, int scheme) throws IOException {
    if (!java.util.Arrays.equals(sender, SENDER)) throw new IOException("Unknown sender");
    return switch (scheme) {
      case 0 -> new SecretKeySpec(new byte[32], "HmacSHA256");
      case 1 -> new SecretKeySpec(new byte[32], "HmacSHA3-256");
      case 0x40, 0x43 -> new SecretKeySpec(new byte[16], "AES");
      case 0x41, 0x44 -> new SecretKeySpec(new byte[24], "AES");
      case 0x42, 0x45 -> new SecretKeySpec(new byte[32], "AES");
      case 0x46 -> new SecretKeySpec(new byte[16], "AES");
      case 0x47 -> new SecretKeySpec(new byte[24], "AES");
      case 0x48 -> new SecretKeySpec(new byte[32], "AES");
      case 0x49 -> new SecretKeySpec(new byte[32], "ChaCha20");
      default -> throw new IOException("Unknown scheme");
    };
  }

  private static MqttSn2ProtectionSession policy(MqttSn2IndexedCounterStore counters, int scheme) {
    return new MqttSn2ProtectionSession(new MqttSn2CryptoProviders(),
        MqttSn2PluggableProtectionSessionTest::key, counters,
        Set.of(scheme), scheme, SENDER);
  }

  @Test void goodFullEnvelopeRoundTripEveryInstalledScheme() throws Exception {
    for (int scheme : new int[]{0, 1, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49}) {
      Path storeDir = root.resolve("scheme-" + scheme);
      try (MqttSn2IndexedCounterStore store = new MqttSn2IndexedCounterStore(storeDir)) {
        MqttSn2ProtectionSession session = policy(store, scheme);
        ByteBuffer packet = MqttSn2ControlCodec.encodePingResponse(123, null);
        ByteBuffer wire = session.send(packet);
        byte[] encoded = bytes(wire);
        int lengthFieldBytes = (encoded[0] & 0xff) == 1 ? 3 : 1;
        int protectionFlags = encoded[lengthFieldBytes + 1] & 0xff;
        assertEquals(1, protectionFlags >>> 4,
            "CSD01 §3.17.2.3-1 requires nominal authentication tag code 0x1 for AEAD");
        assertArrayEquals(bytes(packet), bytes(session.receive(wire.duplicate())));
        assertThrows(IOException.class, () -> session.receive(wire.duplicate()),
            "Replay must be rejected after successful authentication");
      }
    }
  }

  @Test void murphyTamperingMustNotAdvanceReplayState() throws Exception {
    try (MqttSn2IndexedCounterStore store = new MqttSn2IndexedCounterStore(root.resolve("tamper"))) {
      MqttSn2ProtectionSession policy = policy(store, 0x48);
      ByteBuffer signed = policy.send(MqttSn2ControlCodec.encodePingResponse(45, null));
      byte[] corrupt = bytes(signed);
      corrupt[corrupt.length - 1] ^= 1;
      assertThrows(IOException.class, () -> policy.receive(ByteBuffer.wrap(corrupt)));
      assertNotNull(policy.receive(signed.duplicate()), "Authentic packet remains acceptable");
    }
  }

  @Test void sadDisabledInboundSchemeFailsWithoutFallback() throws Exception {
    try (MqttSn2IndexedCounterStore first = new MqttSn2IndexedCounterStore(root.resolve("tx"))) {
      MqttSn2ProtectionSession signer = policy(first, 0x46);
      ByteBuffer packet = signer.send(MqttSn2ControlCodec.encodePingResponse(3, null));
      try (MqttSn2IndexedCounterStore second = new MqttSn2IndexedCounterStore(root.resolve("rx"))) {
        MqttSn2ProtectionSession receiver = policy(second, 0x49);
        assertThrows(IOException.class, () -> receiver.receive(packet.duplicate()));
      }
    }
  }

  @Test void murphyRestartRejectsAuthenticatedReplay() throws Exception {
    Path directory = root.resolve("restart");
    byte[] packet;
    try (MqttSn2IndexedCounterStore store = new MqttSn2IndexedCounterStore(directory)) {
      MqttSn2ProtectionSession session = policy(store, 0x49);
      packet = bytes(session.send(MqttSn2ControlCodec.encodePingResponse(9, null)));
      assertNotNull(session.receive(ByteBuffer.wrap(packet)));
    }
    try (MqttSn2IndexedCounterStore restarted = new MqttSn2IndexedCounterStore(directory)) {
      assertThrows(IOException.class,
          () -> policy(restarted, 0x49).receive(ByteBuffer.wrap(packet)));
    }
  }

  private static byte[] bytes(ByteBuffer frame) {
    ByteBuffer copy = frame.asReadOnlyBuffer();
    byte[] out = new byte[copy.remaining()];
    copy.get(out);
    return out;
  }
}
