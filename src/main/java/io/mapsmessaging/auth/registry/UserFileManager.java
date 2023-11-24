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

import io.mapsmessaging.security.access.mapping.UserIdMap;
import io.mapsmessaging.security.access.mapping.UserMapManagement;
import io.mapsmessaging.security.identity.parsers.PasswordParser;

import java.io.IOException;
import java.util.UUID;

public class UserFileManager extends FileManager {

  private final PasswordParser passwordParser;
  private final UserMapManagement userMapManagement;

  public UserFileManager(String filename, PasswordParser passwordParser, UserMapManagement mapManagement) {
    super(filename);
    this.passwordParser = passwordParser;
    this.userMapManagement = mapManagement;
  }

  public UUID addUser(String username, String password) throws IOException {
    String salt = PasswordGenerator.generateSalt(16);
    byte[] hash = passwordParser.computeHash(password.getBytes(), salt.getBytes(), 12);
    add(username + ":" + new String(hash));
    UserIdMap userIdMap = new UserIdMap(UUID.randomUUID(), username, "apache", "");
    userMapManagement.add(userIdMap);
    return userIdMap.getAuthId();
  }

  public void updateUser(String username, String password) throws IOException {
    deleteUser(username);
    addUser(username, password);
  }

  public void deleteUser(String username) throws IOException {
    delete(username);
    userMapManagement.delete("apache:" + username);
  }

  public UUID getUserUUID(String username) {
    UserIdMap map = userMapManagement.get("apache:" + username);
    if (map != null) {
      return map.getAuthId();
    }
    return null;
  }
}