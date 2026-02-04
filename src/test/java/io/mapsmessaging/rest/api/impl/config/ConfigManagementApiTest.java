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

import io.mapsmessaging.rest.ApiTestBase;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

public class ConfigManagementApiTest extends ApiTestBase {

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

    String firstSectionName = getFirstStringFromArray(response);
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

    String sectionName = getFirstStringFromArray(listResponse);
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

  private String getFirstStringFromArray(Response response) {
    JsonPath jsonPath = response.jsonPath();
    Object raw = jsonPath.get("$");
    if (raw == null) {
      return null;
    }
    if (raw instanceof java.util.List<?> list) {
      for (Object item : list) {
        if (item instanceof String value && !value.isBlank()) {
          return value;
        }
      }
      return null;
    }
    if (raw instanceof String[] values) {
      for (String value : values) {
        if (value != null && !value.isBlank()) {
          return value;
        }
      }
      return null;
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
