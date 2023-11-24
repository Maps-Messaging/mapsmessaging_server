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

import io.mapsmessaging.auth.registry.AuthenticationStorage;
import io.mapsmessaging.auth.registry.PasswordGenerator;
import io.mapsmessaging.auth.registry.Quotas;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.security.identity.IdentityLookup;
import io.mapsmessaging.security.identity.IdentityLookupFactory;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import lombok.Getter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;


public class AuthManager implements Agent {
  @Getter
  private static final AuthManager instance = new AuthManager();

  private final Logger logger;
  private final ConfigurationProperties properties;
  private final AuthenticationStorage authenticationStorage;

  @Getter
  private final boolean authenticationEnabled;
  @Getter
  private final boolean authorisationEnabled;

  private IdentityLookup identityLookup;

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
    if (authenticationEnabled) {
      Map<String, Object> config = ((ConfigurationProperties) properties.get("config")).getMap();
      String authProvider = config.get("identityProvider").toString().trim();
      String password = null;
      if (!authenticationStorage.getUserFile().exists()) {
        password = PasswordGenerator.generateRandomPassword(12);
        String username = "admin";
        addUser(username, password, Quotas.createAdminQuota(username), new String[]{username});
        String path = properties.getProperty("configDirectory", "./security");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + File.separator + "admin_password", true))) {
          bw.write("admin=" + password);
          bw.newLine(); // Add a newline character after each line
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      identityLookup = IdentityLookupFactory.getInstance().get(authProvider, config);
      if (password != null) {
        if (authenticationStorage.validateUser("admin", password, identityLookup)) {
          System.err.println("Successfully added admin");
        } else {
          System.err.println("Failed to add admin");
        }
      }

    } else {
      identityLookup = null;
    }
    if (identityLookup == null) {
      // todo log the fact we have not authentication
    }

  }

  @Override
  public void stop() {
    try {
      authenticationStorage.close();
    } catch (IOException e) {

    }
  }

  public boolean addUser(String username, String password, Quotas quotas, String[] groups) {
    if (identityLookup == null || identityLookup.findEntry(username) == null) {
      if (!quotas.getUsername().equalsIgnoreCase(username)) {
        // ToDo we need to check the quotas are valid
      }
      return authenticationStorage.addUser(username, password, quotas, groups);
    }
    return false;
  }

  public boolean delUser(String username) {
    if (identityLookup.findEntry(username) != null) {
      return authenticationStorage.delUser(username);
    }
    return false;
  }

  private AuthManager() {
    logger = LoggerFactory.getLogger(AuthManager.class);
    properties = ConfigurationManager.getInstance().getProperties("AuthManager");
    authenticationEnabled = properties.getBooleanProperty("authenticationEnabled", false);
    authorisationEnabled = properties.getBooleanProperty("authorizationEnabled", false) && authenticationEnabled;
    authenticationStorage = new AuthenticationStorage(properties.getProperty("configDirectory", "./security"));
  }

  public Quotas getQuota(UUID userId) {
    return authenticationStorage.getQuota(userId);
  }
}
