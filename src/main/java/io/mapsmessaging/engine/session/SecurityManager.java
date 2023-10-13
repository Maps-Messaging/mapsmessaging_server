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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.session.security.AnonymousSecurityContext;
import io.mapsmessaging.engine.session.security.JaasSecurityContext;
import io.mapsmessaging.engine.session.security.SaslSecurityContext;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.security.MapsSecurityProvider;
import io.mapsmessaging.security.jaas.PrincipalCallback;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.security.Principal;

import static io.mapsmessaging.logging.ServerLogMessages.SECURITY_MANAGER_SECURITY_CONTEXT;

public class SecurityManager implements Agent {

  private final Logger logger = LoggerFactory.getLogger(SecurityManager.class);
  private final ConfigurationProperties properties;

  public SecurityManager() {
    logger.log(ServerLogMessages.SECURITY_MANAGER_STARTUP);
    ConfigurationProperties props = ConfigurationManager.getInstance().getProperties("SecurityManager");
    logger.log(ServerLogMessages.SESSION_MANAGER_CREATE_SECURITY_CONTEXT);
    properties = props;
    MapsSecurityProvider.register();
  }

  public SecurityContext getSecurityContext(SessionContext sessionContext) throws LoginException {
    String username = sessionContext.getUsername();
    char[] passCode = sessionContext.getPassword();
    ClientConnection clientConnection = sessionContext.getClientConnection();
    String defined = getAuthenticationName(clientConnection);
    Principal endPointPrincipal = clientConnection.getPrincipal();
    SecurityContext context;
    if(sessionContext.isAuthorized()){
      username = sessionContext.getUsername();
      context = new SaslSecurityContext(username, endPointPrincipal);
    }
    else if (defined != null) {
      LoginContext loginContext = getLoginContext(defined, username, passCode, endPointPrincipal);
      context = new JaasSecurityContext(username, loginContext);
    }
    else {
      context = new AnonymousSecurityContext(endPointPrincipal);
    }
    logger.log(SECURITY_MANAGER_SECURITY_CONTEXT, context.getSubject());
    return context;
  }

  public LoginContext getLoginContext(String definedAuth, String username, char[] passCode, Principal endPointPrincipal) throws LoginException {
    return new LoginContext(definedAuth, new LocalCallbackHandler(username, passCode, endPointPrincipal));
  }

  public String getAuthenticationName(ClientConnection clientConnection) {
    String authConfig = clientConnection.getAuthenticationConfig();
    if (authConfig != null && !authConfig.isEmpty()) {
      return properties.getProperty(authConfig);
    } else {
      return properties.getProperty("default");
    }
  }

  @Override
  public String getName() {
    return "Security Manager";
  }

  @Override
  public String getDescription() {
    return "Manages the different login mechanisms for the server";
  }

  @Override
  public void start() {
    // No Action required
  }

  @Override
  public void stop() {
    // No Action required
  }

  private static class LocalCallbackHandler implements CallbackHandler {

    private final String username;
    private final char[] password;
    private final Principal endPointPrincipal;

    LocalCallbackHandler(String username, char[] password, Principal endPointPrincipal) {
      this.username = username;
      this.password = password;
      this.endPointPrincipal = endPointPrincipal;
    }

    public void handle(Callback[] callbacks) {
      for (Callback callback : callbacks) {
        if (callback instanceof NameCallback) {
          ((NameCallback) callback).setName(username);
        } else if (callback instanceof PasswordCallback) {
          ((PasswordCallback) callback).setPassword(password);
        } else if (callback instanceof PrincipalCallback) {
          ((PrincipalCallback) callback).setPrincipal(endPointPrincipal);
        }
      }
    }
  }
}
