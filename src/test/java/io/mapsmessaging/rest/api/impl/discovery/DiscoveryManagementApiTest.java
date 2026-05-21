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

package io.mapsmessaging.rest.api.impl.discovery;

import io.mapsmessaging.rest.ApiTestBase;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DiscoveryManagementApiTest extends ApiTestBase {

  private static final String BASE_PATH = "/api/v1/server/discovery";

  @Test
  void patchStartDiscoveryManager_returns200() {
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
  void patchDiscoveryManager_blankState_returns400() {
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
  void patchDiscoveryManager_unknownState_returns400() {
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
  void getDiscoveredServers_noFilter_returns200() {
    givenAuthenticated()
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);
  }

  @Test
  void getDiscoveredServers_invalidFilter_returns400() {
    Response response = givenAuthenticated()
        .queryParam("filter", "this is not a valid selector !!!")
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
  void getDiscoveredServers_validFilter_returns200() {
    givenAuthenticated()
        .queryParam("filter", "true")
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);
  }

  @Test
  void getDiscoveredServers_anonymous_returns401_withoutOpenApiValidation() {
    Response response = givenAnonymousNoValidation()
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(401)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void patchDiscoveryManager_anonymous_returns401_withoutOpenApiValidation() {
    Response response = givenAnonymousNoValidation()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"started\"}")
        .when()
        .patch(BASE_PATH)
        .then()
        .statusCode(401)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  private RequestSpecification givenAnonymousNoValidation() {
    return io.restassured.RestAssured
        .given()
        .baseUri(baseUrl);
  }

  private boolean hasNonBlankStatusMessage(Response response) {
    JsonPath path = response.jsonPath();
    String status = null;
    if(path != null) {
      status = path.getString("status");
    }
    return status != null && !status.isBlank();

  }
}
