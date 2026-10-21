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

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.security.certificates.CertificateManager;
import io.mapsmessaging.security.certificates.CertificateManagerFactory;
import io.mapsmessaging.security.certificates.SecretKeyManager;
import io.mapsmessaging.storage.delivery.TargetDataTempDirFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeyStoreProtectionKeyResolverTest {
  @TempDir(factory = TargetDataTempDirFactory.class) Path root;
  private static final byte[] SENDER = {1, 2, 3, 4, 5, 6, 7, 8};
  private static final char[] PASSWORD = "entry-password".toCharArray();

  private ConfigurationProperties configuration() {
    return new ConfigurationProperties(Map.of(
        "type", "JCEKS",
        "store", "file",
        "path", root.resolve("protection.jceks").toString(),
        "passphrase", "store-password"));
  }

  private void createKey(String alias, String algorithm) throws Exception {
    CertificateManager manager = CertificateManagerFactory.getInstance()
        .getManager(configuration());
    SecretKeyManager keys = assertInstanceOf(SecretKeyManager.class, manager);
    keys.addSecretKey(alias, PASSWORD, new SecretKeySpec(new byte[32], algorithm));
  }

  @Test void goodHmacKeyLoadsViaAuthenticationFactory() throws Exception {
    createKey("device-key", "HmacSHA256");
    KeyStoreProtectionKeyResolver resolver = new KeyStoreProtectionKeyResolver(
        configuration(), Map.of("0102030405060708:0", "device-key"), PASSWORD);
    byte[] key = resolver.resolve(SENDER, 0);
    assertArrayEquals(new byte[32], key);
    Arrays.fill(key, (byte) 1);
    assertArrayEquals(new byte[32], resolver.resolve(SENDER, 0),
        "Returned bytes must not mutate keystore key material");
  }

  @Test void badUnknownSecurityAssociationFailsClosed() throws Exception {
    createKey("device-key", "HmacSHA256");
    KeyStoreProtectionKeyResolver resolver = new KeyStoreProtectionKeyResolver(
        configuration(), Map.of("0102030405060708:0", "device-key"), PASSWORD);
    assertThrows(IOException.class, () -> resolver.resolve(SENDER, 1));
    assertThrows(IOException.class, () -> resolver.resolve(new byte[7], 0));
    assertThrows(IOException.class, () -> resolver.resolve(SENDER, 256));
  }

  @Test void murphyWrongEntryPasswordFailsClosed() throws Exception {
    createKey("device-key", "HmacSHA256");
    KeyStoreProtectionKeyResolver resolver = new KeyStoreProtectionKeyResolver(
        configuration(), Map.of("0102030405060708:0", "device-key"),
        "wrong-password".toCharArray());
    assertThrows(IOException.class, () -> resolver.resolve(SENDER, 0));
  }

  @Test void badAlgorithmCannotBeUsedAsHmacKey() throws Exception {
    createKey("aes-only", "AES");
    KeyStoreProtectionKeyResolver resolver = new KeyStoreProtectionKeyResolver(
        configuration(), Map.of("0102030405060708:0", "aes-only"), PASSWORD);
    assertEquals("AES", resolver.resolveSecretKey(SENDER, 0).getAlgorithm());
    assertThrows(IOException.class, () -> resolver.resolve(SENDER, 0),
        "AES key must not be silently reinterpreted as an HMAC key");
  }

  @Test void murphyMissingAliasFailsClosed() throws Exception {
    createKey("device-key", "HmacSHA256");
    KeyStoreProtectionKeyResolver resolver = new KeyStoreProtectionKeyResolver(
        configuration(), Map.of("0102030405060708:0", "missing"), PASSWORD);
    assertThrows(IOException.class, () -> resolver.resolve(SENDER, 0));
  }
}
