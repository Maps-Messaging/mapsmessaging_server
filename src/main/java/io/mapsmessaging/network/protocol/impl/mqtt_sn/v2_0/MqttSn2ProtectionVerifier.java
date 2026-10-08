/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2FrameCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2ProtectionCodec;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Verifies MQTT-SN 2.0 CSD01 HMAC Protection Encapsulation (schemes 0x00 and 0x01).
 * Key selection is delegated to the caller; no legacy transport HMAC wrapper
 * is reused. The monotonic counter is checked only after successful MAC verification.
 *
 * <p>This is an authentication-only provider. AEAD schemes require a separate
 * configured provider and must not be silently treated as plaintext.</p>
 */
public final class MqttSn2ProtectionVerifier {
  @FunctionalInterface
  public interface KeyResolver {
    byte[] resolve(byte[] senderIdentifier, int scheme) throws IOException;
  }

  private final KeyResolver keys;
  private final Map<String, Long> lastCounters = new HashMap<>();

  public MqttSn2ProtectionVerifier(KeyResolver keys) {
    this.keys = Objects.requireNonNull(keys, "keys");
  }

  /**
   * Authenticate and return exactly one embedded MQTT-SN 2.0 frame.
   * This strict server profile requires a monotonic counter (2 or 4 bytes).
   * Counter persistence across server restarts is the deployer's responsibility;
   * do not enable for persistent protection sessions without durable counters.
   */
  public synchronized ByteBuffer verify(ByteBuffer wire) throws IOException {
    MqttSn2ProtectionCodec.Envelope envelope =
        MqttSn2ProtectionCodec.decode(wire, MqttSn2ProtectionVerifier::tagLength);
    int scheme = envelope.scheme();
    if (scheme != 0x00 && scheme != 0x01) {
      throw new IOException("Unsupported MQTT-SN 2.0 HMAC protection scheme");
    }
    byte[] counter = envelope.monotonicCounter();
    if (counter.length != 2 && counter.length != 4) {
      throw new IOException("Protection counter required by gateway security policy");
    }
    byte[] key = keys.resolve(envelope.senderIdentifier(), scheme);
    if (key == null || key.length < 16) {
      throw new IOException("Missing or invalid MQTT-SN 2.0 sender key");
    }
    byte[] expected;
    try {
      String algorithm = scheme == 0 ? "HmacSHA256" : "HmacSHA3-256";
      Mac mac = Mac.getInstance(algorithm);
      mac.init(new SecretKeySpec(key, algorithm));
      mac.update(envelope.authenticatedPrefix());
      expected = Arrays.copyOf(mac.doFinal(envelope.protectedPacket()), envelope.authenticationTag().length);
    } catch (GeneralSecurityException e) {
      throw new IOException("Unable to verify MQTT-SN 2.0 protection", e);
    }
    if (!MessageDigest.isEqual(expected, envelope.authenticationTag())) {
      throw new IOException("MQTT-SN 2.0 protection authentication failed");
    }

    byte[] inner = envelope.protectedPacket();
    MqttSn2FrameCodec.Frame packet = MqttSn2FrameCodec.decode(ByteBuffer.wrap(inner));
    if (packet.packetLength() != inner.length
        || packet.type() == MqttSn2PacketType.FORWARDER_ENCAPSULATION) {
      throw new IOException("Invalid protected inner packet");
    }
    String sender = Base64.getEncoder().encodeToString(envelope.senderIdentifier())
        + ":" + scheme;
    long value = 0;
    for (byte b : counter) value = (value << 8) | (b & 0xFFL);
    Long previous = lastCounters.get(sender);
    if (previous != null && value <= previous) {
      throw new IOException("MQTT-SN 2.0 protection replay detected");
    }
    lastCounters.put(sender, value);
    return ByteBuffer.wrap(inner).asReadOnlyBuffer();
  }

  private static int tagLength(int scheme, int code) throws IOException {
    if (scheme != 0 && scheme != 1) {
      throw new IOException("Unsupported protection scheme");
    }
    if (code == 0 || code == 1) return 32;
    if (code >= 4 && code <= 15) return code * 2;
    throw new IOException("Invalid HMAC tag length code");
  }
}
