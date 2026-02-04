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
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuthorisationResourceTest extends ApiTestBase {

  private static final String PERMISSIONS_PATH = "/api/v1/auth/permissions";
  private static final String RESOURCE_ACL_PATH = "/api/v1/auth/resources/acl";
  private static final String IDENTITY_ACL_PATH = "/api/v1/auth/identities";
  private static final String GROUP_ACL_PATH = "/api/v1/auth/groups";
  private static final String USERS_PATH = "/api/v1/auth/users";
  private static final String GROUPS_PATH = "/api/v1/auth/groups";
  private static final String ACL_CHECK_PATH = "/api/v1/auth/acl/check";
  private static final String RESOURCE_TYPE_PARAM = "resourceType";
  private static final String RESOURCE_KEY_PARAM = "resourceKey";

  @Test
  void getPermissions_returns200AndJson() {
    givenAuthenticated()
        .when()
        .get(PERMISSIONS_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);
  }

  @Test
  void getResourceAcl_blankResourceType_returns400() {
    givenAuthenticatedNoValidation()
        .queryParam(RESOURCE_TYPE_PARAM, "   ")
        .queryParam(RESOURCE_KEY_PARAM, "/some/resource")
        .when()
        .get(RESOURCE_ACL_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void getResourceAcl_blankResourceKey_returns400() {
    givenAuthenticatedNoValidation()
        .queryParam(RESOURCE_TYPE_PARAM, "TOPIC")
        .queryParam(RESOURCE_KEY_PARAM, "   ")
        .when()
        .get(RESOURCE_ACL_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }
  @Test
  void getResourceAcl_invalidResourceType_returns400() {
    givenAuthenticated()
        .queryParam(RESOURCE_TYPE_PARAM, "NOT_A_REAL_TYPE")
        .queryParam(RESOURCE_KEY_PARAM, "/it/nonexistent/" + UUID.randomUUID())
        .when()
        .get(RESOURCE_ACL_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void getIdentityAcl_invalidUuid_returns400() {
    givenAuthenticatedNoValidation()
        .when()
        .get(IDENTITY_ACL_PATH + "/not-a-uuid/acl")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void getIdentityAcl_existingUser_returns200() {
    UUID userUuid = findAnyExistingUserUuid();
    Assertions.assertNotNull(userUuid, "No users returned by /auth/users, cannot test identity ACL");

    givenAuthenticated()
        .when()
        .get(IDENTITY_ACL_PATH + "/" + userUuid + "/acl")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);
  }

  @Test
  void getGroupAcl_existingGroup_returns200() {
    UUID groupUuid = findAnyExistingGroupUuid();
    Assertions.assertNotNull(groupUuid, "No groups returned by /auth/groups, cannot test group ACL");

    givenAuthenticatedNoValidation()
        .when()
        .get(GROUP_ACL_PATH + "/" + groupUuid )
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);
  }

  @Test
  void updateResourceAcl_missingBody_returns400_withoutValidator() {
    givenAuthenticatedNoValidation()
        .contentType(ContentType.JSON)
        .when()
        .put(RESOURCE_ACL_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  void checkAccess_missingBody_returns400_withoutValidator() {
    givenAuthenticatedNoValidation()
        .contentType(ContentType.JSON)
        .when()
        .post(ACL_CHECK_PATH)
        .then()
        .statusCode(400)
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

  private UUID findAnyExistingGroupUuid() {
    Response response = givenAuthenticated()
        .when()
        .get(GROUPS_PATH)
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

      String uuidText = getFirstString(map, "uniqueId", "groupUuid", "id");
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
