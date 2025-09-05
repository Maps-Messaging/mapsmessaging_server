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


import io.mapsmessaging.security.identity.IdentityLookup;
import io.mapsmessaging.security.passwords.PasswordBuffer;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class ServerCallbackHandler implements CallbackHandler {

  private String username;
  private PasswordBuffer hashedPassword;
  private final String serverName;

  private final IdentityLookup identityLookup;

  public ServerCallbackHandler(String serverName, IdentityLookup identityLookup){
    this.identityLookup = identityLookup;
    this.serverName = serverName;
  }

  @Override
  public void handle(Callback[] cbs) throws IOException {
    for (Callback cb : cbs) {
      if (cb instanceof AuthorizeCallback) {
        AuthorizeCallback ac = (AuthorizeCallback) cb;
        ac.setAuthorized(true);
      } else if (cb instanceof NameCallback) {
        NameCallback nc = (NameCallback) cb;
        username = nc.getDefaultName();
        try {
          hashedPassword = identityLookup.getPasswordHash(username);
        } catch (GeneralSecurityException e) {
          throw new IOException(e);
        }
        nc.setName(nc.getDefaultName());
      } else if (cb instanceof PasswordCallback) {
        PasswordCallback pc = (PasswordCallback) cb;
        pc.setPassword(hashedPassword.getHash());
      } else if (cb instanceof RealmCallback) {
        RealmCallback rc = (RealmCallback) cb;
        rc.setText(serverName);
      }
    }
  }
}