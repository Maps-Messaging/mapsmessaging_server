/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
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

package org.maps.messaging.engine.security;

import java.util.Arrays;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;

public abstract class BaseLoginModule implements LoginModule {

  protected final Logger logger = LoggerFactory.getLogger(BaseLoginModule.class);
  protected Subject subject;
  protected CallbackHandler callbackHandler;

  // configurable option
  protected boolean debug = false;

  protected boolean succeeded = false;
  protected boolean commitSucceeded = false;

  protected String username;
  protected char[] password;

  protected AnonymousPrincipal userPrincipal;

  public void initialize(
      Subject subject,
      CallbackHandler callbackHandler,
      Map<String, ?> sharedState,
      Map<String, ?> options) {

    this.subject = subject;
    this.callbackHandler = callbackHandler;

    // initialize any configured options
    debug = "true".equalsIgnoreCase((String) options.get("debug"));
  }

  public boolean abort() throws LoginException {
    if (!succeeded) {
      return false;
    } else if (!commitSucceeded) {
      // login succeeded but overall authentication failed
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

  /**
   * Logout the user.
   *
   * <p>This method removes the <code>SamplePrincipal</code> that was added by the <code>commit
   * </code> method.
   *
   * <p>
   *
   * @return true in all cases since this <code>LoginModule</code> should not be ignored.
   * @throws LoginException if the logout fails.
   */
  public boolean logout() throws LoginException {
    if (subject != null && userPrincipal != null) {
      subject.getPrincipals().remove(userPrincipal);
    }
    succeeded = false;
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