/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.rest.api.impl.auth;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.rest.responses.StatusResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class AuthResourceValidationResponseTest {

  private final UserManagementApi users = new UserManagementApi() {
    @Override
    protected void hasAccess(String resource) {
      // Isolate UUID/response contracts from authentication service availability.
    }
  };
  private final GroupManagementApi groups = new GroupManagementApi() {
    @Override
    protected void hasAccess(String resource) {
      // Isolate UUID/response contracts from authentication service availability.
    }
  };

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "invalid"})
  void invalid_uuids_preserve_bad_request_diagnostics(String uuid) {
    assertResponse(users.getUser(uuid), 400, "Invalid UUID");
    assertResponse(users.deleteUser(uuid), 400, "Invalid UUID");
    assertResponse(users.changeUserPassword(uuid, null), 400, "Invalid UUID");
    assertResponse(groups.getGroupById(uuid), 400, "Invalid UUID");
    assertResponse(groups.deleteGroup(uuid), 400, "Invalid UUID");
    assertResponse(groups.addUserToGroup(uuid, uuid), 400, "Invalid UUID");
    assertResponse(groups.removeUserFromGroup(uuid, uuid), 400, "Invalid UUID");
  }

  @Test
  void missing_users_and_groups_preserve_not_found_diagnostics() {
    AuthManager manager = mock(AuthManager.class);
    when(manager.getUsers()).thenReturn(List.of());
    when(manager.getGroups()).thenReturn(List.of());
    try (MockedStatic<AuthManager> singleton = mockStatic(AuthManager.class)) {
      singleton.when(AuthManager::getInstance).thenReturn(manager);
      String uuid = UUID.randomUUID().toString();
      assertResponse(users.getUser(uuid), 404, "User not found");
      assertResponse(users.deleteUser(uuid), 404, "User not found");
      assertResponse(groups.getGroupById(uuid), 404, "Group not found");
      assertResponse(groups.deleteGroup(uuid), 404, "Group not found");
    }
  }

  @Test
  void resource_paths_preserve_existing_routes() {
    assertEquals(URI_PATH + "/auth/users", UserManagementApi.class.getAnnotation(Path.class).value());
    assertEquals(URI_PATH + "/auth/groups", GroupManagementApi.class.getAnnotation(Path.class).value());
  }

  private static void assertResponse(Response response, int status, String text) {
    try (response) {
      assertEquals(status, response.getStatus());
      assertEquals(text, assertInstanceOf(StatusResponse.class, response.getEntity()).getStatus());
    }
  }
}
