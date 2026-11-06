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
  // Protocol value container; payload bytes are cloned and compared explicitly in tests.
  @SuppressWarnings("java:S6218")
  record ProtectedPayload(byte[] protectedBytes, byte[] tag) {
    public ProtectedPayload {
      protectedBytes = protectedBytes.clone();
      tag = tag.clone();
    }
    @Override public byte[] protectedBytes() { return protectedBytes.clone(); }
    @Override public byte[] tag() { return tag.clone(); }
  }
}
