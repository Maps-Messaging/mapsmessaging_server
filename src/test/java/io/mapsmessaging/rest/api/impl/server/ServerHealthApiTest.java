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

package io.mapsmessaging.rest.api.impl.server;

import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.rest.ApiTestBase;
import io.mapsmessaging.rest.responses.ServerHealthStateResponse;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;
import static org.hamcrest.Matchers.notNullValue;

public class ServerHealthApiTest extends ApiTestBase {

  private static final String BASE_PATH = URI_PATH + "/server";

  @Test
  void getServerStatus_returns200_andHasArrayBody() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/status")
        .then()
        .statusCode(200)
        .body(notNullValue())
        .extract()
        .response();

    SubSystemStatusDTO[] statusArray = response.as(SubSystemStatusDTO[].class);
    Assertions.assertNotNull(statusArray);
  }

  @Test
  void getServerHealthSummary_returns200_andHasBody() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/health")
        .then()
        .statusCode(200)
        .body(notNullValue())
        .extract()
        .response();

    ServerHealthStateResponse serverHealthStateResponse = response.as(ServerHealthStateResponse.class);
    Assertions.assertNotNull(serverHealthStateResponse);
  }

  @Test
  void serverAction_blankState_returns400_withStatusResponse() {
    ServerActionRequest serverActionRequest = new ServerActionRequest("");
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(serverActionRequest)
        .when()
        .patch(BASE_PATH)
        .then()
        .statusCode(400)
        .extract()
        .response();

    StatusResponse statusResponse = response.as(StatusResponse.class);
    Assertions.assertNotNull(statusResponse);
    Assertions.assertNotNull(statusResponse.getStatus());
  }

  @Test
  void serverAction_unknownState_returns400_withStatusResponse() {
    String requestedState = "it_unknown_" + System.currentTimeMillis();
    ServerActionRequest serverActionRequest = new ServerActionRequest(requestedState);

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(serverActionRequest)
        .when()
        .patch(BASE_PATH)
        .then()
        .statusCode(400)
        .extract()
        .response();

    StatusResponse statusResponse = response.as(StatusResponse.class);
    Assertions.assertNotNull(statusResponse);
    Assertions.assertNotNull(statusResponse.getStatus());
  }
}
