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

package io.mapsmessaging.rest.destination;

import io.mapsmessaging.rest.ApiTestBase;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DestinationListManagementApiTest extends ApiTestBase {

  private static final String BASE_PATH = "/api/v1/server/destination/list";

  @Test
  void getDestinationPage_defaultRequest_returns200_andHasExpectedShape() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .extract()
        .response();

    Integer totalEntries = response.jsonPath().getInt("totalEntries");
    Integer totalPages = response.jsonPath().getInt("totalPages");
    Integer pageNo = response.jsonPath().getInt("pageNo");
    Object entries = response.jsonPath().get("entries");

    Assertions.assertNotNull(totalEntries);
    Assertions.assertTrue(totalEntries >= 0);

    Assertions.assertNotNull(totalPages);
    Assertions.assertTrue(totalPages >= 0);

    Assertions.assertNotNull(pageNo);
    Assertions.assertTrue(pageNo >= 0);

    Assertions.assertNotNull(entries);

    String etag = response.getHeader("ETag");
    Assertions.assertNotNull(etag);
    Assertions.assertFalse(etag.trim().isEmpty());
  }

  @Test
  void getDestinationPage_invalidPrefixEncoding_returns400_withStatusResponse() {
    Response response = givenAuthenticated()
        .queryParam("prefix", "%E0%A4%A")
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(400)
        .extract()
        .response();

    StatusResponse statusResponse = response.as(StatusResponse.class);
    Assertions.assertNotNull(statusResponse);
    Assertions.assertNotNull(statusResponse.getStatus());
    Assertions.assertFalse(statusResponse.getStatus().trim().isEmpty());
  }

  @Test
  void getDestinationPage_notFoundPrefix_returns404_withStatusResponse() {
    String prefix = "it_missing_namespace_" + System.currentTimeMillis();

    Response response = givenAuthenticated()
        .queryParam("prefix", prefix)
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(404)
        .extract()
        .response();

    StatusResponse statusResponse = response.as(StatusResponse.class);
    Assertions.assertNotNull(statusResponse);
    Assertions.assertNotNull(statusResponse.getStatus());
    Assertions.assertFalse(statusResponse.getStatus().trim().isEmpty());
  }

  @Test
  void getDestinationPage_etagMatch_returns304_noBody() {
    Response first = givenAuthenticated()
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .extract()
        .response();

    String etag = first.getHeader("ETag");
    Assertions.assertNotNull(etag);
    Assertions.assertFalse(etag.trim().isEmpty());

    Response second = givenAuthenticated()
        .header("If-None-Match", etag)
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(304)
        .extract()
        .response();

    String body = second.getBody() == null ? "" : second.getBody().asString();
    Assertions.assertTrue(body == null || body.isEmpty());
  }
}
