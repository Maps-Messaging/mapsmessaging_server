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

import com.sun.security.auth.UserPrincipal;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.auth.registry.AuthenticationStorage;
import io.mapsmessaging.auth.registry.GroupDetails;
import io.mapsmessaging.auth.registry.PasswordGenerator;
import io.mapsmessaging.auth.registry.UserDetails;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.security.access.mapping.UserIdMap;
import io.mapsmessaging.security.identity.IdentityLookupFactory;
import io.mapsmessaging.security.identity.principals.UniqueIdentifierPrincipal;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import lombok.Getter;

import javax.security.auth.Subject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Principal;
import java.util.*;


public class AuthManager implements Agent {
  private static final String ADMIN_USER = "admin";
  private static final String USER = "user";

  private static final String ADMIN_GROUP = "admin";
  private static final String EVERYONE = "everyone";

  @Getter
  private static final AuthManager instance = new AuthManager();

  private final Logger logger;
  private final ConfigurationProperties properties;
  private AuthenticationStorage authenticationStorage;
  private final Map<String, Subject> subjectMap = new WeakHashMap<>();

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
      authenticationStorage = new AuthenticationStorage(config);
      if (!authenticationStorage.isFirstBoot()) {
        createInitialUsers(config.get("configDirectory").toString());
      }
      IdentityLookupFactory.getInstance().registerSiteIdentityLookup("system", authenticationStorage.getIdentityAccessManager().getIdentityLookup());
    }
  }

  private void saveInitialUserDetails(String path, String[][] details) {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + File.separator + "admin_password", true))) {
      for (String[] detail : details) {
        bw.write(detail[0] + "=" + detail[1]);
        bw.newLine(); // Add a newline character after each line
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void stop() {
    try {
      if (authenticationStorage != null) authenticationStorage.close();
    } catch (IOException e) {

    }
  }

  public boolean addUser(String username, String password, SessionPrivileges quotas, String[] groups) {
    if (authenticationStorage != null) {
      return authenticationStorage.addUser(username, password, quotas, groups);
    }
    return false;
  }

  public boolean delUser(String username) {
    if (authenticationStorage != null) {
      return authenticationStorage.delUser(username);
    }
    return false;
  }

  public Subject update(Subject subject) {
    if (authenticationStorage != null) {
      return authenticationStorage.update(subject);
    }
    return null;
  }

  public SessionPrivileges getQuota(UUID userId) {
    if (authenticationStorage != null) {
      return authenticationStorage.getQuota(userId);
    }
    return null;
  }

  private AuthManager() {
    logger = LoggerFactory.getLogger(AuthManager.class);
    properties = ConfigurationManager.getInstance().getProperties("AuthManager");
    authenticationEnabled = properties.getBooleanProperty("authenticationEnabled", false);
    authorisationEnabled = properties.getBooleanProperty("authorizationEnabled", false) && authenticationEnabled;
  }

  private void createInitialUsers(String path) {
    String password = PasswordGenerator.generateRandomPassword(12);
    if (!addUser(ADMIN_USER, password, SessionPrivileges.create(ADMIN_USER), new String[]{ADMIN_GROUP, EVERYONE})) {
      // ToDo : log
    }

    String userpassword = PasswordGenerator.generateRandomPassword(12);
    if (addUser(USER, userpassword, SessionPrivileges.create(USER), new String[]{EVERYONE})) {
      // ToDo : log

    }

    saveInitialUserDetails(path, new String[][]{{ADMIN_USER, password}, {USER, userpassword}});
    if (authenticationStorage.validateUser(ADMIN_USER, password)) {
      // To Do : log
    } else {
      // To Do : log
    }
    if (authenticationStorage.validateUser(USER, userpassword)) {
      // To Do : log
    } else {
      // To Do : log
    }
  }

  public boolean validate(String username, String password) {
    return authenticationStorage.validateUser(username, password);
  }

  public UserIdMap getUserIdentity(String username) {
    return authenticationStorage.findUser(username);
  }

  public UserIdMap getUserIdentity(UUID uuid) {
    return authenticationStorage.findUser(uuid);
  }

  public Subject getUserSubject(String username) {
    Subject subject = subjectMap.get(username);
    if (subject == null) {
      subject = new Subject();
      Set<Principal> principalList = subject.getPrincipals();
      UserIdMap map = authenticationStorage.findUser(username);
      if (map != null) {
        principalList.add(new UniqueIdentifierPrincipal(map.getAuthId()));
        principalList.add(new UserPrincipal(username));
        authenticationStorage.update(subject);
      }
      subjectMap.put(username, subject);
    }
    return subject;
  }

  public List<UserDetails> getUsers() {
    if (authenticationStorage != null) {
      return authenticationStorage.getUsers();
    }
    return new ArrayList<>();
  }

  public List<GroupDetails> getGroups() {
    if (authenticationStorage != null) {
      return authenticationStorage.getGroups();
    }
    return new ArrayList<>();
  }

  public void delGroup(String groupName) throws IOException {
    authenticationStorage.delGroup(groupName);
  }

  public GroupIdMap addGroup(String groupName) throws IOException {
    return authenticationStorage.addGroup(groupName);
  }

  public void addUserToGroup(String user, String group) throws IOException {
    authenticationStorage.addUserToGroup(user, group);
  }

  public void removeUserFromGroup(String username, String groupName) throws IOException {
    authenticationStorage.removeUserFromGroup(username, groupName);
  }
}
