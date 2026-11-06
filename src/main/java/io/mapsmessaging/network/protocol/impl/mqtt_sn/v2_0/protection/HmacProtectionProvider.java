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

/** Authentication-only provider sharing the AEAD provider contract. */
public final class HmacProtectionProvider implements MqttSn2CryptoProvider {
  @Override
  public Set<Integer> supportedSchemes() {
    return Set.of(0, 1);
  }

  @Override
  public int tagLength(int scheme) {
    algorithm(scheme);
    return 32;
  }

  @Override
  public ProtectedPayload protect(int scheme, SecretKey key, byte[] nonce,
      byte[] authenticatedPrefix, byte[] plaintext) throws GeneralSecurityException {
    String algorithm = algorithm(scheme);
    if (!key.getAlgorithm().equalsIgnoreCase(algorithm)) {
      throw new GeneralSecurityException("HMAC key algorithm does not match protection scheme");
    }
    Mac mac = Mac.getInstance(algorithm);
    mac.init(key);
    mac.update(authenticatedPrefix);
    return new ProtectedPayload(plaintext, mac.doFinal(plaintext));
  }

  @Override
  public byte[] unprotect(int scheme, SecretKey key, byte[] nonce,
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
