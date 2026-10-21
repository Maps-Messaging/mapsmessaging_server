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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.protection;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/** CSD01 authentication-only AES-CMAC 128, 192 and 256 (schemes 0x02–0x04). */
public final class BouncyCastleCmacProtectionProvider implements MqttSn2CryptoProvider {
  private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

  @Override public Set<Integer> supportedSchemes() { return Set.of(2, 3, 4); }

  @Override public int tagLength(int scheme) {
    validate(scheme);
    return 16;
  }

  @Override public ProtectedPayload protect(int scheme, SecretKey key, byte[] nonce,
      byte[] authenticatedPrefix, byte[] plaintext) throws GeneralSecurityException {
    validate(scheme);
    if (!"AES".equalsIgnoreCase(key.getAlgorithm())) {
      throw new GeneralSecurityException("CMAC requires an AES SecretKey");
    }
    byte[] encoded = key.getEncoded();
    if (encoded != null && encoded.length != 16 + (scheme - 2) * 8) {
      throw new GeneralSecurityException("CMAC key length does not match scheme");
    }
    Mac mac = Mac.getInstance("AESCMAC", PROVIDER);
    mac.init(key);
    mac.update(authenticatedPrefix);
    return new ProtectedPayload(plaintext, mac.doFinal(plaintext));
  }

  @Override public byte[] unprotect(int scheme, SecretKey key, byte[] nonce,
      byte[] authenticatedPrefix, ProtectedPayload payload) throws GeneralSecurityException {
    if (payload.tag().length != tagLength(scheme)) {
      throw new GeneralSecurityException("Invalid CMAC authentication tag length");
    }
    byte[] expected = protect(scheme, key, nonce, authenticatedPrefix,
        payload.protectedBytes()).tag();
    if (!MessageDigest.isEqual(expected, payload.tag())) {
      throw new GeneralSecurityException("CMAC authentication failed");
    }
    return payload.protectedBytes();
  }

  private static void validate(int scheme) {
    if (scheme < 2 || scheme > 4) {
      throw new IllegalArgumentException("Unsupported AES-CMAC scheme");
    }
  }
}
