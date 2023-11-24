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

import io.mapsmessaging.security.access.mapping.GroupMapManagement;
import io.mapsmessaging.security.access.mapping.UserMapManagement;
import io.mapsmessaging.security.identity.IdentityEntry;
import io.mapsmessaging.security.identity.IdentityLookup;
import io.mapsmessaging.security.identity.parsers.PasswordParser;
import io.mapsmessaging.security.identity.parsers.bcrypt.BCrypt2yPasswordParser;
import lombok.Getter;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class AuthenticationStorage implements Closeable {

  @Getter
  private final UserFileManager userFile;
  @Getter
  private final GroupFileManager groupFile;

  private final PasswordParser passwordParser;
  private final UserMapManagement userMapManagement;
  private final GroupMapManagement groupMapManagement;
  private final UserPermisionManager userPermisionManager;

  public AuthenticationStorage(String securityDirectory) {
    if (securityDirectory != null) {
      File file = new File(securityDirectory);
      if (!file.exists()) {
        file.mkdirs();
      }
    }
    userMapManagement = new UserMapManagement(securityDirectory + File.separator + ".userMap");
    groupMapManagement = new GroupMapManagement(securityDirectory + File.separator + ".groupMap");
    UserMapManagement.setGlobalInstance(userMapManagement);
    GroupMapManagement.setGlobalInstance(groupMapManagement);
    passwordParser = new BCrypt2yPasswordParser();
    this.groupFile = new GroupFileManager(securityDirectory + File.separator + ".htgroup", groupMapManagement);
    this.userFile = new UserFileManager(securityDirectory + File.separator + ".htpassword", passwordParser, userMapManagement);
    userPermisionManager = new UserPermisionManager(securityDirectory + File.separator + ".userPermissions");
  }


  public boolean addUser(String username, String password, Quotas quotas, String[] groups) {
    try {
      UUID uuid = userFile.addUser(username, password);
      groupFile.addGroup(username, groups);
      quotas.setUuid(uuid);
      userPermisionManager.add(quotas);
      return true;
    } catch (IOException e) {

    }
    return false;
  }

  public boolean delUser(String username) {
    try {
      UUID userId = userFile.getUserUUID(username);
      userFile.deleteUser(username);
      //groupFile.deleteGroup(username);
      userPermisionManager.delete(userId);
      return true;
    } catch (IOException e) {

    }
    return false;
  }


  public boolean validateUser(String username, String password, IdentityLookup identityLookup) {
    IdentityEntry identityEntry = identityLookup.findEntry(username);
    if (identityEntry != null) {
      PasswordParser passwordParser = identityEntry.getPasswordParser();
      byte[] hash = passwordParser.computeHash(password.getBytes(), passwordParser.getSalt(), passwordParser.getCost());
      return Arrays.equals(hash, identityEntry.getPassword().getBytes());
    }
    return false;
  }

  @Override
  public void close() throws IOException {
    userPermisionManager.close();
  }

  public Quotas getQuota(UUID userId) {
    return userPermisionManager.get(userId);
  }
}
