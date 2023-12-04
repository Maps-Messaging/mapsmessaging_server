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

package io.mapsmessaging.auth.registry;

import io.mapsmessaging.auth.priviliges.PrivilegeSerializer;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.auth.registry.mapping.GroupIdSerializer;
import io.mapsmessaging.auth.registry.mapping.IdDbStore;
import io.mapsmessaging.auth.registry.mapping.UserIdSerializer;
import io.mapsmessaging.auth.registry.principal.SessionPrivilegePrincipal;
import io.mapsmessaging.security.SubjectHelper;
import io.mapsmessaging.security.access.IdentityAccessManager;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.security.access.mapping.UserIdMap;
import io.mapsmessaging.security.identity.GroupEntry;
import io.mapsmessaging.security.identity.IdentityEntry;
import io.mapsmessaging.security.identity.parsers.PasswordParser;
import io.mapsmessaging.security.identity.parsers.bcrypt.BCrypt2yPasswordParser;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import lombok.Getter;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import javax.security.auth.Subject;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class AuthenticationStorage implements Closeable {

  private final IdentityAccessManager identityAccessManager;
  private final PasswordParser globalPasswordParser;
  private final UserPermisionManager userPermisionManager;
  private final DB db;
  @Getter
  private final boolean firstBoot;

  public AuthenticationStorage(ConfigurationProperties config) {
    String securityDirectory = config.getProperty("configDirectory", "./.security");
    if (securityDirectory != null) {
      File file = new File(securityDirectory);
      if (!file.exists()) {
        file.mkdirs();
      }
    }

    firstBoot = new File(securityDirectory + File.separator + ".auth.db").exists();
    db = DBMaker.fileDB(securityDirectory + File.separator + ".auth.db")
        .checksumStoreEnable()
        .fileChannelEnable()
        .fileMmapEnableIfSupported()
        .fileMmapPreclearDisable()
        .closeOnJvmShutdown()
        .make();
    db.getStore().fileLoad();
    Map<UUID, UserIdMap> userMapSet = db.hashMap("userIdMap", new UUIDSerializer(), new UserIdSerializer()).createOrOpen();
    Map<UUID, GroupIdMap> groupMapSet = db.hashMap("groupIdMap", new UUIDSerializer(), new GroupIdSerializer()).createOrOpen();
    Map<UUID, SessionPrivileges> sessionPrivilegesMap = db.hashMap(UserPermisionManager.class.getName(), new UUIDSerializer(), new PrivilegeSerializer()).createOrOpen();
    String authProvider = config.getProperty("identityProvider", "Apache-Basic-Auth");

    identityAccessManager = new IdentityAccessManager(authProvider, Map.of("configDirectory", securityDirectory), new IdDbStore<UserIdMap>(userMapSet), new IdDbStore<GroupIdMap>(groupMapSet));
    globalPasswordParser = new BCrypt2yPasswordParser();
    userPermisionManager = new UserPermisionManager(sessionPrivilegesMap);
  }

  public boolean addUser(String username, String password, SessionPrivileges quotas, String[] groups) {
    try {
      UserIdMap userIdMap = identityAccessManager.createUser(username, password, globalPasswordParser);
      UUID uuid = userIdMap.getAuthId();
      for (String group : groups) {
        if (identityAccessManager.getGroup(group) == null) {
          identityAccessManager.createGroup(group);
        }
        identityAccessManager.addUserToGroup(username, group);
      }
      quotas.setUniqueId(uuid);
      userPermisionManager.add(quotas);
      return true;
    } catch (IOException e) {

    }
    return false;
  }

  public boolean delUser(String username) {
    try {
      UserIdMap userIdMap = identityAccessManager.getUser(username);
      if (userIdMap != null) {
        identityAccessManager.deleteUser(username);
        userPermisionManager.delete(userIdMap.getAuthId());
      }
      return true;
    } catch (IOException e) {

    }
    return false;
  }


  public boolean validateUser(String username, String password) {
    IdentityEntry identityEntry = identityAccessManager.getUserIdentity(username);
    if (identityEntry != null) {
      PasswordParser passwordParser = identityEntry.getPasswordParser();
      byte[] hash = passwordParser.computeHash(password.getBytes(), passwordParser.getSalt(), passwordParser.getCost());
      return Arrays.equals(hash, identityEntry.getPassword().getBytes());
    }
    return false;
  }

  @Override
  public void close() throws IOException {
    db.close();
  }

  public SessionPrivileges getQuota(UUID userId) {
    return userPermisionManager.get(userId);
  }

  public Subject update(Subject subject) {
    Subject subject1 = identityAccessManager.updateSubject(subject);
    UUID userId = SubjectHelper.getUniqueId(subject1);
    if (userId != null) {
      SessionPrivileges sessionPrivileges = userPermisionManager.get(userId);
      if (sessionPrivileges != null) {
        subject1.getPrincipals().add(new SessionPrivilegePrincipal(sessionPrivileges));
      }
    }
    return subject1;
  }

  public UserIdMap findUser(String username) {
    return identityAccessManager.getUser(username);
  }

  public UserIdMap findUser(UUID uuid) {
    return identityAccessManager.getAllUsers().stream().filter(userIdMap -> userIdMap.getAuthId().equals(uuid)).findFirst().orElse(null);
  }

  public List<UserDetails> getUsers() {
    List<UserIdMap> userIdMaps = identityAccessManager.getAllUsers();
    List<UserDetails> users = new ArrayList<>();
    for (UserIdMap userIdMap : userIdMaps) {
      IdentityEntry entry = identityAccessManager.getUserIdentity(userIdMap.getUsername());
      List<UUID> groupIds = new ArrayList<>();
      List<GroupEntry> groupEntries = entry.getGroups();
      for (GroupEntry groupEntry : groupEntries) {
        GroupIdMap groupIdMap = identityAccessManager.getGroup(groupEntry.getName());
        if (groupIdMap != null) {
          groupIds.add(groupIdMap.getAuthId());
        }
      }
      UserDetails details = new UserDetails(
          userIdMap,
          entry,
          groupIds
      );
      users.add(details);
    }
    return users;
  }

  public List<GroupDetails> getGroups() {
    List<GroupIdMap> groupIdMaps = identityAccessManager.getAllGroups();
    List<GroupDetails> groups = new ArrayList<>();
    for (GroupIdMap groupIdMap : groupIdMaps) {
      GroupEntry entry = identityAccessManager.getGroupDetails(groupIdMap.getGroupName());
      List<UUID> userIds = new ArrayList<>();
      Set<String> userList = entry.getUsers();
      for (String user : userList) {
        UserIdMap userIdMap = identityAccessManager.getUser(user);
        if (userIdMap != null) {
          userIds.add(userIdMap.getAuthId());
        }
      }
      GroupDetails details = new GroupDetails(
          groupIdMap.getGroupName(),
          groupIdMap.getAuthId(),
          userIds
      );
      groups.add(details);
    }
    return groups;
  }

  public void delGroup(String groupName) throws IOException {
    identityAccessManager.deleteGroup(groupName);
  }

  public GroupIdMap addGroup(String groupName) throws IOException {
    return identityAccessManager.createGroup(groupName);
  }

  public void addUserToGroup(String user, String group) throws IOException {
    identityAccessManager.addUserToGroup(user, group);
  }

  public void removeUserFromGroup(String username, String groupName) throws IOException {
    identityAccessManager.removeUserFromGroup(username, groupName);
  }
}
