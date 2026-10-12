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

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import io.mapsmessaging.dto.rest.config.auth.SaslConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.network.protocol.sasl.SaslAuthenticationMechanism;
import lombok.Getter;
import org.apache.qpid.proton.engine.Sasl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SaslManager {

  private final Sasl sasl;
  private final SaslAuthenticationMechanism saslAuthenticationMechanism;
  @Getter
  private String username;

  public SaslManager(ProtonEngine protonEngine) throws IOException {
    this(protonEngine.getTransport().sasl(),
        buildMechanism(protonEngine.getProtocol().getEndPoint().getConfig()));
  }

  // Package-private to test SASL transport progress without requiring real credentials.
  SaslManager(Sasl sasl, SaslAuthenticationMechanism mechanism) {
    this.sasl = sasl;
    this.saslAuthenticationMechanism = mechanism;
    String mechanismName = mechanism == null ? "ANONYMOUS" : mechanism.getMechanism();
    sasl.setMechanisms(mechanismName);
    sasl.server();
    if ("ANONYMOUS".equalsIgnoreCase(mechanismName)) {
      sasl.done(Sasl.PN_SASL_OK);
    }
  }

  public void challenge() throws IOException {
    if (saslAuthenticationMechanism == null) {
      return;
    }
    int pending = Math.max(0, sasl.pending());
    if (pending == 0) {
      // SCRAM is client-first. Do not advance the mechanism or consume a
      // challenge attempt until Proton actually supplies client bytes.
      return;
    }
    byte[] challenge = new byte[pending];
    int received = sasl.recv(challenge, 0, challenge.length);
    if (received <= 0) {
      return;
    }
    if (received != challenge.length) {
      challenge = java.util.Arrays.copyOf(challenge, received);
    }
    byte[] response = saslAuthenticationMechanism.challenge(challenge);
    if (response != null && response.length > 0) {
      sasl.send(response, 0, response.length);
    }
    if (saslAuthenticationMechanism.complete()) {
      sasl.done(Sasl.SaslOutcome.PN_SASL_OK);
      username = saslAuthenticationMechanism.getUsername();
    }
  }

  private static SaslAuthenticationMechanism buildMechanism(EndPointServerConfigDTO config) throws IOException {
    SaslAuthenticationMechanism authenticationContext = null;
    SaslConfigDTO saslConfig = config.getSaslConfig();
    if (saslConfig != null) {
      Map<String, String> props = new HashMap<>();
      props.put(javax.security.sasl.Sasl.QOP, "auth");
      String serverName = saslConfig.getRealmName();
      String mechanism = saslConfig.getMechanism();
      authenticationContext = new SaslAuthenticationMechanism(mechanism, serverName, "AMQP", props, config);
    }
    return authenticationContext;
  }

  public boolean isDone() {
    // SASL-OUTCOME is emitted exactly once by challenge() on completion.
    // Repeating done() on each transport poll can disrupt the AMQP handoff.
    return saslAuthenticationMechanism == null
        || Sasl.SaslOutcome.PN_SASL_OK.equals(sasl.getOutcome());
  }

  public void close() throws IOException {
    if (saslAuthenticationMechanism != null) {
      saslAuthenticationMechanism.close();
    }
  }

}
