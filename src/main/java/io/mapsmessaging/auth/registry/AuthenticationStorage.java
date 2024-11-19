/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.auth.registry;

import io.mapsmessaging.auth.priviliges.PrivilegeSerializer;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.auth.registry.mapping.GroupIdSerializer;
import io.mapsmessaging.auth.registry.mapping.IdDbStore;
import io.mapsmessaging.auth.registry.mapping.UserIdSerializer;
import io.mapsmessaging.auth.registry.principal.SessionPrivilegePrincipal;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.security.SubjectHelper;
import io.mapsmessaging.security.access.*;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.security.access.mapping.UserIdMap;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import javax.security.auth.Subject;
import lombok.Getter;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class AuthenticationStorage implements Closeable {
  @Getter
  private final IdentityAccessManager identityAccessManager;
  private final UserPermisionManager userPermisionManager;
  private final DB db;
  @Getter
  private final boolean firstBoot;

  public AuthenticationStorage(ConfigurationProperties config) throws Exception {
    String securityDirectory = config.getProperty("configDirectory", "./.security");
    if (securityDirectory != null) {
      File file = new File(securityDirectory);
      if (!file.exists()) {
        file.mkdirs();
      }
    }

    firstBoot = !(new File(securityDirectory + File.separator + ".auth.db").exists());
    db = DBMaker.fileDB(securityDirectory + File.separator + ".auth.db")
        .checksumStoreEnable()
        .cleanerHackEnable()
        .fileChannelEnable()
        .fileMmapEnableIfSupported()
        .fileMmapPreclearDisable()
        .closeOnJvmShutdown()
        .make();
    db.getStore().fileLoad();
    Map<UUID, UserIdMap> userMapSet = db.hashMap("userIdMap", new UUIDSerializer(), new UserIdSerializer()).createOrOpen();
    Map<UUID, GroupIdMap> groupMapSet = db.hashMap("groupIdMap", new UUIDSerializer(), new GroupIdSerializer()).createOrOpen();
    Map<UUID, SessionPrivileges> sessionPrivilegesMap = db.hashMap(UserPermisionManager.class.getName(), new UUIDSerializer(), new PrivilegeSerializer()).createOrOpen();

    Map<String, Object> map = new LinkedHashMap<>(config.getMap());
    map.put("configDirectory", securityDirectory);
    map.put("passwordHandler", config.getProperty("passwordHandler"));

    if (config.containsKey("certificateStore")) {
      Map<String, ?> cert = ((ConfigurationProperties) config.get("certificateStore")).getMap();
      map.put("certificateStore", cert);
    }

    String authProvider = config.getProperty("identityProvider", "Apache-Basic-Auth");
    identityAccessManager = new IdentityAccessManager(authProvider, map, new IdDbStore<>(userMapSet), new IdDbStore<>(groupMapSet));
    userPermisionManager = new UserPermisionManager(sessionPrivilegesMap);
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
      return true;
    } catch (IOException | GeneralSecurityException e) {

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
      return true;
    } catch (IOException e) {

    }
    return false;
  }


  public boolean validateUser(String username, char[] password) throws IOException {
    return identityAccessManager.validateUser(username, password);
  }

  @Override
  public void close() throws IOException {
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
  }

  public GroupIdMap addGroup(String groupName) throws IOException {
    return identityAccessManager.getGroupManagement().createGroup(groupName);
  }

  public void addUserToGroup(String user, String group) throws IOException {
    identityAccessManager.getGroupManagement().addUserToGroup(user, group);
  }

  public void removeUserFromGroup(String username, String groupName) throws IOException {
    identityAccessManager.getGroupManagement().removeUserFromGroup(username, groupName);
  }

}
