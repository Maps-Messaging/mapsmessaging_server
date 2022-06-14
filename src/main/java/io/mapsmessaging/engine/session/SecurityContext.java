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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.audit.AuditEvent;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import java.io.IOException;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public class SecurityContext {

  final Logger logger = LoggerFactory.getLogger(SecurityContext.class);
  private final LoginContext loginContext;

  protected final String username;
  protected boolean isLoggedIn;

  public SecurityContext(String username, LoginContext lc) {
    loginContext = lc;
    this.username = username;
    isLoggedIn = false;
  }

  public String getUsername(){
    return username;
  }

  public Subject getSubject() {
    return loginContext.getSubject();
  }

  public boolean isLoggedIn() {
    return isLoggedIn;
  }

  public void login() throws IOException {
    try {
      loginContext.login();
      logger.log(AuditEvent.SUCCESSFUL_LOGIN, username);
      isLoggedIn = true;
    } catch (LoginException e) {
      logger.log(ServerLogMessages.SECURITY_MANAGER_FAILED_LOG_IN, username, e.getMessage());
      IOException ioException = new IOException(e.getMessage());
      ioException.fillInStackTrace();
      throw ioException;
    }
  }

  public void logout() {
    try {
      if (isLoggedIn) {
        isLoggedIn = false;
        loginContext.logout();
        logger.log(AuditEvent.SUCCESSFUL_LOGOUT, username);
      }
    } catch (LoginException e) {
      logger.log(ServerLogMessages.SECURITY_MANAGER_FAILED_LOG_OFF, username, e.getMessage());
    }
  }
}
