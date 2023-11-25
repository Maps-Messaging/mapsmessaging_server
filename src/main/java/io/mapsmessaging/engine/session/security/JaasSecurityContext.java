/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.engine.session.security;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.QuotaPrincipal;
import io.mapsmessaging.auth.registry.priviliges.session.SessionPrivileges;
import io.mapsmessaging.engine.audit.AuditEvent;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.security.identity.principals.UniqueIdentifierPrincipal;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.UUID;

public class JaasSecurityContext extends SecurityContext {

  private final Logger logger = LoggerFactory.getLogger(JaasSecurityContext.class);
  private final LoginContext loginContext;

  public JaasSecurityContext(String username, LoginContext lc) {
    super(username);
    loginContext = lc;
    isLoggedIn = false;
  }

  @Override
  public void login() throws IOException {
    try {
      loginContext.login();
      subject = loginContext.getSubject();
      UUID userId = subject.getPrincipals(UniqueIdentifierPrincipal.class).stream()
          .findFirst()
          .map(UniqueIdentifierPrincipal::getAuthId)
          .orElse(null);
      if (userId != null) {
        SessionPrivileges session = AuthManager.getInstance().getQuota(userId);
        if (session != null) {
          subject.getPrincipals().add(new QuotaPrincipal(session, null));
        }
      }
      logger.log(AuditEvent.SUCCESSFUL_LOGIN, subject);
      isLoggedIn = true;
    } catch (LoginException e) {
      logger.log(ServerLogMessages.SECURITY_MANAGER_FAILED_LOG_IN, username, e.getMessage());
      IOException ioException = new IOException(e.getMessage());
      ioException.fillInStackTrace();
      throw ioException;
    }
  }

  @Override
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
