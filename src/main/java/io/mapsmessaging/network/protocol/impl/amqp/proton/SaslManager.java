/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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
    saslAuthenticationMechanism = buildMechansim(protonEngine.getProtocol().getEndPoint().getConfig());
    sasl = protonEngine.getTransport().sasl();
    String mechanism = saslAuthenticationMechanism != null ? saslAuthenticationMechanism.getMechanism() : "ANONYMOUS";
    sasl.setMechanisms(mechanism);
    sasl.server();
    if (mechanism.equalsIgnoreCase("ANONYMOUS")) {
      sasl.done(Sasl.PN_SASL_OK);
    }
  }

  public void challenge() throws IOException {
    int pending = Math.max(0, sasl.pending());
    byte[] challenge;
    if (pending > 0) {
      challenge = new byte[pending];
      sasl.recv(challenge, 0, challenge.length);
    } else {
      challenge = new byte[0];
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

  private SaslAuthenticationMechanism buildMechansim(EndPointServerConfigDTO config) throws IOException {
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
    if (saslAuthenticationMechanism != null && saslAuthenticationMechanism.complete()) {
      sasl.done(Sasl.PN_SASL_OK);
    }

    return saslAuthenticationMechanism == null || sasl.getOutcome().equals(Sasl.SaslOutcome.PN_SASL_OK);
  }

}
