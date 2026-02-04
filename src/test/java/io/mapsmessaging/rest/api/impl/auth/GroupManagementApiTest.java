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

import io.mapsmessaging.rest.ApiTestBase;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class GroupManagementApiTest extends ApiTestBase {

  private static final String BASE_PATH = "/api/v1/auth/groups";
  private static final String USERS_PATH = "/api/v1/auth/users";

  @Test
  void create_get_addUser_removeUser_delete_happyPath() {
    String groupName = "it_group_" + Instant.now().getEpochSecond();

    createGroup(groupName);

    UUID groupUuid = findGroupUuidByName(groupName);
    Assertions.assertNotNull(groupUuid, "Created group not found in group list: " + groupName);

    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/" + groupUuid)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);

    UUID userUuid = findAnyExistingUserUuid();
    Assertions.assertNotNull(userUuid, "No users returned by /auth/users, cannot test membership endpoints");

    givenAuthenticated()
        .when()
        .post(BASE_PATH + "/" + groupUuid + "/" + userUuid)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);

    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + groupUuid + "/" + userUuid)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);

    deleteGroup(groupUuid, groupName);

    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/" + groupUuid)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON);
  }

  @Test
  void getAllGroups_invalidFilter_returns400() {
    givenAuthenticated()
        .queryParam("filter", "name = 'x' and (")
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void getGroup_invalidUuid_returns400() {
    givenAuthenticatedNoValidation()
        .when()
        .get(BASE_PATH + "/not-a-uuid")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void getGroup_notFound_returns404() {
    UUID missingUuid = UUID.randomUUID();

    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/" + missingUuid)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON);
  }

  @Test
  void createGroup_blankName_returns400() {
    givenAuthenticated()
        .contentType("text/plain")
        .body("   ")
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void createGroup_duplicate_returns409() {
    String groupName = "it_group_dup" + Instant.now().getEpochSecond();

    createGroup(groupName);

    givenAuthenticated()
        .contentType("text/plain")
        .body(groupName)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(409)
        .contentType(ContentType.JSON);

    UUID groupUuid = findGroupUuidByName(groupName);
    deleteGroup(groupUuid, groupName);
  }

  @Test
  void deleteGroup_invalidUuid_returns400() {
    givenAuthenticatedNoValidation()
        .when()
        .delete(BASE_PATH + "/not-a-uuid")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void deleteGroup_notFound_returns404() {
    UUID missingUuid = UUID.randomUUID();

    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + missingUuid)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON);
  }

  @Test
  void addUserToGroup_invalidUuid_returns400() {
    UUID validGroupUuid = UUID.randomUUID();

    givenAuthenticatedNoValidation()
        .when()
        .post(BASE_PATH + "/" + validGroupUuid + "/not-a-uuid")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void addUserToGroup_groupNotFound_returns404() {
    UUID missingGroupUuid = UUID.randomUUID();
    UUID anyUserUuid = findAnyExistingUserUuid();
    Assertions.assertNotNull(anyUserUuid);

    givenAuthenticated()
        .when()
        .post(BASE_PATH + "/" + missingGroupUuid + "/" + anyUserUuid)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON);
  }

  @Test
  void removeUserFromGroup_groupNotFound_returns404() {
    UUID missingGroupUuid = UUID.randomUUID();
    UUID anyUserUuid = findAnyExistingUserUuid();
    Assertions.assertNotNull(anyUserUuid);

    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + missingGroupUuid + "/" + anyUserUuid)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON);
  }

  private void createGroup(String groupName) {
    Assertions.assertNotNull(groupName);
    Assertions.assertTrue(groupName.startsWith("it_"), "Refusing to create non-test group: " + groupName);

    givenAuthenticated()
        .contentType("text/plain")
        .body(groupName)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(201)
        .contentType(ContentType.JSON);
  }

  private void deleteGroup(UUID groupUuid, String groupName) {
    Assertions.assertNotNull(groupUuid);
    Assertions.assertNotNull(groupName);
    Assertions.assertTrue(groupName.startsWith("it_"), "Refusing to delete non-test group: " + groupName);

    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + groupUuid)
        .then()
        .statusCode(204);
  }

  private UUID findGroupUuidByName(String groupName) {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    JsonPath jsonPath = response.jsonPath();
    List<Object> list = jsonPath.getList("$");
    if (list == null || list.isEmpty()) {
      return null;
    }

    for (Object item : list) {
      if (!(item instanceof Map)) {
        continue;
      }
      Map<?, ?> map = (Map<?, ?>) item;

      String name = getFirstString(map, "name", "groupName", "displayName");
      if (name == null || !name.equals(groupName)) {
        continue;
      }

      String uuidText = getFirstString(map, "uniqueId", "groupUuid", "id");
      if (uuidText != null && !uuidText.isBlank()) {
        return UUID.fromString(uuidText);
      }
    }

    return null;
  }

  private UUID findAnyExistingUserUuid() {
    Response response = givenAuthenticated()
        .when()
        .get(USERS_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    JsonPath jsonPath = response.jsonPath();
    List<Object> list = jsonPath.getList("$");
    if (list == null || list.isEmpty()) {
      return null;
    }

    for (Object item : list) {
      if (!(item instanceof Map)) {
        continue;
      }
      Map<?, ?> map = (Map<?, ?>) item;

      String uuidText = getFirstString(map, "uniqueId", "userUuid", "id");
      if (uuidText != null && !uuidText.isBlank()) {
        return UUID.fromString(uuidText);
      }
    }

    return null;
  }

  private String getFirstString(Map<?, ?> map, String firstKey, String secondKey, String thirdKey) {
    Object value = map.get(firstKey);
    if (value instanceof String stringValue && !stringValue.isBlank()) {
      return stringValue;
    }
    value = map.get(secondKey);
    if (value instanceof String stringValue && !stringValue.isBlank()) {
      return stringValue;
    }
    value = map.get(thirdKey);
    if (value instanceof String stringValue && !stringValue.isBlank()) {
      return stringValue;
    }
    return null;
  }
}
