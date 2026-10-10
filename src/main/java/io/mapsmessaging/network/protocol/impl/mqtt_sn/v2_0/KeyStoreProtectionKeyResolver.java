/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.security.certificates.CertificateManager;
import io.mapsmessaging.security.certificates.CertificateManagerFactory;
import io.mapsmessaging.security.certificates.SecretKeyManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import javax.crypto.SecretKey;

/**
 * Resolve sender+scheme to a keystore alias, using Authentication library's
 * ServiceLoader-backed certificate manager and SecretKeyManager capability.
 * Keys and entry passwords are never encoded in the MQTT-SN wire envelope.
 */
public final class KeyStoreProtectionKeyResolver implements MqttSn2ProtectionVerifier.KeyResolver {
  private final SecretKeyManager secretKeys;
  private final Map<String, String> senderAliases;
  private final char[] entryPassword;

  public KeyStoreProtectionKeyResolver(ConfigurationProperties storeConfiguration,
      Map<String, String> senderAliases, char[] entryPassword) throws IOException {
    Objects.requireNonNull(storeConfiguration, "storeConfiguration");
    Objects.requireNonNull(senderAliases, "senderAliases");
    Objects.requireNonNull(entryPassword, "entryPassword");
    try {
      CertificateManager manager = CertificateManagerFactory.getInstance().getManager(storeConfiguration);
      if (!(manager instanceof SecretKeyManager keyManager)) {
        throw new IOException("Configured Authentication keystore does not support symmetric keys");
      }
      this.secretKeys = keyManager;
    } catch (GeneralSecurityException failure) {
      throw new IOException("Unable to open MQTT-SN protection keystore", failure);
    }
    this.senderAliases = Map.copyOf(senderAliases);
    this.entryPassword = entryPassword.clone();
  }

  /** Return the JCA key directly for AEAD providers and HSM-capable backends. */
  public SecretKey resolveSecretKey(byte[] senderIdentifier, int scheme) throws IOException {
    if (senderIdentifier == null || senderIdentifier.length != 8
        || scheme < 0 || scheme > 255) throw new IOException("Invalid sender or scheme");
    String identity = HexFormat.of().formatHex(senderIdentifier) + ":" + scheme;
    String alias = senderAliases.get(identity);
    if (alias == null || alias.isBlank()) throw new IOException("Unknown protection security association");
    try {
      SecretKey key = secretKeys.getSecretKey(alias, entryPassword);
      if (key == null) throw new IOException("Secret key lookup returned null");
      return key;
    } catch (GeneralSecurityException failure) {
      throw new IOException("Unable to resolve protection key", failure);
    }
  }

  /** Legacy HMAC verifier adapter until the provider API consumes SecretKey. */
  @Override
  public byte[] resolve(byte[] senderIdentifier, int scheme) throws IOException {
    if (scheme != 0 && scheme != 1) throw new IOException("HMAC resolver accepts schemes 0 and 1");
    SecretKey key = resolveSecretKey(senderIdentifier, scheme);
    String expected = scheme == 0 ? "HmacSHA256" : "HmacSHA3-256";
    if (!key.getAlgorithm().equalsIgnoreCase(expected)) {
      throw new IOException("Key algorithm does not match selected HMAC scheme");
    }
    byte[] encoded = key.getEncoded();
    if (encoded == null || encoded.length < 16) {
      throw new IOException("Missing or non-exportable HMAC key material");
    }
    return encoded;
  }
}
