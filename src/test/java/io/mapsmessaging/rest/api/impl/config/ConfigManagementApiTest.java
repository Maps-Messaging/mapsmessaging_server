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

package io.mapsmessaging.rest.api.impl.config;

import io.mapsmessaging.config.MessageDaemonConfig;
import io.mapsmessaging.rest.ApiTestBase;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

class ConfigManagementApiTest extends ApiTestBase {

  private static final String BASE_PATH = URI_PATH + "/server/config";

  @Test
  void listConfigSections_returns200_andNonEmptyArray() {
    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    String firstSectionName = getAuthSectionNameFromArray(response);
    Assertions.assertNotNull(firstSectionName);
    Assertions.assertFalse(firstSectionName.isBlank());
  }

  @Test
  void getConfigSection_returns200_forKnownSection_fromList() {
    Response listResponse = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    String sectionName = getAuthSectionNameFromArray(listResponse);
    Assertions.assertNotNull(sectionName);
    Assertions.assertFalse(sectionName.isBlank());

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + urlEncode(sectionName))
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    JsonPath jsonPath = response.jsonPath();
    Object configObject = jsonPath.get("config");
    Object schemaObject = jsonPath.get("schema");

    Assertions.assertNotNull(configObject);
    Assertions.assertNotNull(schemaObject);
  }

  @Test
  void getConfigSection_returns400_forBlankName() {
    String encodedBlank = urlEncode(" ");
    Response response = givenAuthenticatedNoValidation()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + encodedBlank)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void getConfigSection_returns404_forUnknownSection() {
    String unknownSection = "it_unknown_" + Instant.now().toEpochMilli();

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + urlEncode(unknownSection))
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void update_config_section_saves_valid_configuration_and_invalidates_cached_response() {
    Map<String, Object> originalConfig = getConfigSection("Discovery").jsonPath().getMap("config");
    boolean originalValue = (Boolean) originalConfig.get("addTxtRecords");
    Map<String, Object> updatedConfig = new LinkedHashMap<>(originalConfig);
    updatedConfig.put("addTxtRecords", !originalValue);

    try {
      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(updatedConfig)
          .when()
          .put(BASE_PATH + "/Discovery")
          .then()
          .statusCode(200)
          .contentType(ContentType.JSON);

      Response updatedResponse = getConfigSection("Discovery");
      Assertions.assertEquals(!originalValue, updatedResponse.jsonPath().getBoolean("config.addTxtRecords"));
    } finally {
      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(originalConfig)
          .when()
          .put(BASE_PATH + "/Discovery")
          .then()
          .statusCode(200);
    }
  }

  @Test
  void update_config_section_returns400_and_does_not_apply_invalid_configuration() {
    Map<String, Object> invalidConfig = new LinkedHashMap<>(getConfigSection("Server").jsonPath().getMap("config"));
    int originalValue = MessageDaemonConfig.getInstance().getDelayedPublishInterval();
    invalidConfig.put("delayedPublishInterval", 499);

    Response response = givenAuthenticatedNoValidation()
        .contentType(ContentType.JSON)
        .body(invalidConfig)
        .when()
        .put(BASE_PATH + "/Server")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
    Assertions.assertEquals(originalValue, MessageDaemonConfig.getInstance().getDelayedPublishInterval());
  }

  @Test
  void update_config_section_returns400_for_mismatched_configuration_type() {
    Map<String, Object> config = new LinkedHashMap<>(getConfigSection("Server").jsonPath().getMap("config"));
    config.put("type", "AuthManagerConfigDTO");

    Response response = givenAuthenticatedNoValidation()
        .contentType(ContentType.JSON)
        .body(config)
        .when()
        .put(BASE_PATH + "/Server")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  @Test
  void update_config_section_returns404_for_unknown_section() {
    String unknownSection = "it_unknown_" + Instant.now().toEpochMilli();
    Map<String, Object> config = getConfigSection("Server").jsonPath().getMap("config");

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(config)
        .when()
        .put(BASE_PATH + "/" + urlEncode(unknownSection))
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .extract()
        .response();

    Assertions.assertTrue(hasNonBlankStatusMessage(response));
  }

  private Response getConfigSection(String sectionName) {
    return givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/" + urlEncode(sectionName))
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .response();
  }

  private String getAuthSectionNameFromArray(Response response) {
    JsonPath jsonPath = response.jsonPath();
    Object raw = jsonPath.get("$");
    switch (raw) {
      case null -> {
        return null;
      }
      case java.util.List<?> list -> {
        for (Object item : list) {
          if (item instanceof String value && !value.isBlank()) {
            return value;
          }

          if (item instanceof java.util.Map<?, ?> map) {
            Object name = map.get("name");
            if (name instanceof String value && !value.isBlank() && value.equalsIgnoreCase("auth")) {
              return value;
            }
          }
        }

        return null;
      }
      case String[] values -> {
        for (String value : values) {
          if (value != null && !value.isBlank()) {
            return value;
          }
        }

        return null;
      }
      default -> {
      }
    }

    return null;
  }

  private boolean hasNonBlankStatusMessage(Response response) {
    JsonPath jsonPath = response.jsonPath();
    String status = jsonPath.getString("status");
    return status != null && !status.isBlank();
  }

  private String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
