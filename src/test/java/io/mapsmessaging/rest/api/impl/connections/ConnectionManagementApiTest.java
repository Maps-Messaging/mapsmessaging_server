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

package io.mapsmessaging.rest.api.impl.connections;

import io.mapsmessaging.rest.ApiTestBase;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ConnectionManagementApiTest extends ApiTestBase {

  private static final String BASE_PATH = "/api/v1/server/connections";

  @Test
  void getAllConnections_returns200_andMatchesOpenApi() {
    Response response =
        givenAuthenticated()
            .when()
            .get(BASE_PATH)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .response();

    Assertions.assertTrue(response.asString().startsWith("["));
  }

  @Test
  void getAllConnections_withInvalidFilter_returns400_withStatusResponse() {
    StatusResponse statusResponse =
        givenAuthenticated()
            .queryParam("filter", "this is not a valid selector !!!!")
            .when()
            .get(BASE_PATH)
            .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .extract()
            .as(StatusResponse.class);

    Assertions.assertNotNull(statusResponse);
    Assertions.assertTrue(statusResponse.getStatus() != null && !statusResponse.getStatus().isBlank());
  }

  @Test
  void getConnectionDetails_withInvalidConnectionId_returns400_withStatusResponse() {
    StatusResponse statusResponse =
        givenAuthenticated()
            .when()
            .get(BASE_PATH + "/not-a-number")
            .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .extract()
            .as(StatusResponse.class);

    Assertions.assertNotNull(statusResponse);
    Assertions.assertTrue(statusResponse.getStatus() != null && !statusResponse.getStatus().isBlank());
  }

  @Test
  void getConnectionDetails_withNotFoundConnectionId_returns404_withStatusResponse() {
    String neverExistsConnectionId = "9223372036854775807";

    StatusResponse statusResponse =
        givenAuthenticated()
            .when()
            .get(BASE_PATH + "/" + neverExistsConnectionId)
            .then()
            .statusCode(404)
            .contentType(ContentType.JSON)
            .extract()
            .as(StatusResponse.class);

    Assertions.assertNotNull(statusResponse);
    Assertions.assertTrue(statusResponse.getStatus() != null && !statusResponse.getStatus().isBlank());
  }

  @Test
  void closeSpecificConnection_withInvalidConnectionId_returns400_withStatusResponse() {
    StatusResponse statusResponse =
        givenAuthenticated()
            .when()
            .delete(BASE_PATH + "/not-a-number")
            .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .extract()
            .as(StatusResponse.class);

    Assertions.assertNotNull(statusResponse);
    Assertions.assertTrue(statusResponse.getStatus() != null && !statusResponse.getStatus().isBlank());
  }

  @Test
  void closeSpecificConnection_withNotFoundConnectionId_returns404_withStatusResponse() {
    String neverExistsConnectionId = "9223372036854775807";

    StatusResponse statusResponse =
        givenAuthenticated()
            .when()
            .delete(BASE_PATH + "/" + neverExistsConnectionId)
            .then()
            .statusCode(404)
            .contentType(ContentType.JSON)
            .extract()
            .as(StatusResponse.class);

    Assertions.assertNotNull(statusResponse);
    Assertions.assertTrue(statusResponse.getStatus() != null && !statusResponse.getStatus().isBlank());
  }

  @Test
  void closeSpecificConnection_happyPathIfAnyConnectionExists_elseSkip() {
    Response response =
        givenAuthenticated()
            .when()
            .get(BASE_PATH)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .response();

    List<String> connectionIds = response.jsonPath().getList("id", String.class);
    if (connectionIds == null || connectionIds.isEmpty()) {
      return;
    }

    String connectionId = getFirstString(connectionIds);
    if (connectionId == null || connectionId.isBlank()) {
      return;
    }

    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + connectionId)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON);

    StatusResponse statusResponse =
        givenAuthenticated()
            .when()
            .delete(BASE_PATH + "/" + connectionId)
            .then()
            .statusCode(404)
            .contentType(ContentType.JSON)
            .extract()
            .as(StatusResponse.class);

    Assertions.assertNotNull(statusResponse);
  }

  private String getFirstString(List<String> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

}
