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

package io.mapsmessaging.auth;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.security.identity.IdentityLookup;
import io.mapsmessaging.security.identity.IdentityLookupFactory;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import lombok.Getter;

import java.util.Map;


public class AuthManager implements Agent {
  @Getter
  private static final AuthManager instance = new AuthManager();

  private final Logger logger;
  private final ConfigurationProperties properties;

  @Getter
  private final boolean authenticationEnabled;
  @Getter
  private final boolean authorisationEnabled;

  private final IdentityLookup identityLookup;

  @Override
  public String getName() {
    return "AuthManager";
  }

  @Override
  public String getDescription() {
    return "Manages the authentication and authorisation of user connections to the server";
  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {

  }


  private AuthManager() {
    logger = LoggerFactory.getLogger(AuthManager.class);
    properties = ConfigurationManager.getInstance().getProperties("AuthManager");
    authenticationEnabled = properties.getBooleanProperty("authenticationEnabled", false);
    authorisationEnabled = properties.getBooleanProperty("authorizationEnabled", false) && authenticationEnabled;
    if (authenticationEnabled) {
      Map<String, Object> config = ((ConfigurationProperties) properties.get("config")).getMap();
      String authProvider = config.get("identityProvider").toString().trim();
      String password = InitialStateHelper.checkForFirstInstall(properties);
      identityLookup = IdentityLookupFactory.getInstance().get(authProvider, config);
      if (password != null) {
        if (InitialStateHelper.validate("admin", password, identityLookup)) {
          // Success!!!

        } else {
          // Broke!!!!
        }
      }
    } else {
      identityLookup = null;
    }
    if (identityLookup == null) {
      // todo log the fact we have not authentication
    }
  }
}
