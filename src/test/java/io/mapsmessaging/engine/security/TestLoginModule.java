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

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.security.jaas.AnonymousPrincipal;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.util.stream.IntStream;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

public class TestLoginModule extends BaseLoginModule {

  private static final String[] USERNAMES = {"user1", "admin", "user2", "anonymous"};
  private static final char[][] PASSWORDS = {"password1".toCharArray(), "admin1".toCharArray(), "password2".toCharArray(), "".toCharArray()};
  private static final String[] GROUPS = {"everyone"};

  @BeforeAll
  static void setUp() {
    for(int i = 0; i < USERNAMES.length; i++) {
      AuthManager.getInstance().addUser(USERNAMES[i], PASSWORDS[i], SessionPrivileges.create(USERNAMES[i]), GROUPS);
    }
  }

  public static String[] getUsernames() {
    return USERNAMES;
  }

  public static char[][] getPasswords() {
    return PASSWORDS;
  }

  @Override
  public boolean login() throws LoginException {
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

      succeeded = authorise();
    } catch (IOException ioe) {
      throw new LoginException(ioe.toString());
    } catch (UnsupportedCallbackException uce) {
      throw new LoginException("Error: " + uce.getCallback().toString() + " not available to garner authentication information from the user");
    }
    return succeeded;
  }

  private boolean authorise() {
    boolean found = false;
    for (int x = 0; x < USERNAMES.length; x++) {
      if (username.equals(USERNAMES[x]) && password.length == PASSWORDS[x].length) {
        for (int y = 0; y < password.length; y++) {
          if (password[y] == PASSWORDS[x][y]) {
            found = true;
          } else {
            found = false;
            break;
          }
        }
      }
    }
    return found;
  }

  @Override
  public boolean commit() throws LoginException {
    if (!succeeded) {
      return false;
    } else {
      userPrincipal = new AnonymousPrincipal(username);
      subject.getPrincipals().add(userPrincipal);
      // in any case, clean out state
      username = null;
      IntStream.range(0, password.length).forEach(i -> password[i] = ' ');
      password = null;

      commitSucceeded = true;
      return true;
    }
  }
}