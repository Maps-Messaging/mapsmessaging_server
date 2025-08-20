package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

public final class CipherManager {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_TAG_BITS = 128;
  private static final int IV_BYTES = 12;

  private final SecretKey key;
  private final SecureRandom rng = new SecureRandom();

  /** Accepts any-length shared secret; hashed to 256-bit AES key with SHA-256. */
  public CipherManager(byte[] sharedSecret) {
    this.key = new SecretKeySpec(sha256(sharedSecret), "AES");
  }

  /** encrypted = IV(12) || ciphertext+tag */
  public byte[] encrypt(byte[] plaintext) {
    try {
      byte[] iv = new byte[IV_BYTES];
      rng.nextBytes(iv);

      Cipher c = Cipher.getInstance(TRANSFORMATION);
      c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] ct = c.doFinal(plaintext);

      ByteBuffer out = ByteBuffer.allocate(IV_BYTES + ct.length);
      out.put(iv).put(ct);
      return out.array();
    } catch (Exception e) {
      throw new RuntimeException("AES-GCM encrypt failed", e);
    }
  }

  /** Input must be IV(12) || ciphertext+tag */
  public byte[] decrypt(byte[] ivPlusCiphertext) {
    if (ivPlusCiphertext == null || ivPlusCiphertext.length < IV_BYTES + 16)
      throw new IllegalArgumentException("Ciphertext too short");
    try {
      byte[] iv = Arrays.copyOfRange(ivPlusCiphertext, 0, IV_BYTES);
      byte[] ct = Arrays.copyOfRange(ivPlusCiphertext, IV_BYTES, ivPlusCiphertext.length);

      Cipher c = Cipher.getInstance(TRANSFORMATION);
      c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
      return c.doFinal(ct);
    } catch (Exception e) {
      throw new RuntimeException("AES-GCM decrypt failed", e);
    }
  }

  private static byte[] sha256(byte[] in) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return md.digest(in);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
