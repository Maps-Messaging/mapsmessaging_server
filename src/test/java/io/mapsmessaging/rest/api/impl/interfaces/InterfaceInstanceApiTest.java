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

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.ApiTestBase;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

class InterfaceInstanceApiTest extends ApiTestBase {

  private static final String BASE_PATH = "/api/v1/server/interface";

  @Test
  @Disabled("Not yet implemented")
  void getEndPoint_existingEndpoint_returns200() {
    UUID endpointId = getExistingEndpointId();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + endpointId)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertNotNull(response.asString());
    Assertions.assertFalse(response.asString().trim().isEmpty());
  }

  @Test
  void getEndPoint_invalidEndpointId_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/not-a-uuid")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getEndPoint_notFound_returns404() {
    UUID endpointId = UUID.randomUUID();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + endpointId)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getConnections_existingEndpoint_returns200() {
    UUID endpointId = getExistingEndpointId();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + endpointId + "/connections")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertNotNull(response.asString());
  }

  @Test
  void getConnections_invalidEndpointId_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/not-a-uuid/connections")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getConnections_notFound_returns404() {
    UUID endpointId = UUID.randomUUID();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + endpointId + "/connections")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getStatus_existingEndpoint_returns200() {
    UUID endpointId = getExistingEndpointId();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + endpointId + "/status")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertNotNull(response.asString());
  }

  @Test
  void getStatus_invalidEndpointId_returns400() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/not-a-uuid/status")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getStatus_notFound_returns404() {
    UUID endpointId = UUID.randomUUID();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + endpointId + "/status")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void patchControl_blankState_returns400() {
    UUID endpointId = getExistingEndpointId();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"\"}")
        .when()
        .patch(BASE_PATH + "/" + endpointId)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void patchControl_unknownState_returns400() {
    UUID endpointId = getExistingEndpointId();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"teleport\"}")
        .when()
        .patch(BASE_PATH + "/" + endpointId)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void patchControl_notFound_returns404() {
    UUID endpointId = UUID.randomUUID();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"paused\"}")
        .when()
        .patch(BASE_PATH + "/" + endpointId)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void patchControl_validState_returns200() {
    UUID endpointId = getExistingEndpointId();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("{\"state\":\"started\"}")
        .when()
        .patch(BASE_PATH + "/" + endpointId)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void putUpdate_nullBody_returns400_withoutOpenApiValidation() {
    UUID endpointId = getExistingEndpointId();

    Response response = givenAuthenticatedNoValidation()
        .contentType(ContentType.JSON)
        .when()
        .put(BASE_PATH + "/" + endpointId)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  private UUID getExistingEndpointId() {
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance()
        .getSubSystemManager()
        .getNetworkManager()
        .getAll();

    Assertions.assertNotNull(endPointManagers);
    Assertions.assertFalse(endPointManagers.isEmpty());

    UUID endpointId = endPointManagers.get(0).getUniqueId();
    Assertions.assertNotNull(endpointId);
    return endpointId;
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
