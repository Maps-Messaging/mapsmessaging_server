/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection;

import java.security.GeneralSecurityException;
import java.util.Set;
import javax.crypto.SecretKey;

/** Stateless cryptographic algorithm family discovered with Java ServiceLoader. */
public interface MqttSn2CryptoProvider {
  Set<Integer> supportedSchemes();
  int tagLength(int scheme);
  /** The nonce is SHA-256 derived from the CSD01 authenticated prefix for AEAD. */
  ProtectedPayload protect(int scheme, SecretKey key, byte[] nonce,
      byte[] authenticatedPrefix, byte[] plaintext) throws GeneralSecurityException;
  byte[] unprotect(int scheme, SecretKey key, byte[] nonce,
      byte[] authenticatedPrefix, ProtectedPayload payload) throws GeneralSecurityException;
  record ProtectedPayload(byte[] protectedBytes, byte[] tag) {
    public ProtectedPayload {
      protectedBytes = protectedBytes.clone();
      tag = tag.clone();
    }
    @Override public byte[] protectedBytes() { return protectedBytes.clone(); }
    @Override public byte[] tag() { return tag.clone(); }
  }
}
