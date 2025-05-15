/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.auth;

import static io.mapsmessaging.logging.ServerLogMessages.*;

import com.sun.security.auth.UserPrincipal;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.auth.registry.AuthenticationStorage;
import io.mapsmessaging.auth.registry.GroupDetails;
import io.mapsmessaging.auth.registry.PasswordGenerator;
import io.mapsmessaging.auth.registry.UserDetails;
import io.mapsmessaging.config.AuthManagerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.security.access.Group;
import io.mapsmessaging.security.access.Identity;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.security.identity.IdentityLookupFactory;
import io.mapsmessaging.security.identity.principals.UniqueIdentifierPrincipal;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.SystemProperties;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import javax.security.auth.Subject;
import lombok.Getter;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class AuthManager implements Agent {


  private static final String ADMIN_USER = "admin";
  private static final String USER = "user";

  private static final String ADMIN_GROUP = "admin";
  private static final String EVERYONE = "everyone";

  private static class Holder {
    static final AuthManager INSTANCE = new AuthManager();
  }
  public static AuthManager getInstance() {
    return Holder.INSTANCE;
  }


  private final Logger logger;

  @Getter
  private final AuthManagerConfig config;
  private AuthenticationStorage authenticationStorage;
  private final Map<String, Subject> subjectMap = new WeakHashMap<>();

  @Getter
  private final boolean authenticationEnabled;
  @Getter
  private final boolean authorisationEnabled;

  private String errMessage="";

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
      String jaasConfig =  SystemProperties.getInstance().getProperty("java.security.auth.login.config");
      boolean exists = false;
      String msg ="Authentication enabled but login config file does not exist Configured:"+ jaasConfig+", please configure -Djava.security.auth.login.config=<valid path to jaas.config>";
      if (jaasConfig != null) {
        File loginConfig = new File(jaasConfig);
        exists = loginConfig.exists();
      }
      else{
        msg = "Authentication enabled but login config not configured, please configure -Djava.security.auth.login.config=<valid path to jaas.config>";
      }
      if(!exists || jaasConfig.isEmpty() ) {
        throw new NoLoginContextConfigException(msg);
      }
      try {
        authenticationStorage = new AuthenticationStorage(new ConfigurationProperties(config.getAuthConfig()));
        if (authenticationStorage.isFirstBoot()) {
          createInitialUsers((String)config.getAuthConfig().get("configDirectory"));
        }
        IdentityLookupFactory.getInstance().registerSiteIdentityLookup("system", authenticationStorage.getIdentityAccessManager().getIdentityLookup());
      } catch (Exception e) {
        errMessage = e.getMessage();
        logger.log(AUTH_STARTUP_FAILED, e);
      }
    }
  }

  private void saveInitialUserDetails(String path, String[][] details) {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + File.separator + "admin_password", true))) {
      for (String[] detail : details) {
        bw.write(detail[0] + "=" + detail[1]);
        bw.newLine(); // Add a newline character after each line
      }
    } catch (IOException e) {
      logger.log(AUTH_SAVE_FAILED, e);
    }
  }

  @Override
  public void stop() {
    if (authenticationStorage != null) authenticationStorage.close();
  }

  public boolean addUser(String username, char[] password, SessionPrivileges quotas, String[] groups) {
    if (authenticationStorage != null) {
      logger.log(AUTH_ADDED_USER, username);
      return authenticationStorage.addUser(username, password, quotas, groups);
    }
    return false;
  }

  public boolean delUser(String username) {
    if (authenticationStorage != null) {
      logger.log(AUTH_DELETED_USER, username);
      return authenticationStorage.delUser(username);
    }
    return false;
  }

  public Subject update(Subject subject) {
    if (subject == null) subject = new Subject();
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
    config = ConfigurationManager.getInstance().getConfiguration(AuthManagerConfig.class);
    if (config != null) {
      authenticationEnabled = config.isAuthenticationEnabled();
      authorisationEnabled = config.isAuthorisationEnabled();
    }
    else{
      authenticationEnabled = false;
      authorisationEnabled = false;
    }
  }

  private void createInitialUsers(String path) throws IOException {
    String password = PasswordGenerator.generateRandomPassword(12);
    if (!addUser(ADMIN_USER, password.toCharArray(), SessionPrivileges.create(ADMIN_USER), new String[]{ADMIN_GROUP, EVERYONE})) {
      logger.log(SECURITY_MANAGER_FAILED_TO_CREATE_USER, ADMIN_USER);
    }

    String userpassword = PasswordGenerator.generateRandomPassword(12);
    if (!addUser(USER, userpassword.toCharArray(), SessionPrivileges.create(USER), new String[]{EVERYONE})) {
      logger.log(SECURITY_MANAGER_FAILED_TO_CREATE_USER, USER);
    }

    saveInitialUserDetails(path, new String[][]{{ADMIN_USER, password}, {USER, userpassword}});
    if (!authenticationStorage.validateUser(ADMIN_USER, password.toCharArray())) {
      logger.log(SECURITY_MANAGER_FAILED_TO_INITIALISE_USER, USER);
    }
    if (!authenticationStorage.validateUser(USER, userpassword.toCharArray())) {
      logger.log(SECURITY_MANAGER_FAILED_TO_INITIALISE_USER, USER);
    }
  }

  public boolean validate(String username, char[] password) throws IOException {
    return authenticationStorage.validateUser(username, password);
  }

  public Identity getUserIdentity(String username) {
    return authenticationStorage.findUser(username);
  }

  public Identity getUserIdentity(UUID uuid) {
    return authenticationStorage.findUser(uuid);
  }

  public Group getGroupIdentity(String groupName){
    return authenticationStorage.findGroup(groupName);
  }

  public Group getGroupIdentity(UUID uuid){
    return authenticationStorage.findGroup(uuid);
  }

  public Subject getUserSubject(String username) {
    Subject subject = subjectMap.get(username);
    if (subject == null) {
      subject = new Subject();
      Set<Principal> principalList = subject.getPrincipals();
      Identity map = authenticationStorage.findUser(username);
      if (map != null) {
        principalList.add(new UniqueIdentifierPrincipal(map.getId()));
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
    logger.log(AUTH_DELETED_GROUP, groupName);
    authenticationStorage.delGroup(groupName);
  }

  public GroupIdMap addGroup(String groupName) throws IOException {
    logger.log(AUTH_ADDED_GROUP, groupName);
    return authenticationStorage.addGroup(groupName);
  }

  public void addUserToGroup(String user, String group) throws IOException {
    logger.log(AUTH_MODIFIED_GROUP, user, "added to",  group);
    authenticationStorage.addUserToGroup(user, group);
  }

  public void removeUserFromGroup(String username, String groupName) throws IOException {
    logger.log(AUTH_MODIFIED_GROUP, username, "removed from",  groupName);
    authenticationStorage.removeUserFromGroup(username, groupName);
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("");
    if (authenticationEnabled) {
      status.setStatus(Status.OK);
      if(!errMessage.isEmpty()){
        status.setComment(errMessage);
        status.setStatus(Status.ERROR);
      }
    }
    else{
      status.setStatus(Status.DISABLED);
    }
    return status;
  }

  private static class NoLoginContextConfigException extends RuntimeException {

    public NoLoginContextConfigException(String s) {
      super(s);
    }
  }
}
