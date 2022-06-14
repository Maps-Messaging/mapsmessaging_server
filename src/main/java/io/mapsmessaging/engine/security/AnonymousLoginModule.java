/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

import io.mapsmessaging.logging.ServerLogMessages;
import java.io.IOException;
import java.util.stream.IntStream;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

public class AnonymousLoginModule extends BaseLoginModule {

  @Override
  public boolean login() throws LoginException {

    // prompt for a user name and password
    if (callbackHandler == null) {
      throw new LoginException("Error: no CallbackHandler available to garner authentication information from the user");
    }

    Callback[] callbacks = new Callback[2];
    callbacks[0] = new NameCallback("user name: ");
    callbacks[1] = new PasswordCallback("password: ", false);

    try {
      callbackHandler.handle(callbacks);
      username = ((NameCallback) callbacks[0]).getName();
      char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
      if (tmpPassword == null) {
        // treat a NULL password as an empty password
        tmpPassword = new char[0];
      }
      password = new char[tmpPassword.length];
      System.arraycopy(tmpPassword, 0, password, 0, tmpPassword.length);
      ((PasswordCallback) callbacks[1]).clearPassword();
    } catch (IOException ioe) {
      throw new LoginException(ioe.toString());
    } catch (UnsupportedCallbackException uce) {
      throw new LoginException(
          "Error: "
              + uce.getCallback().toString()
              + " not available to garner authentication information "
              + "from the user");
    }

    // print debugging information
    if (debug) {
      logger.log(ServerLogMessages.ANON_LOGIN_MODULE_USERNAME, username);
      logger.log(ServerLogMessages.ANON_LOGIN_MODULE_USERNAME, new String(password));
    }
    succeeded = true;
    return true;
  }

  @Override
  public boolean commit()  {
    if (!succeeded) {
      return false;
    } else {
      userPrincipal = new AnonymousPrincipal(username);
      subject.getPrincipals().add(userPrincipal);
      if (debug) {
        logger.log(ServerLogMessages.ANON_LOGIN_MODULE_PASSWORD, "");
      }
      // in any case, clean out state
      IntStream.range(0, password.length).forEach(i -> password[i] = ' ');
      password = null;

      commitSucceeded = true;
      return true;
    }
  }

  @Override
  public boolean logout() throws LoginException {
    return super.logout();
  }

}
