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

package io.mapsmessaging.rest.api.impl.lora;

import io.mapsmessaging.dto.rest.lora.LoRaDeviceInfoDTO;
import io.mapsmessaging.rest.ApiTestBase;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LoRaDeviceApiTest extends ApiTestBase {

  private static final String BASE_PATH = "/api/v1/device/lora";

  @Test
  void getAllLoRaDevices_returns200_andJson() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    LoRaDeviceInfoDTO[] devices = response.as(LoRaDeviceInfoDTO[].class);
    Assertions.assertNotNull(devices);
  }

  @Test
  void getLoRaDevice_unknownDevice_returns404() {
    String unknownDeviceName = "it_unknown_" + System.currentTimeMillis();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + unknownDeviceName)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getLoRaDevice_existingDevice_returns200_whenAvailable() {
    Response listResponse = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    LoRaDeviceInfoDTO[] devices = listResponse.as(LoRaDeviceInfoDTO[].class);
    Assertions.assertNotNull(devices);

    Assumptions.assumeTrue(devices.length > 0, "No LoRa devices configured in this environment");

    String deviceName = devices[0].getName();
    Assertions.assertNotNull(deviceName);
    Assertions.assertFalse(deviceName.trim().isEmpty());

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + deviceName)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    LoRaDeviceInfoDTO device = response.as(LoRaDeviceInfoDTO.class);
    Assertions.assertNotNull(device);
    Assertions.assertNotNull(device.getName());
  }

  @Test
  void getLoRaEndPointConnections_invalidNodeId_returns400() {
    String unknownDeviceName = "it_unknown_" + System.currentTimeMillis();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + unknownDeviceName + "/not-an-int")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getLoRaEndPointConnections_unknownDevice_returns404() {
    String unknownDeviceName = "it_unknown_" + System.currentTimeMillis();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + unknownDeviceName + "/1")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
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
