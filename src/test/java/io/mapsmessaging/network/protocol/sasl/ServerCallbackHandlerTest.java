/*
 *
 * Copyright [ 2020 - 2024 ] Matthew Buckton
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.AuthorizeCallback;
import org.junit.jupiter.api.Test;

class ServerCallbackHandlerTest {

  @Test
  void authorization_identity_must_match_authentication_identity() throws Exception {
    ServerCallbackHandler callbackHandler = new ServerCallbackHandler("server", null);
    AuthorizeCallback sameIdentity = new AuthorizeCallback("matthew", "matthew");
    AuthorizeCallback delegatedIdentity = new AuthorizeCallback("matthew", "administrator");

    callbackHandler.handle(new Callback[] {sameIdentity, delegatedIdentity});

    assertTrue(sameIdentity.isAuthorized());
    assertEquals("matthew", sameIdentity.getAuthorizedID());
    assertFalse(delegatedIdentity.isAuthorized());
  }

  @Test
  void password_callback_requires_a_resolved_identity() {
    ServerCallbackHandler callbackHandler = new ServerCallbackHandler("server", null);
    assertThrows(IOException.class, () -> callbackHandler.handle(new Callback[] {new PasswordCallback("password", false)}));
  }
}
