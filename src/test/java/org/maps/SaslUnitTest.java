/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Provider.Service;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class SaslUnitTest {

  private static final String MECHANISM = "DIGEST-MD5";
  private static final String SERVER_NAME = "myServer";
  private static final String PROTOCOL = "myProtocol";
  private static final String AUTHORIZATION_ID = null;
  private static final String QOP_LEVEL = "auth-conf";

  private SaslServer saslServer;
  private SaslClient saslClient;

  @BeforeEach
  public void setUp() throws SaslException {

    ServerCallbackHandler serverHandler = new ServerCallbackHandler();
    ClientCallbackHandler clientHandler = new ClientCallbackHandler();

    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, QOP_LEVEL);

    saslServer = Sasl.createSaslServer(MECHANISM, PROTOCOL, SERVER_NAME, props, serverHandler);
    saslClient = Sasl.createSaslClient(new String[]{MECHANISM}, AUTHORIZATION_ID, PROTOCOL, SERVER_NAME, props, clientHandler);

  }

  public void givenHandlers_whenStarted_thenAutenticationWorks() throws SaslException {

    Provider[] providers = java.security.Security.getProviders();
    for(Provider provider:providers){
      System.err.println(provider.toString());
      for(Service service:provider.getServices()){
        System.err.println("\t"+service.toString());
      }
    }

    byte[] challenge;
    byte[] response;

    challenge = saslServer.evaluateResponse(new byte[0]);
    response = saslClient.evaluateChallenge(challenge);

    challenge = saslServer.evaluateResponse(response);
    response = saslClient.evaluateChallenge(challenge);
    System.err.println(saslServer.getAuthorizationID());

    assertTrue(saslServer.isComplete());
    assertTrue(saslClient.isComplete());

    String qop = (String) saslClient.getNegotiatedProperty(Sasl.QOP);
    assertEquals("auth-conf", qop);

    byte[] outgoing = "Baeldung".getBytes();
    byte[] secureOutgoing = saslClient.wrap(outgoing, 0, outgoing.length);

    byte[] secureIncoming = secureOutgoing;
    byte[] incoming = saslServer.unwrap(secureIncoming, 0, secureIncoming.length);
    assertEquals("Baeldung", new String(incoming, StandardCharsets.UTF_8));
  }

  @AfterEach
  public void tearDown() throws SaslException {
    saslClient.dispose();
    saslServer.dispose();
  }

  class ClientCallbackHandler implements CallbackHandler {

    @Override
    public void handle(Callback[] cbs) throws IOException, UnsupportedCallbackException {
      for (Callback cb : cbs) {
        if (cb instanceof NameCallback) {
          NameCallback nc = (NameCallback) cb;
          nc.setName("userName");
        } else if (cb instanceof PasswordCallback) {
          PasswordCallback pc = (PasswordCallback) cb;
          pc.setPassword("password1".toCharArray());
        } else if (cb instanceof RealmCallback) {
          RealmCallback rc = (RealmCallback) cb;
          rc.setText("myServer");
        }
      }
    }
  }

  class ServerCallbackHandler implements CallbackHandler {

    @Override
    public void handle(Callback[] cbs) throws IOException, UnsupportedCallbackException {
      for (Callback cb : cbs) {
        if (cb instanceof AuthorizeCallback) {
          AuthorizeCallback ac = (AuthorizeCallback) cb;
          ac.setAuthorized(true);
        } else if (cb instanceof NameCallback) {
          NameCallback nc = (NameCallback) cb;
          nc.setName(nc.getDefaultName());
        } else if (cb instanceof PasswordCallback) {
          PasswordCallback pc = (PasswordCallback) cb;
          pc.setPassword("password2".toCharArray());
        } else if (cb instanceof RealmCallback) {
          RealmCallback rc = (RealmCallback) cb;
          rc.setText("myServer");
        }
      }
    }
  }
}