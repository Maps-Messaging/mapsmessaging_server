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
import io.mapsmessaging.rest.responses.StatusResponse;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class UserManagementApiTest extends ApiTestBase {

  private static final String BASE_PATH = "/api/v1/auth/users";

  @Test
  void create_get_changePassword_delete_happyPath() {
    String username = "it_user_" + System.currentTimeMillis();
    String initialPassword = "it_pw_" + System.currentTimeMillis();
    String newPassword = "it_pw2_" + System.currentTimeMillis();

    createUser(username, initialPassword);

    UUID userUuid = findUserUuidByUsername(username);
    Assertions.assertNotNull(userUuid);

    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/" + userUuid)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);

    JsonPath getUserJson = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/" + userUuid)
        .then()
        .statusCode(200)
        .extract()
        .jsonPath();

    String returnedUsername = getFirstString(getUserJson, "username");
    Assertions.assertEquals(username, returnedUsername);

    givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"newPassword\":\"" + newPassword + "\"}")
        .when()
        .put(BASE_PATH + "/" + userUuid + "/password")
        .then()
        .statusCode(204);

    deleteUser(userUuid, username);

    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/" + userUuid)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON);
  }

  @Test
  void getAllUsers_invalidFilter_returns400() {
    givenAuthenticated()
        .queryParam("filter", "username = 'x' and (") // intentionally broken selector
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void getUser_invalidUuid_returns400() {
    givenAuthenticatedNoValidation()
        .when()
        .get(BASE_PATH + "/not-a-uuid")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void getUser_notFound_returns404() {
    UUID missingUuid = UUID.randomUUID();

    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/" + missingUuid)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON);
  }

  @Test
  void createUser_blankUsername_returns400() {
    givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"username\":\" \",\"password\":\"pw\"}")
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void createUser_missingPassword_returns400() {
    givenAuthenticatedNoValidation()
        .contentType(ContentType.JSON)
        .body("{\"username\":\"it_user_" + System.currentTimeMillis() + "\"}")
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void createUser_conflict_returns409() {
    String username = "it_user_" + System.currentTimeMillis();
    String password = "it_pw_" + System.currentTimeMillis();

    createUser(username, password);

    givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(409)
        .contentType(ContentType.JSON);

    UUID userUuid = findUserUuidByUsername(username);
    deleteUser(userUuid, username);
  }

  @Test
  void deleteUser_invalidUuid_returns400() {
    givenAuthenticatedNoValidation()
        .when()
        .delete(BASE_PATH + "/not-a-uuid")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void deleteUser_notFound_returns404() {
    UUID missingUuid = UUID.randomUUID();

    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + missingUuid)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON);
  }

  @Test
  void changePassword_invalidUuid_returns400() {
    givenAuthenticatedNoValidation()
        .contentType(ContentType.JSON)
        .body("{\"newPassword\":\"pw\"}")
        .when()
        .put(BASE_PATH + "/not-a-uuid/password")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void changePassword_blankNewPassword_returns400() {
    UUID uuid = UUID.randomUUID();

    givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"newPassword\":\" \"}")
        .when()
        .put(BASE_PATH + "/" + uuid + "/password")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void changePassword_notFound_returns404() {
    UUID missingUuid = UUID.randomUUID();

    givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"newPassword\":\"it_pw_" + System.currentTimeMillis() + "\"}")
        .when()
        .put(BASE_PATH + "/" + missingUuid + "/password")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON);
  }

  private void createUser(String username, String password) {
    Assertions.assertTrue(username.startsWith("it_") || username.startsWith("it"));

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(201)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    StatusResponse statusResponse = response.as(StatusResponse.class);
    Assertions.assertNotNull(statusResponse);
  }

  private UUID findUserUuidByUsername(String username) {
    String filter = "username = '" + username + "'";

    JsonPath jsonPath = givenAuthenticated()
        .queryParam("filter", filter)
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .jsonPath();

    int size = jsonPath.getList("$").size();
    Assertions.assertTrue(size >= 1);

    String uuidText = firstNonBlank(
        jsonPath.getString("[0].uniqueId"),
        jsonPath.getString("[0].id"),
        jsonPath.getString("[0].uuid")
    );

    Assertions.assertNotNull(uuidText);
    return UUID.fromString(uuidText);
  }

  private void deleteUser(UUID userUuid, String username) {
    Assertions.assertNotNull(userUuid);
    Assertions.assertNotNull(username);
    Assertions.assertTrue(username.startsWith("it_"));

    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + userUuid)
        .then()
        .statusCode(204);
  }

  private String getFirstString(JsonPath jsonPath, String fieldName) {
    Object value = jsonPath.get(fieldName);
    if (value == null) {
      return null;
    }
    if (value instanceof String stringValue) {
      return stringValue;
    }
    return String.valueOf(value);
  }

  private String firstNonBlank(String firstValue, String secondValue, String thirdValue) {
    if (firstValue != null && !firstValue.isBlank()) {
      return firstValue;
    }
    if (secondValue != null && !secondValue.isBlank()) {
      return secondValue;
    }
    if (thirdValue != null && !thirdValue.isBlank()) {
      return thirdValue;
    }
    return null;
  }
}
