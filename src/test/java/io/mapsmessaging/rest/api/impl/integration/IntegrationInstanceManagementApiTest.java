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
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class IntegrationInstanceManagementApiTest extends ApiTestBase {

  private static final String LIST_BASE_PATH = "/api/v1/server/integrations";
  private static final String INSTANCE_BASE_PATH = "/api/v1/server/integration";

  @Test
  void getIntegrationByName_returns200_forNameFromList() {
    String integrationName = resolveFirstIntegrationNameOrSkip();
    if (integrationName == null) {
      return;
    }

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(INSTANCE_BASE_PATH + "/" + urlEncodePathSegment(integrationName))
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertNotNull(response.jsonPath().get("$"));
  }

  @Test
  void getIntegrationByName_returns404_forUnknownName() {
    String unknownName = "it_unknown_integration_404";

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(INSTANCE_BASE_PATH + "/" + urlEncodePathSegment(unknownName))
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getIntegrationConnection_returns200_forNameFromList() {
    String integrationName = resolveFirstIntegrationNameOrSkip();
    if (integrationName == null) {
      return;
    }

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(INSTANCE_BASE_PATH + "/" + urlEncodePathSegment(integrationName) + "/connection")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertNotNull(response.jsonPath().get("$"));
  }

  @Test
  void getIntegrationStatus_returns200_forNameFromList() {
    String integrationName = resolveFirstIntegrationNameOrSkip();
    if (integrationName == null) {
      return;
    }

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(INSTANCE_BASE_PATH + "/" + urlEncodePathSegment(integrationName) + "/status")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertNotNull(response.jsonPath().get("$"));
  }

  @Test
  void patchIntegrationAction_started_returns200_forNameFromList() {
    String integrationName = resolveFirstIntegrationNameOrSkip();
    if (integrationName == null) {
      return;
    }

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"started\"}")
        .when()
        .patch(INSTANCE_BASE_PATH + "/" + urlEncodePathSegment(integrationName))
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void patchIntegrationAction_blankState_returns400_forNameFromList() {
    String integrationName = resolveFirstIntegrationNameOrSkip();
    if (integrationName == null) {
      return;
    }

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"\"}")
        .when()
        .patch(INSTANCE_BASE_PATH + "/" + urlEncodePathSegment(integrationName))
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void patchIntegrationAction_unknownState_returns400_forNameFromList() {
    String integrationName = resolveFirstIntegrationNameOrSkip();
    if (integrationName == null) {
      return;
    }

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"banana\"}")
        .when()
        .patch(INSTANCE_BASE_PATH + "/" + urlEncodePathSegment(integrationName))
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getIntegrationByName_anonymous_returns401_withoutOpenApiValidation() {
    String integrationName = resolveFirstIntegrationNameOrSkip();
    if (integrationName == null) {
      return;
    }

    Response response = givenAnonymousNoValidation()
        .contentType(ContentType.JSON)
        .when()
        .get(INSTANCE_BASE_PATH + "/" + urlEncodePathSegment(integrationName))
        .then()
        .statusCode(401)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  private String resolveFirstIntegrationNameOrSkip() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(LIST_BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    JsonPath jsonPath = response.jsonPath();

    Object listObject = jsonPath.get("protocols");
    if (!(listObject instanceof List<?> list)) {
      return null;
    }

    for (Object entry : list) {
      if (!(entry instanceof java.util.Map<?, ?> map)) {
        continue;
      }

      Object nameValue = map.get("name");
      if (nameValue instanceof String name && !name.isBlank()) {
        return name;
      }

      Object configNameValue = map.get("configName");
      if (configNameValue instanceof String configName && !configName.isBlank()) {
        return configName;
      }
    }

    return null;
  }

  private RequestSpecification givenAnonymousNoValidation() {
    return RestAssured
        .given()
        .baseUri(baseUrl);
  }

  private String urlEncodePathSegment(String value) {
    return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
  }

  private boolean hasNonBlankStatusMessage(Response response) {
    String status = response.jsonPath().getString("status");
    return status != null && !status.isBlank();
  }
}
