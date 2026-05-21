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

import io.mapsmessaging.dto.rest.ServerInfoDTO;
import io.mapsmessaging.dto.rest.ServerStatisticsDTO;
import io.mapsmessaging.rest.ApiTestBase;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;
import static org.hamcrest.Matchers.notNullValue;

public class ServerDetailsApiTest extends ApiTestBase {

  private static final String BASE_PATH = URI_PATH + "/server/details";

  @Test
  void getBuildInfo_returns200_andHasBody() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/info")
        .then()
        .statusCode(200)
        .body(notNullValue())
        .extract()
        .response();

    ServerInfoDTO serverInfoDTO = response.as(ServerInfoDTO.class);
    Assertions.assertNotNull(serverInfoDTO);
  }

  @Test
  void getBuildInfo_secondCall_returns200_andHasBody() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/info")
        .then()
        .statusCode(200)
        .body(notNullValue())
        .extract()
        .response();

    ServerInfoDTO serverInfoDTO = response.as(ServerInfoDTO.class);
    Assertions.assertNotNull(serverInfoDTO);
  }

  @Test
  void getStats_returns200_andHasBody() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/stats")
        .then()
        .statusCode(200)
        .body(notNullValue())
        .extract()
        .response();

    ServerStatisticsDTO serverStatisticsDTO = response.as(ServerStatisticsDTO.class);
    Assertions.assertNotNull(serverStatisticsDTO);
  }

  @Test
  void getStats_secondCall_returns200_andHasBody() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/stats")
        .then()
        .statusCode(200)
        .body(notNullValue())
        .extract()
        .response();

    ServerStatisticsDTO serverStatisticsDTO = response.as(ServerStatisticsDTO.class);
    Assertions.assertNotNull(serverStatisticsDTO);
  }
}
