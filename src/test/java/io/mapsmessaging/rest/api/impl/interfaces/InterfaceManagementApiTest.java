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

package io.mapsmessaging.rest.api.impl.interfaces;

import io.mapsmessaging.rest.ApiTestBase;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InterfaceManagementApiTest extends ApiTestBase {

  private static final String BASE_PATH = "/api/v1/server/interfaces";

  @Test
  void getAllInterfaces_noFilter_returns200() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertNotNull(response.asString());
  }

  @Test
  void getAllInterfaces_blankFilter_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .queryParam("filter", "   ")
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getAllInterfaces_invalidFilter_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .queryParam("filter", "this is not valid selector syntax")
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getAllInterfaces_validFilter_returns200() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .queryParam("filter", "state = 'started'")
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertNotNull(response.asString());
  }

  @Test
  void patchManageAll_nullBody_returns400_withoutOpenApiValidation() {
    Response response = givenAuthenticatedNoValidation()
        .contentType(ContentType.JSON)
        .when()
        .patch(BASE_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void patchManageAll_blankState_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"\"}")
        .when()
        .patch(BASE_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void patchManageAll_unknownAction_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"teleport\"}")
        .when()
        .patch(BASE_PATH)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void patchManageAll_validAction_returns200() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"paused\"}")
        .when()
        .patch(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getAllStatus_noFilter_returns200() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/status")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertNotNull(response.asString());
  }

  @Test
  void getAllStatus_blankFilter_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .queryParam("filter", " ")
        .when()
        .get(BASE_PATH + "/status")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getAllStatus_invalidFilter_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .queryParam("filter", "this is not valid selector syntax")
        .when()
        .get(BASE_PATH + "/status")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getAllStatus_validFilter_returns200() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .queryParam("filter", "state = 'started'")
        .when()
        .get(BASE_PATH + "/status")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertNotNull(response.asString());
  }

  private boolean hasNonBlankStatusMessage(Response response) {
    StatusResponse statusResponse = response.as(StatusResponse.class);
    if (statusResponse == null) {
      return false;
    }
    if (statusResponse.getStatus() == null) {
      return false;
    }
    return !statusResponse.getStatus().trim().isEmpty();
  }
}
