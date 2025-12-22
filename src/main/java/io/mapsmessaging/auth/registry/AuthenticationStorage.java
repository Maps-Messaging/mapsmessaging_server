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

package io.mapsmessaging.auth.registry;

import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.auth.ServerTraversalFactory;
import io.mapsmessaging.auth.priviliges.PrivilegeSerializer;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.auth.registry.mapping.GroupIdSerializer;
import io.mapsmessaging.auth.registry.mapping.IdDbStore;
import io.mapsmessaging.auth.registry.mapping.UserIdSerializer;
import io.mapsmessaging.auth.registry.principal.SessionPrivilegePrincipal;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.security.SubjectHelper;
import io.mapsmessaging.security.access.*;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.security.access.mapping.UserIdMap;
import io.mapsmessaging.security.access.monitor.AuthenticationMonitorConfig;
import io.mapsmessaging.security.authorisation.*;
import lombok.Getter;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import javax.security.auth.Subject;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import static io.mapsmessaging.logging.ServerLogMessages.AUTH_STORAGE_FAILED_ON_UPDATE;
import static io.mapsmessaging.logging.ServerLogMessages.AUTH_STORAGE_FAILED_TO_LOAD;

public class AuthenticationStorage {
  private static final String CERTIFICATE_STORE = "certificateStore";

  @Getter
  private final IdentityAccessManager identityAccessManager;
  private final UserPermisionManager userPermisionManager;
  private final AuthDbStoreManager dbStoreManager;
  @Getter
  private final boolean firstBoot;

  private final Logger logger = LoggerFactory.getLogger(AuthenticationStorage.class);

  public AuthenticationStorage(ConfigurationProperties config, AuthenticationMonitorConfig monitorConfig)  {
    String securityDirectory = config.getProperty("configDirectory", "./.security");
    if (securityDirectory != null) {
      File file = new File(securityDirectory);
      if (!file.exists()) {
        file.mkdirs();
      }
    }

    firstBoot = !(
        new File(securityDirectory + File.separator + ".auth.db").exists() ||
        new File(securityDirectory + File.separator + ".auth_tx.db").exists()
    );

    Map<String, Object> map = new LinkedHashMap<>(config.getMap());
    map.put("configDirectory", securityDirectory);
    map.put("passwordHandler", config.getProperty("passwordHandler"));

    if (config.containsKey(CERTIFICATE_STORE)) {
      Map<String, ?> cert = ((ConfigurationProperties) config.get(CERTIFICATE_STORE)).getMap();
      map.put(CERTIFICATE_STORE, cert);
    }

    String authProvider = config.getProperty("identityProvider", "Apache-Basic-Auth");
    try {
      dbStoreManager = new AuthDbStoreManager(securityDirectory);
      identityAccessManager = new IdentityAccessManager(authProvider, map, new IdDbStore<>(dbStoreManager.getUserMapSet()), new IdDbStore<>(dbStoreManager.getGroupMapSet()), new ServerTraversalFactory(), monitorConfig, ServerPermissions.values());
      userPermisionManager = new UserPermisionManager(dbStoreManager.getSessionPrivilegesMap());
    } catch (IOException e) {

      // ToDo: This is catastrophic and needs to be logged and the server stopped!, no auth(x) no server
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public boolean addUser(String username, char[] password, SessionPrivileges quotas, String[] groups) {
    try {
      UserManagement userManagement = identityAccessManager.getUserManagement();
      GroupManagement groupManagement = identityAccessManager.getGroupManagement();
      UserIdMap userIdMap = userManagement.createUser(username, password);
      UUID uuid = userIdMap.getAuthId();
      for (String group : groups) {
        if (groupManagement.getGroup(group) == null) {
          groupManagement.createGroup(group);
        }
        groupManagement.addUserToGroup(username, group);
      }
      quotas.setUniqueId(uuid);
      userPermisionManager.add(quotas);
      dbStoreManager.commit();
      return true;
    } catch (IOException | GeneralSecurityException e) {
      logger.log(AUTH_STORAGE_FAILED_TO_LOAD, e);
    }
    return false;
  }

  public boolean delUser(String username) {
    try {
      UserManagement userManagement = identityAccessManager.getUserManagement();
      Identity userIdMap = userManagement.getUser(username);
      if (userIdMap != null) {
        userManagement.deleteUser(username);
        userPermisionManager.delete(userIdMap.getId());
      }
      dbStoreManager.commit();
      return true;
    } catch (IOException e) {
      logger.log(AUTH_STORAGE_FAILED_ON_UPDATE, e);
    }
    return false;
  }


  public boolean validateUser(String username, char[] password, AuthContext context) throws IOException {
    return identityAccessManager.validateUser(username, password, context);
  }

  public SessionPrivileges getQuota(UUID userId) {
    return userPermisionManager.get(userId);
  }

  public Subject update(Subject subject) {
    Subject subject1 = identityAccessManager.updateSubject(subject);
    if (subject1 == null) return subject;
    UUID userId = SubjectHelper.getUniqueId(subject1);
    if (userId != null) {
      SessionPrivileges sessionPrivileges = userPermisionManager.get(userId);
      if (sessionPrivileges != null) {
        subject1.getPrincipals().add(new SessionPrivilegePrincipal(sessionPrivileges));
      }
    }
    return subject1;
  }

  public Identity findUser(String username) {
    return identityAccessManager.getUserManagement().getUser(username);
  }

  public Identity findUser(UUID uuid) {
    return identityAccessManager.getUserManagement().getAllUsers().stream().filter(userIdMap -> userIdMap.getId().equals(uuid)).findFirst().orElse(null);
  }

  public Group findGroup(String groupName) {
    return identityAccessManager.getGroupManagement().getGroup(groupName);
  }

  public Group findGroup(UUID uuid) {
    return identityAccessManager.getGroupManagement().getAllGroups().stream().filter(groupIdMap -> groupIdMap.getId().equals(uuid)).findFirst().orElse(null);
  }

  public List<UserDetails> getUsers() {
    List<Identity> userIdMaps = identityAccessManager.getUserManagement().getAllUsers();
    List<UserDetails> users = new ArrayList<>();
    for (Identity entry : userIdMaps) {
      List<UUID> groupIds = new ArrayList<>();
      List<Group> groupEntries = entry.getGroupList();
      for (Group group : groupEntries) {
        groupIds.add(group.getId());
      }
      UserDetails details = new UserDetails(
          entry,
          groupIds
      );
      users.add(details);
    }
    return users;
  }

  public List<GroupDetails> getGroups() {
    List<Group> groupIdMaps = identityAccessManager.getGroupManagement().getAllGroups();
    List<GroupDetails> groups = new ArrayList<>();
    for (Group entry : groupIdMaps) {
      List<UUID> userIds = new ArrayList<>();
      Set<String> userList = entry.getUserSet();
      for (String user : userList) {
        Identity userIdMap = identityAccessManager.getUserManagement().getUser(user);
        if (userIdMap != null) {
          userIds.add(userIdMap.getId());
        }
      }
      GroupDetails details = new GroupDetails(
          entry.getName(),
          entry.getId(),
          userIds
      );
      groups.add(details);
    }
    return groups;
  }

  public void delGroup(String groupName) throws IOException {
    identityAccessManager.getGroupManagement().deleteGroup(groupName);
    dbStoreManager.commit();
  }

  public GroupIdMap addGroup(String groupName) throws IOException {
    GroupIdMap id = identityAccessManager.getGroupManagement().createGroup(groupName);
    dbStoreManager.commit();
    return id;
  }

  public void addUserToGroup(String user, String group) throws IOException {
    identityAccessManager.getGroupManagement().addUserToGroup(user, group);
    dbStoreManager.commit();
  }

  public void removeUserFromGroup(String username, String groupName) throws IOException {
    identityAccessManager.getGroupManagement().removeUserFromGroup(username, groupName);
    dbStoreManager.commit();
  }

  public AuthorizationProvider getAuthorizationProvider() {
    return identityAccessManager.getAuthorizationProvider();
  }

  public boolean canAccess(Identity identity, Permission permission, ProtectedResource resource) {
    return identityAccessManager.getAuthorizationProvider().canAccess(identity, permission, resource);
  }

  public boolean hasAllAccess(List<AuthRequest> request) {
    return identityAccessManager.getAuthorizationProvider().hasAllAccess(request.toArray(new AuthRequest[0]));
  }

  public void grant(Identity identity, Permission permission, ProtectedResource resource) {
    identityAccessManager.getAuthorizationProvider().grant(identity, permission, resource);
  }

  public void deny(Identity identity, Permission permission, ProtectedResource resource) {
    identityAccessManager.getAuthorizationProvider().denyAccess(Grantee.forIdentity(identity), permission, resource);
  }

  public void grant(Group group, Permission permission, ProtectedResource resource) {
    identityAccessManager.getAuthorizationProvider().grant(group, permission, resource);
  }

  public void deny(Group group, Permission permission, ProtectedResource resource) {
    identityAccessManager.getAuthorizationProvider().denyAccess(Grantee.forGroup(group), permission, resource);
  }

  public void revoke(Identity identity, Permission permission, ProtectedResource resource) {
    identityAccessManager.getAuthorizationProvider().revoke(identity, permission, resource);
  }

  public void revoke(Group group, Permission permission, ProtectedResource resource) {
    identityAccessManager.getAuthorizationProvider().revoke(group, permission, resource);
  }



  public void close() {
    dbStoreManager.close();
  }
}
