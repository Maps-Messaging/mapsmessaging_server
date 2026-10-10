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
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

/** AES-GCM and ChaCha20-Poly1305. No provider-global mutable cipher state. */
public final class JcaAeadProtectionProvider implements MqttSn2CryptoProvider {
  @Override public Set<Integer> supportedSchemes() { return Set.of(0x46, 0x47, 0x48, 0x49); }
  @Override public int tagLength(int scheme) { validateScheme(scheme); return 16; }

  @Override public ProtectedPayload protect(int scheme, SecretKey key, byte[] nonce,
      byte[] authenticatedPrefix, byte[] plaintext) throws GeneralSecurityException {
    Cipher cipher = cipher(scheme, key, nonce, Cipher.ENCRYPT_MODE);
    cipher.updateAAD(authenticatedPrefix);
    byte[] result = cipher.doFinal(plaintext);
    int dataSize = result.length - 16;
    return new ProtectedPayload(Arrays.copyOf(result, dataSize),
        Arrays.copyOfRange(result, dataSize, result.length));
  }

  @Override public byte[] unprotect(int scheme, SecretKey key, byte[] nonce,
      byte[] authenticatedPrefix, ProtectedPayload payload) throws GeneralSecurityException {
    Cipher cipher = cipher(scheme, key, nonce, Cipher.DECRYPT_MODE);
    cipher.updateAAD(authenticatedPrefix);
    byte[] encrypted = payload.protectedBytes();
    byte[] tag = payload.tag();
    if (tag.length != 16) throw new GeneralSecurityException("AEAD tag must be 16 bytes");
    byte[] input = Arrays.copyOf(encrypted, encrypted.length + tag.length);
    System.arraycopy(tag, 0, input, encrypted.length, tag.length);
    return cipher.doFinal(input);
  }

  private static Cipher cipher(int scheme, SecretKey key, byte[] nonce, int mode)
      throws GeneralSecurityException {
    validateScheme(scheme);
    if (nonce == null || nonce.length != 12) {
      throw new GeneralSecurityException("AEAD requires a 12-byte nonce");
    }
    int bits = scheme == 0x49 ? 256 : 128 + (scheme - 0x46) * 64;
    byte[] encoded = key.getEncoded();
    if (encoded != null && encoded.length * 8 != bits) {
      throw new GeneralSecurityException("Key size does not match AEAD scheme");
    }
    String expectedAlgorithm = scheme == 0x49 ? "ChaCha20" : "AES";
    if (!key.getAlgorithm().equalsIgnoreCase(expectedAlgorithm)) {
      throw new GeneralSecurityException("Key algorithm does not match scheme");
    }
    Cipher cipher = Cipher.getInstance(scheme == 0x49
        ? "ChaCha20-Poly1305" : "AES/GCM/NoPadding");
    if (scheme == 0x49) cipher.init(mode, key, new IvParameterSpec(nonce));
    else cipher.init(mode, key, new GCMParameterSpec(128, nonce));
    return cipher;
  }

  private static void validateScheme(int scheme) {
    if (scheme < 0x46 || scheme > 0x49)
      throw new IllegalArgumentException("Unsupported JCA AEAD scheme");
  }
}
