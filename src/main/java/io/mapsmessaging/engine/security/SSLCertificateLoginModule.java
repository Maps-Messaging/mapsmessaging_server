/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.security;

import io.mapsmessaging.logging.LogMessages;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

public class SSLCertificateLoginModule extends BaseLoginModule {

  private Principal sslPrincipal;

  @Override
  public void initialize(
      Subject subject,
      CallbackHandler callbackHandler,
      Map<String, ?> sharedState,
      Map<String, ?> options) {
    super.initialize(subject, callbackHandler, sharedState, options);
    sslPrincipal = null;
  }

  @Override
  public boolean login() throws LoginException {

    // prompt for a user name and password
    if (callbackHandler == null) {
      throw new LoginException(
          "Error: no CallbackHandler available to garner authentication information from the user");
    }

    Callback[] callbacks = new Callback[3];
    callbacks[0] = new NameCallback("user name: ");
    callbacks[1] = new PrincipalCallback();
    callbacks[2] = new PasswordCallback("password: ", false);

    try {
      callbackHandler.handle(callbacks);
      username = ((NameCallback) callbacks[0]).getName();
      sslPrincipal = ((PrincipalCallback) callbacks[1]).getPrincipal();
      char[] tmpPassword = ((PasswordCallback) callbacks[2]).getPassword();
      if (tmpPassword == null) {
        // treat a NULL password as an empty password
        tmpPassword = new char[0];
      }
      password = new char[tmpPassword.length];
      System.arraycopy(tmpPassword, 0, password, 0, tmpPassword.length);
      ((PasswordCallback) callbacks[2]).clearPassword();

    } catch (IOException ioe) {
      throw new LoginException(ioe.toString());
    } catch (UnsupportedCallbackException uce) {
      throw new LoginException(
          "Error: "
              + uce.getCallback().toString()
              + " not available to garner authentication information from the user");
    }

    // print debugging information
    if (debug) {
      logger.log(LogMessages.SSL_CERTIFICATE_SECURITY_USERNAME, username);
      logger.log(LogMessages.SSL_CERTIFICATE_SECURITY_PASSWORD, new String(password));
    }
    succeeded = true;
    return true;
  }

  @Override
  public boolean commit() {
    if (!succeeded) {
      return false;
    } else {
      subject.getPrincipals().add(sslPrincipal);
      if (debug) {
        logger.log(LogMessages.SSL_CERTIFICATE_SECURITY_PASSWORD);
      }
      // in any case, clean out state
      username = null;
      if (password != null) {
        Arrays.fill(password, ' ');
        password = null;
      }
      sslPrincipal = null;
      commitSucceeded = true;
      return true;
    }
  }

  @Override
  public boolean abort() throws LoginException {
    sslPrincipal = null;
    return super.abort();
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
  @Override
  public boolean logout() throws LoginException {
    logger.log(LogMessages.SSL_CERTIFICATE_SECURITY_SUBJECT_LOG_IN);
    return super.logout();
  }
}
