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

package io.mapsmessaging.rest.api.impl.integration;

import io.mapsmessaging.rest.ApiTestBase;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class IntegrationManagementApiTest extends ApiTestBase {

  private static final String BASE_PATH = "/api/v1/server/integrations";

  @Test
  void getAllIntegrations_returns200_andHasProtocolsArray() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Object protocols = response.jsonPath().get("protocols");
    Assertions.assertNotNull(protocols);
  }

  @Test
  void getAllIntegrations_withValidFilter_returns200() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .queryParam("filter", "true")
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Object protocols = response.jsonPath().get("protocols");
    Assertions.assertNotNull(protocols);
  }

  @Test
  void getAllIntegrations_withInvalidFilter_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .queryParam("filter", "this is not valid !!!")
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
  void patchAllIntegrations_started_returns200() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"started\"}")
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
  void patchAllIntegrations_blankState_returns400() {
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
  void patchAllIntegrations_unknownState_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"banana\"}")
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
  void getAllIntegrationStatus_returns200_andIsArray() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/status")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Object raw = response.jsonPath().get("$");
    Assertions.assertNotNull(raw);
    Assertions.assertTrue(raw instanceof List<?>);
  }

  @Test
  void getAllIntegrationStatus_withValidFilter_returns200_andIsArray() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .queryParam("filter", "true")
        .when()
        .get(BASE_PATH + "/status")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Object raw = response.jsonPath().get("$");
    Assertions.assertNotNull(raw);
    Assertions.assertTrue(raw instanceof List<?>);
  }

  @Test
  void getAllIntegrationStatus_withInvalidFilter_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .queryParam("filter", "this is not valid !!!")
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
  void getAllIntegrations_anonymous_returns401_withoutOpenApiValidation() {
    Response response = givenAnonymousNoValidation()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(401)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  private RequestSpecification givenAnonymousNoValidation() {
    return RestAssured
        .given()
        .baseUri(baseUrl);
  }

  private boolean hasNonBlankStatusMessage(Response response) {
    String status = response.jsonPath().getString("status");
    return status != null && !status.isBlank();
  }
}
