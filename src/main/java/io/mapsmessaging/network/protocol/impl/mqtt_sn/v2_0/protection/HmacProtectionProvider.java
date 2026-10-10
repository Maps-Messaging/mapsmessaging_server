/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

/** Authentication-only provider sharing the AEAD provider contract. */
public final class HmacProtectionProvider implements MqttSn2CryptoProvider {
  @Override public Set<Integer> supportedSchemes() { return Set.of(0, 1); }
  @Override public int tagLength(int scheme) { algorithm(scheme); return 32; }

  @Override public ProtectedPayload protect(int scheme, SecretKey key, byte[] nonce,
      byte[] authenticatedPrefix, byte[] plaintext) throws GeneralSecurityException {
    Mac mac = Mac.getInstance(algorithm(scheme));
    mac.init(key);
    mac.update(authenticatedPrefix);
    return new ProtectedPayload(plaintext, mac.doFinal(plaintext));
  }

  @Override public byte[] unprotect(int scheme, SecretKey key, byte[] nonce,
      byte[] authenticatedPrefix, ProtectedPayload payload) throws GeneralSecurityException {
    ProtectedPayload expected = protect(scheme, key, nonce, authenticatedPrefix, payload.protectedBytes());
    if (!MessageDigest.isEqual(expected.tag(), payload.tag())) {
      throw new GeneralSecurityException("MQTT-SN HMAC authentication failed");
    }
    return payload.protectedBytes();
  }

  private static String algorithm(int scheme) {
    return switch (scheme) {
      case 0 -> "HmacSHA256";
      case 1 -> "HmacSHA3-256";
      default -> throw new IllegalArgumentException("Unsupported HMAC scheme");
    };
  }
}
