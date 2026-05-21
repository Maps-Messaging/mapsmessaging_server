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

import java.util.List;
import java.util.Map;
import java.util.UUID;

class LockedUsersApiTest extends ApiTestBase {

  private static final String BASE_PATH = "/api/v1/auth/user-lockouts";
  private static final String USERS_PATH = "/api/v1/auth/users";

  @Test
  void getAllLockedUsers_returns200AndJson() {
    givenAuthenticated()
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);
  }

  @Test
  void unlockUser_invalidUuid_returns400() {
    givenAuthenticatedNoValidation()
        .when()
        .delete(BASE_PATH + "/not-a-uuid")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void unlockUser_notFound_returns404() {
    UUID missingUuid = UUID.randomUUID();

    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + missingUuid)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON);
  }

  @Test
  void unlockUser_existingUser_returns200() {
    UUID anyUserUuid = findAnyExistingUserUuid();
    Assertions.assertNotNull(anyUserUuid, "No users returned by /auth/users, cannot test unlock endpoint");

    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + anyUserUuid)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);
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
