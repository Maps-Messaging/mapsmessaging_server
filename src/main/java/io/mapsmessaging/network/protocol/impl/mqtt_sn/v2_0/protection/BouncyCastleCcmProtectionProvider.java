/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import org.bouncycastle.jcajce.spec.AEADParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/** AES-CCM 64/128-bit tags with 128/192/256-bit keys (CSD01 0x40–0x45). */
public final class BouncyCastleCcmProtectionProvider implements MqttSn2CryptoProvider {
  private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

  @Override public Set<Integer> supportedSchemes() {
    return Set.of(0x40, 0x41, 0x42, 0x43, 0x44, 0x45);
  }

  @Override public int tagLength(int scheme) {
    validateScheme(scheme);
    return scheme < 0x43 ? 8 : 16;
  }

  @Override public ProtectedPayload protect(int scheme, SecretKey key, byte[] nonce,
      byte[] authenticatedPrefix, byte[] plaintext) throws GeneralSecurityException {
    Cipher cipher = cipher(scheme, key, nonce, Cipher.ENCRYPT_MODE);
    cipher.updateAAD(authenticatedPrefix);
    byte[] combined = cipher.doFinal(plaintext);
    int tagSize = tagLength(scheme);
    int length = combined.length - tagSize;
    return new ProtectedPayload(Arrays.copyOf(combined, length),
        Arrays.copyOfRange(combined, length, combined.length));
  }

  @Override public byte[] unprotect(int scheme, SecretKey key, byte[] nonce,
      byte[] authenticatedPrefix, ProtectedPayload payload) throws GeneralSecurityException {
    Cipher cipher = cipher(scheme, key, nonce, Cipher.DECRYPT_MODE);
    cipher.updateAAD(authenticatedPrefix);
    byte[] message = payload.protectedBytes();
    byte[] tag = payload.tag();
    if (tag.length != tagLength(scheme)) throw new GeneralSecurityException("Invalid CCM tag length");
    byte[] combined = Arrays.copyOf(message, message.length + tag.length);
    System.arraycopy(tag, 0, combined, message.length, tag.length);
    return cipher.doFinal(combined);
  }

  private static Cipher cipher(int scheme, SecretKey key, byte[] nonce, int mode)
      throws GeneralSecurityException {
    validateScheme(scheme);
    if (nonce == null || nonce.length != 13) {
      throw new GeneralSecurityException("MQTT-SN AES-CCM requires a 13-byte nonce");
    }
    if (!"AES".equalsIgnoreCase(key.getAlgorithm())) {
      throw new GeneralSecurityException("CCM requires AES key");
    }
    byte[] encoded = key.getEncoded();
    int keyBits = (128 + ((scheme - 0x40) % 3) * 64);
    if (encoded != null && encoded.length * 8 != keyBits) {
      throw new GeneralSecurityException("CCM key size does not match scheme");
    }
    Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding", PROVIDER);
    cipher.init(mode, key, new AEADParameterSpec(nonce, scheme < 0x43 ? 64 : 128));
    return cipher;
  }

  private static void validateScheme(int scheme) {
    if (scheme < 0x40 || scheme > 0x45) {
      throw new IllegalArgumentException("Unsupported AES-CCM scheme");
    }
  }
}
