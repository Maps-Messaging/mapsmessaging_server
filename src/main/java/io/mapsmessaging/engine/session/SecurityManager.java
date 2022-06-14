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

import io.mapsmessaging.engine.security.PrincipalCallback;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.security.Principal;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public class SecurityManager {

  private final Logger logger = LoggerFactory.getLogger(SecurityManager.class);
  private final ConfigurationProperties properties;

  public SecurityManager() {
    logger.log(ServerLogMessages.SECURITY_MANAGER_STARTUP);
    ConfigurationProperties props = ConfigurationManager.getInstance().getProperties("SecurityManager");
    logger.log(ServerLogMessages.SESSION_MANAGER_CREATE_SECURITY_CONTEXT);
    properties = props;
  }

  public SecurityContext getSecurityContext(SessionContext sessionContext) throws LoginException {
    String username = sessionContext.getUsername();
    char[] passCode = sessionContext.getPassword();
    ProtocolImpl protocol = sessionContext.getProtocol();
    String defined = getAuthenticationName(protocol);
    if (defined != null) {
      Principal endPointPrincipal = protocol.getEndPoint().getEndPointPrincipal();
      if (username == null && endPointPrincipal != null) {
        username = endPointPrincipal.getName();
      }
      LoginContext loginContext = getLoginContext(defined, username, passCode, endPointPrincipal);
      return new SecurityContext(username, loginContext);
    }
    return new AnonymousSecurityContext();
  }

  public LoginContext getLoginContext(String definedAuth, String username, char[] passCode, Principal endPointPrincipal) throws LoginException {
    return new LoginContext(definedAuth, new LocalCallbackHandler(username, passCode, endPointPrincipal));
  }

  public String getAuthenticationName(ProtocolImpl protocol){
    String authConfig = protocol.getEndPoint().getAuthenticationConfig();
    if (authConfig != null && authConfig.length() > 0) {
      return properties.getProperty(authConfig);
    } else {
      return properties.getProperty("default");
    }
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
