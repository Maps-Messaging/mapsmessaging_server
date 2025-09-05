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

import io.mapsmessaging.auth.priviliges.SessionPrivileges;

import java.util.Map;
import java.util.UUID;

public class UserPermisionManager {

  private final Map<UUID, SessionPrivileges> store;

  public UserPermisionManager(Map<UUID, SessionPrivileges> map) {
    store = map;
  }


  public void add(SessionPrivileges userDetails) {
    store.put(userDetails.getUniqueId(), userDetails);
  }

  public void update(SessionPrivileges userDetails) {
    store.put(userDetails.getUniqueId(), userDetails);
  }

  public void delete(UUID uuid) {
    store.remove(uuid);
  }

  public SessionPrivileges get(UUID userId) {
    return store.get(userId);
  }
}
