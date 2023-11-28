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
import io.mapsmessaging.auth.registry.priviliges.session.SessionPrivileges;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import lombok.Getter;

import javax.security.auth.Subject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;


public class AuthManager implements Agent {
  @Getter
  private static final AuthManager instance = new AuthManager();

  private final Logger logger;
  private final ConfigurationProperties properties;
  private AuthenticationStorage authenticationStorage;

  @Getter
  private final boolean authenticationEnabled;
  @Getter
  private final boolean authorisationEnabled;

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
      ConfigurationProperties config = (ConfigurationProperties) properties.get("config");
      String password = null;
      String userpassword = null;
      authenticationStorage = new AuthenticationStorage(config);

      if (!authenticationStorage.isExisted()) {
        password = PasswordGenerator.generateRandomPassword(12);
        String username = "admin";
        addUser(username, password, SessionPrivileges.create(username), new String[]{username, "everyone"});

        userpassword = PasswordGenerator.generateRandomPassword(12);
        username = "user";
        addUser(username, userpassword, SessionPrivileges.create(username), new String[]{"everyone"});

        String path = config.get("configDirectory").toString();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + File.separator + "admin_password", true))) {
          bw.write("admin=" + password);
          bw.newLine(); // Add a newline character after each line
          bw.write("user=" + userpassword);
          bw.newLine(); // Add a newline character after each line

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (password != null) {
        if (authenticationStorage.validateUser("admin", password)) {
          System.err.println("Successfully added admin");
        } else {
          System.err.println("Failed to add admin");
        }
        if (authenticationStorage.validateUser("user", userpassword)) {
          System.err.println("Successfully added user");
        } else {
          System.err.println("Failed to add user");
        }
      }
    }

  }

  @Override
  public void stop() {
    try {
      authenticationStorage.close();
    } catch (IOException e) {

    }
  }

  public boolean addUser(String username, String password, SessionPrivileges quotas, String[] groups) {
    return authenticationStorage.addUser(username, password, quotas, groups);
  }

  public boolean delUser(String username) {
    return authenticationStorage.delUser(username);
  }

  public Subject update(Subject subject) {
    return authenticationStorage.update(subject);
  }

  private AuthManager() {
    logger = LoggerFactory.getLogger(AuthManager.class);
    properties = ConfigurationManager.getInstance().getProperties("AuthManager");
    authenticationEnabled = properties.getBooleanProperty("authenticationEnabled", false);
    authorisationEnabled = properties.getBooleanProperty("authorizationEnabled", false) && authenticationEnabled;
  }

  public SessionPrivileges getQuota(UUID userId) {
    return authenticationStorage.getQuota(userId);
  }
}
