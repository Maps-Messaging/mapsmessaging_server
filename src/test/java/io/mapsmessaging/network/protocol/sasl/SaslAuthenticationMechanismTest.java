/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
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

package io.mapsmessaging.network.protocol.sasl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SaslAuthenticationMechanismTest {

  @Test
  void supported_mechanisms_are_explicitly_allowlisted() {
    assertTrue(SaslAuthenticationMechanism.isSupportedMechanism("SCRAM-SHA-256"));
    assertTrue(SaslAuthenticationMechanism.isSupportedMechanism("PLAIN"));
    assertFalse(SaslAuthenticationMechanism.isSupportedMechanism("SCRAM-SHA-1"));
    assertFalse(SaslAuthenticationMechanism.isSupportedMechanism("SCRAM-SHA-512"));
    assertFalse(SaslAuthenticationMechanism.isSupportedMechanism("DIGEST-MD5"));
    assertFalse(SaslAuthenticationMechanism.isSupportedMechanism("CRAM-MD5"));
    assertFalse(SaslAuthenticationMechanism.isSupportedMechanism("plain"));
  }

  @Test
  void plain_requires_a_protected_transport() {
    assertTrue(SaslAuthenticationMechanism.isProtectedTransport("ssl://localhost:8883"));
    assertTrue(SaslAuthenticationMechanism.isProtectedTransport("dtls://localhost:1884"));
    assertTrue(SaslAuthenticationMechanism.isProtectedTransport("wss://localhost:9443/mqtt"));
    assertFalse(SaslAuthenticationMechanism.isProtectedTransport("tcp://localhost:1883"));
    assertFalse(SaslAuthenticationMechanism.isProtectedTransport("udp://localhost:1884"));
    assertFalse(SaslAuthenticationMechanism.isProtectedTransport("ws://localhost:8080/mqtt"));
    assertFalse(SaslAuthenticationMechanism.isProtectedTransport("not a url"));
    assertFalse(SaslAuthenticationMechanism.isProtectedTransport(null));
  }
}
