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

package io.mapsmessaging.network.protocol.impl;

import io.mapsmessaging.network.AuthenticationMechanism;
import java.io.IOException;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

public class SaslAuthenticationMechanism implements AuthenticationMechanism {
  private final String mechanism;
  private final SaslServer saslServer;

  public SaslAuthenticationMechanism(String mechanism, Map<String, String> props) throws SaslException {
    this.mechanism = mechanism;
    // Now we use SASL but this should be configured to use any class in the future
    saslServer = Sasl.createSaslServer(mechanism, "mqtt", null, props, new ServerCallbackHandler());
  }

  @Override
  public byte[] challenge(byte[] challenge) throws IOException {
    return saslServer.evaluateResponse(challenge);
  }

  @Override
  public boolean complete() {
    return saslServer.isComplete();
  }

  static class ServerCallbackHandler implements CallbackHandler {

    @Override
    public void handle(Callback[] cbs) {
      for (Callback cb : cbs) {
        if (cb instanceof AuthorizeCallback) {
          AuthorizeCallback ac = (AuthorizeCallback) cb;
          ac.setAuthorized(true);
        } else if (cb instanceof NameCallback) {
          NameCallback nc = (NameCallback) cb;
          nc.setName("username");
        } else if (cb instanceof PasswordCallback) {
          PasswordCallback pc = (PasswordCallback) cb;
          pc.setPassword("password".toCharArray());
        } else if (cb instanceof RealmCallback) {
          RealmCallback rc = (RealmCallback) cb;
          rc.setText("myServer");
        }
      }
    }
  }
}
