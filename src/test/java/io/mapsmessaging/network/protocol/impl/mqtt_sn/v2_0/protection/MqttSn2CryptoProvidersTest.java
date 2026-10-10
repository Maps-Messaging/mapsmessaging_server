/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection;

import static org.junit.jupiter.api.Assertions.*;

import java.security.GeneralSecurityException;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class MqttSn2CryptoProvidersTest {
  private static final byte[] AAD = {0x12, 0x34, 0x56};
  private static final byte[] MESSAGE = {1, 2, 3, 4, 5};
  private static final byte[] NONCE = new byte[12];

  @Test void goodServiceLoaderDiscoversHmacAndAead() {
    MqttSn2CryptoProviders registry = new MqttSn2CryptoProviders();
    for (int scheme : new int[] {0, 1, 0x46, 0x47, 0x48, 0x49}) {
      assertTrue(registry.require(scheme).supportedSchemes().contains(scheme));
    }
  }

  @Test void goodAeadRoundTripsEverySupportedKeySize() throws Exception {
    MqttSn2CryptoProviders registry = new MqttSn2CryptoProviders();
    for (int scheme : new int[] {0x46, 0x47, 0x48, 0x49}) {
      int length = scheme == 0x49 ? 32 : 16 + 8 * (scheme - 0x46);
      var key = new SecretKeySpec(new byte[length], scheme == 0x49 ? "ChaCha20" : "AES");
      var provider = registry.require(scheme);
      var protectedValue = provider.protect(scheme, key, NONCE, AAD, MESSAGE);
      assertFalse(java.util.Arrays.equals(MESSAGE, protectedValue.protectedBytes()));
      assertEquals(16, protectedValue.tag().length);
      assertArrayEquals(MESSAGE, provider.unprotect(scheme, key, NONCE, AAD, protectedValue));
    }
  }

  @Test void goodHmacPassesWithoutEncryption() throws Exception {
    var provider = new MqttSn2CryptoProviders().require(0);
    var key = new SecretKeySpec(new byte[32], "HmacSHA256");
    var protectedValue = provider.protect(0, key, new byte[0], AAD, MESSAGE);
    assertArrayEquals(MESSAGE, protectedValue.protectedBytes());
    assertArrayEquals(MESSAGE, provider.unprotect(0, key, new byte[0], AAD, protectedValue));
  }

  @Test void badTamperedAeadTagAndAssociatedDataFail() throws Exception {
    var provider = new MqttSn2CryptoProviders().require(0x48);
    var key = new SecretKeySpec(new byte[32], "AES");
    var payload = provider.protect(0x48, key, NONCE, AAD, MESSAGE);
    byte[] badTag = payload.tag();
    badTag[0] ^= 1;
    assertThrows(GeneralSecurityException.class, () ->
        provider.unprotect(0x48, key, NONCE, AAD,
            new MqttSn2CryptoProvider.ProtectedPayload(payload.protectedBytes(), badTag)));
    assertThrows(GeneralSecurityException.class, () ->
        provider.unprotect(0x48, key, NONCE, new byte[]{0}, payload));
  }

  @Test void badInvalidAeadKeySizesAreRejected() {
    var provider = new MqttSn2CryptoProviders().require(0x46);
    assertThrows(GeneralSecurityException.class, () ->
        provider.protect(0x46, new SecretKeySpec(new byte[32], "AES"), NONCE, AAD, MESSAGE));
    assertThrows(GeneralSecurityException.class, () ->
        provider.protect(0x46, new SecretKeySpec(new byte[16], "AES"), new byte[8], AAD, MESSAGE));
  }

  @Test void murphyDuplicateSchemeAndUnknownSchemeFailAtStartup() {
    assertThrows(IllegalStateException.class, () ->
        new MqttSn2CryptoProviders(List.of(new HmacProtectionProvider(), new HmacProtectionProvider())));
    assertThrows(IllegalArgumentException.class, () -> new MqttSn2CryptoProviders().require(0x7f));
  }

  @Test void murphyNonceDerivationIsDeterministicAndPrefixBound() {
    assertEquals(12, MqttSn2NonceDeriver.derive(AAD, 12).length);
    assertEquals(13, MqttSn2NonceDeriver.derive(AAD, 13).length);
    assertFalse(java.util.Arrays.equals(
        MqttSn2NonceDeriver.derive(AAD, 12),
        MqttSn2NonceDeriver.derive(new byte[]{1}, 12)));
  }
}
