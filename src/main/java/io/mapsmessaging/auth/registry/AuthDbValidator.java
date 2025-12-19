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

import io.mapsmessaging.security.access.mapping.DomainIdMapping;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.security.access.mapping.UserIdMap;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public final class AuthDbValidator {

  public static void validate(
      Map<UUID, UserIdMap> userMap,
      Map<UUID, GroupIdMap> groupMap) throws IOException {

    validateUserMap(userMap);
    validateGroupMap(groupMap);
  }

  private static void validateUserMap(Map<UUID, UserIdMap> userMap) throws IOException {
    for (Map.Entry<UUID, UserIdMap> entry : userMap.entrySet()) {
      UUID key = entry.getKey();
      UserIdMap value = entry.getValue();

      if (key == null) {
        throw new IOException("User map contains null UUID key");
      }
      if (value == null) {
        throw new IOException("User map contains null value for UUID " + key);
      }
      validateMapping("userIdMap", key, value);
    }
  }

  private static void validateGroupMap(Map<UUID, GroupIdMap> groupMap) throws IOException {
    for (Map.Entry<UUID, GroupIdMap> entry : groupMap.entrySet()) {
      UUID key = entry.getKey();
      GroupIdMap value = entry.getValue();

      if (key == null) {
        throw new IOException("Group map contains null UUID key");
      }
      if (value == null) {
        throw new IOException("Group map contains null value for UUID " + key);
      }
      validateMapping("groupIdMap", key, value);
    }
  }

  private static void validateMapping(String mapName, UUID key, DomainIdMapping mapping) throws IOException {
    UUID authId = mapping.getAuthId();
    if (authId == null) {
      throw new IOException(mapName + " has null authId for key " + key);
    }
    if (!key.equals(authId)) {
      throw new IOException(mapName + " key/authId mismatch. key=" + key + " authId=" + authId);
    }

    String id = mapping.getId();
    if (id == null || id.isBlank()) {
      throw new IOException(mapName + " has invalid id for UUID " + key);
    }

    String authDomain = mapping.getAuthDomain();
    if (authDomain == null || authDomain.isBlank()) {
      throw new IOException(mapName + " has invalid authDomain for UUID " + key);
    }
  }
  private AuthDbValidator() {
  }
}