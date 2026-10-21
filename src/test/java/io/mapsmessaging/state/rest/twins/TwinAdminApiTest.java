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

package io.mapsmessaging.state.rest.twins;

import io.mapsmessaging.rest.ApiTestBase;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.mavlink.model.impl.uav.GenericPx4UavModel;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TwinAdminApiTest extends ApiTestBase {

  private static final String ADMIN_BASE_PATH = URI_PATH + "/server/twin/admin";
  private static final String CONFIG_BASE_PATH = URI_PATH + "/server/twin/config";

  @Test
  void listDrones_returnsConfiguredDroneEnvelope() {
    String name = "it_admin_drone_" + UUID.randomUUID();
    UUID uuid = UUID.randomUUID();
    String body = """
        {
          "name": "%s",
          "uuid": "%s",
          "modelName": "%s"
        }
        """.formatted(name, uuid, GenericPx4UavModel.MODEL_NAME);

    try {
      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body)
          .when()
          .post(CONFIG_BASE_PATH + "/drone-info")
          .then()
          .statusCode(201);

      givenAuthenticated()
          .when()
          .get(ADMIN_BASE_PATH + "/drones")
          .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body("$", notNullValue())
          .body("find { it.drone.name == '" + name + "' }.drone.uuid", equalTo(uuid.toString()));
    } finally {
      givenAuthenticatedNoValidation()
          .when()
          .delete(CONFIG_BASE_PATH + "/drone-info/" + name);
    }
  }

  @Test
  void toAdminDroneConfigurations_preservesGeospatialAssignment() {
    DroneInfoDTO drone = new DroneInfoDTO();
    drone.setName("USV-001");
    drone.setUuid(UUID.randomUUID());
    drone.setGeospatialArea("sesimbra-usv");

    List<DroneAdminConfigurationDTO> response =
        TwinAdminApi.toAdminDroneConfigurations(List.of(drone));

    assertEquals(1, response.size());
    assertSame(drone, response.get(0).getDrone());
    assertEquals("sesimbra-usv", response.get(0).getDrone().getGeospatialArea());
  }
}
