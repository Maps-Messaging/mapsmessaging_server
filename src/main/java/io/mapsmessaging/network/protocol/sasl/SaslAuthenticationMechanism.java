/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.sasl;

import io.mapsmessaging.network.AuthenticationMechanism;
import io.mapsmessaging.security.identity.IdentityLookup;
import io.mapsmessaging.security.identity.IdentityLookupFactory;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
import java.util.Map;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

public class SaslAuthenticationMechanism implements AuthenticationMechanism {

  private final SaslServer saslServer;
  private final IdentityLookup identityLookup;
  private final ServerCallbackHandler serverCallbackHandler;

  public SaslAuthenticationMechanism(String mechanism, String serverName, String protocol, Map<String, String> props, ConfigurationProperties properties) throws SaslException {
    ConfigurationProperties config = (ConfigurationProperties) properties.get("Sasl");
    identityLookup = IdentityLookupFactory.getInstance().get(config.getProperty("identityProvider"), config.getMap());
    if(identityLookup == null){
      throw new SaslException("Unable to locate identity look up mechanism for "+config.getProperty("identityProvider"));
    }
    serverCallbackHandler = new ServerCallbackHandler(serverName,identityLookup );
    saslServer = Sasl.createSaslServer(mechanism, protocol, serverName, props, serverCallbackHandler);
  }

  @Override
  public byte[] challenge(byte[] challenge) throws IOException {
    return saslServer.evaluateResponse(challenge);
  }

  @Override
  public boolean complete() {
    return saslServer.isComplete();
  }
}
