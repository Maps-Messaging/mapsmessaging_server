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

package io.mapsmessaging.network.protocol.sasl;

import io.mapsmessaging.dto.rest.config.auth.SaslConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.network.AuthenticationMechanism;
import io.mapsmessaging.security.identity.IdentityLookup;
import io.mapsmessaging.security.identity.IdentityLookupFactory;
import lombok.Getter;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.util.Map;

public class SaslAuthenticationMechanism implements AuthenticationMechanism {

  private final SaslServer saslServer;

  @Getter
  private final String mechanism;

  public SaslAuthenticationMechanism(String mechanism, String serverName, String protocol, Map<String, String> props, EndPointServerConfigDTO properties) throws IOException {
    SaslConfigDTO saslConfig = properties.getSaslConfig();
    IdentityLookup identityLookup;
    ServerCallbackHandler serverCallbackHandler;
    if (saslConfig.getIdentityProvider().equalsIgnoreCase("system")) {
      identityLookup = IdentityLookupFactory.getInstance().getSiteWide("system");
    } else {
      identityLookup = IdentityLookupFactory.getInstance().get(saslConfig.getIdentityProvider(), saslConfig.getSaslEntries());
    }
    if(identityLookup == null){
      throw new SaslException("Unable to locate identity look up mechanism for " + saslConfig.getSaslEntries());
    }
    serverCallbackHandler = new ServerCallbackHandler(serverName, identityLookup);
    saslServer = Sasl.createSaslServer(mechanism, protocol, serverName, props, serverCallbackHandler);
    if (saslServer == null) {
      throw new IOException("Unsupported Sasl Mechanism : " + mechanism);
    }
    this.mechanism = mechanism;
  }

  @Override
  public byte[] challenge(byte[] challenge) throws IOException {
    return saslServer.evaluateResponse(challenge);
  }

  @Override
  public boolean complete() {
    if (saslServer == null) {
      return true;
    }
    return saslServer.isComplete();
  }

  public String getName(){
    return mechanism;
  }

  public String getUsername() {
    if(saslServer.isComplete()) {
      return saslServer.getAuthorizationID();
    }
    return null;
  }
}
