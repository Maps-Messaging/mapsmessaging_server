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

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.rest.ApiTestBase;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import static io.mapsmessaging.rest.api.Constants.URI_PATH;

public class ConnectionManagementApiTest extends ApiTestBase {

  private static final String BASE_PATH = URI_PATH+"/server/connections";

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
  void closeSpecificConnection_closesOwnedNetworkConnection() throws Exception {
    EndPointManager manager = MessageDaemon.getInstance()
        .getSubSystemManager()
        .getNetworkManager()
        .getAll()
        .stream()
        .filter(candidate -> candidate.getState() == EndPointManager.STATE.START)
        .filter(candidate -> candidate.getEndPointServer().getConfig().getUrl().startsWith("tcp://"))
        .filter(candidate -> candidate.getProtocols().toLowerCase(Locale.ROOT).contains("mqtt"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No active TCP MQTT listener available"));

    EndPointURL listener = new EndPointURL(manager.getEndPointServer().getConfig().getUrl());
    try (Socket socket = new Socket("127.0.0.1", listener.getPort())) {
      EndPoint connection = awaitConnection(manager, socket.getLocalPort());
      String connectionId = Long.toString(connection.getId());

      StatusResponse success = givenAuthenticated()
          .when()
          .delete(BASE_PATH + "/" + connectionId)
          .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .extract()
          .as(StatusResponse.class);

      Assertions.assertNotNull(success);
      Assertions.assertEquals("success", success.getStatus());
      Assertions.assertTrue(manager.getEndPointServer().getActiveEndPoints().stream()
          .noneMatch(endpoint -> endpoint.getId() == connection.getId()));

      StatusResponse notFound = givenAuthenticated()
          .when()
          .delete(BASE_PATH + "/" + connectionId)
          .then()
          .statusCode(404)
          .contentType(ContentType.JSON)
          .extract()
          .as(StatusResponse.class);

      Assertions.assertNotNull(notFound);
      Assertions.assertEquals("Connection not found", notFound.getStatus());
    }
  }

  private EndPoint awaitConnection(EndPointManager manager, int localPort) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (System.nanoTime() < deadline) {
      for (EndPoint endpoint : manager.getEndPointServer().getActiveEndPoints()) {
        if (endpoint.getRemoteSocketAddress().endsWith(":" + localPort)) {
          return endpoint;
        }
      }
      Thread.sleep(25);
    }
    throw new AssertionError("Test-owned TCP connection not registered within 5 seconds");
  }

}
