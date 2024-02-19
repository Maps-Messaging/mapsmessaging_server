/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.sasl;

import io.mapsmessaging.configuration.ConfigurationProperties;
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

  private static final String IDENTITY_PROVIDER = "identityProvider";

  private final SaslServer saslServer;

  @Getter
  private final String mechanism;

  public SaslAuthenticationMechanism(String mechanism, String serverName, String protocol, Map<String, String> props, ConfigurationProperties properties) throws IOException {
    ConfigurationProperties config = (ConfigurationProperties) properties.get("sasl");
    IdentityLookup identityLookup;
    ServerCallbackHandler serverCallbackHandler;
    if (config.getProperty(IDENTITY_PROVIDER).equalsIgnoreCase("system")) {
      identityLookup = IdentityLookupFactory.getInstance().getSiteWide("system");
    } else {
      identityLookup = IdentityLookupFactory.getInstance().get(config.getProperty(IDENTITY_PROVIDER), config.getMap());
    }
    if(identityLookup == null){
      throw new SaslException("Unable to locate identity look up mechanism for " + config.getProperty(IDENTITY_PROVIDER));
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
