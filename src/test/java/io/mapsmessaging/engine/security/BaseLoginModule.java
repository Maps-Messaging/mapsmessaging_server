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

package io.mapsmessaging.engine.security;

import java.security.Principal;
import java.util.Arrays;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public abstract class BaseLoginModule implements LoginModule {

  protected Subject subject;
  protected CallbackHandler callbackHandler;

  // configurable option
  protected boolean debug = false;

  protected boolean succeeded = false;
  protected boolean commitSucceeded = false;

  protected String username;
  protected char[] password;

  protected Principal userPrincipal;

  public void initialize(
      Subject subject,
      CallbackHandler callbackHandler,
      Map<String, ?> sharedState,
      Map<String, ?> options) {
    if (subject == null) {
      this.subject = new Subject();
    } else {
      this.subject = subject;
    }
    this.callbackHandler = callbackHandler;
    debug = "true".equalsIgnoreCase((String) options.get("debug"));
  }

  public boolean abort() throws LoginException {
    if (!succeeded) {
      return false;
    } else if (!commitSucceeded) {
      succeeded = false;
      username = null;
      if (password != null) {
        Arrays.fill(password, ' ');
        password = null;
      }
      userPrincipal = null;
    } else {
      logout();
    }
    return true;
  }

  public boolean logout() throws LoginException {
    if (subject != null && userPrincipal != null) {
      subject.getPrincipals().remove(userPrincipal);
    }
    succeeded = commitSucceeded;
    username = null;
    if (password != null) {
      Arrays.fill(password, ' ');
      password = null;
    }
    userPrincipal = null;
    return true;
  }
}