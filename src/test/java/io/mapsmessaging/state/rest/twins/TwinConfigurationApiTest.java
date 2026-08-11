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
import io.mapsmessaging.state.mavlink.model.impl.uav.GenericPx4UavModel;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;
import static org.hamcrest.Matchers.*;

class TwinConfigurationApiTest extends ApiTestBase {

  private static final String BASE_PATH = URI_PATH + "/server/twin/config";
  private static final Path CONFIG_DIRECTORY = Path.of("conf");
  private static final Path TWIN_CONFIG_FILE = CONFIG_DIRECTORY.resolve("TwinManager.yaml");
  private static boolean createdConfigDirectory;

  @BeforeAll
  static void prepareWritableConfigurationDirectory() throws IOException {
    createdConfigDirectory = Files.notExists(CONFIG_DIRECTORY);
    Files.createDirectories(CONFIG_DIRECTORY);
  }

  @AfterAll
  static void removeGeneratedConfiguration() throws IOException {
    if (!createdConfigDirectory) {
      return;
    }
    Files.deleteIfExists(TWIN_CONFIG_FILE);
    try (var entries = Files.list(CONFIG_DIRECTORY)) {
      if (entries.findAny().isEmpty()) {
        Files.delete(CONFIG_DIRECTORY);
      }
    }
  }

  @Test
  void getConfigurationAndCore_returns200() {
    givenAuthenticatedNoValidation()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("heartbeatTimeoutMillis", greaterThan(0))
        .body("staleTimeoutMillis", greaterThan(0))
        .body("defaultRootPath", not(isEmptyOrNullString()));

    givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + "/core")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("heartbeatTimeoutMillis", greaterThan(0))
        .body("staleTimeoutMillis", greaterThan(0))
        .body("defaultRootPath", not(isEmptyOrNullString()));
  }

  @Test
  void catalogues_returnAvailableModelsAndConfiguredAreaNames() {
    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/drone-models")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("$", hasItem(GenericPx4UavModel.MODEL_NAME));

    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/geospatial-area-names")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("$", notNullValue());
  }

  @Test
  void singletonSections_putGetDelete_behaveAsCrud() {
    Response originalTak = getSectionNoValidation("/tak");
    Response originalPublish = getSectionNoValidation("/publish");
    Response originalN2k = getSectionNoValidation("/n2k");

    try {
      putAndReadTak();
      putAndReadPublish();
      putAndReadN2k();
    } finally {
      restoreOptionalSection("/tak", originalTak);
      restoreOptionalSection("/publish", originalPublish);
      restoreOptionalSection("/n2k", originalN2k);
    }
  }

  @Test
  void mavlink_lifecycle_exposesHttpCrudAndValidation() {
    String name = uniqueName("mavlink");
    String body = """
        {
          "name": "%s",
          "topic": "/it/mavlink/%s/#",
          "dialectName": "common"
        }
        """.formatted(name, name);

    try {
      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body)
          .when()
          .post(BASE_PATH + "/mavlink")
          .then()
          .statusCode(201)
          .contentType(ContentType.JSON)
          .body("name", equalTo(name))
          .body("topic", equalTo("/it/mavlink/" + name + "/#"));

      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body)
          .when()
          .post(BASE_PATH + "/mavlink")
          .then()
          .statusCode(409)
          .contentType(ContentType.JSON)
          .body("status", not(isEmptyOrNullString()));

      givenAuthenticated()
          .when()
          .get(BASE_PATH + "/mavlink/" + name)
          .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body("name", equalTo(name));

      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body.replace("/#", "/updated/#"))
          .when()
          .put(BASE_PATH + "/mavlink/" + name)
          .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body("topic", equalTo("/it/mavlink/" + name + "/updated/#"));

      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body.replace(name, name + "_other"))
          .when()
          .put(BASE_PATH + "/mavlink/" + name)
          .then()
          .statusCode(400)
          .contentType(ContentType.JSON)
          .body("status", not(isEmptyOrNullString()));
    } finally {
      deleteIfPresent("/mavlink/" + name);
    }

    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/mavlink/" + name)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));
  }

  @Test
  void droneInfo_lifecycle_exposesHttpCrudAndValidation() {
    String name = uniqueName("drone");
    UUID uuid = UUID.randomUUID();
    String body = """
        {
          "name": "%s",
          "uuid": "%s",
          "modelName": "%s",
          "description": {
            "model": "integration-test"
          }
        }
        """.formatted(name, uuid, GenericPx4UavModel.MODEL_NAME);

    try {
      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body)
          .when()
          .post(BASE_PATH + "/drone-info")
          .then()
          .statusCode(201)
          .contentType(ContentType.JSON)
          .body("name", equalTo(name));

      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body)
          .when()
          .post(BASE_PATH + "/drone-info")
          .then()
          .statusCode(409)
          .contentType(ContentType.JSON)
          .body("status", not(isEmptyOrNullString()));

      givenAuthenticated()
          .when()
          .get(BASE_PATH + "/drone-info/" + name)
          .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body("name", equalTo(name));

      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body.replace("integration-test", "updated"))
          .when()
          .put(BASE_PATH + "/drone-info/" + name)
          .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body("description.model", equalTo("updated"));

      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body.replace(name, name + "_other"))
          .when()
          .put(BASE_PATH + "/drone-info/" + name)
          .then()
          .statusCode(400)
          .contentType(ContentType.JSON)
          .body("status", not(isEmptyOrNullString()));

      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body.replace(uuid.toString(), UUID.randomUUID().toString()))
          .when()
          .put(BASE_PATH + "/drone-info/" + name)
          .then()
          .statusCode(409)
          .contentType(ContentType.JSON)
          .body("status", not(isEmptyOrNullString()));
    } finally {
      deleteIfPresent("/drone-info/" + name);
    }

    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/drone-info/" + name)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));
  }

  @Test
  void authorityBindings_rejectInvalidRequests() {
    givenAuthenticated()
        .contentType(ContentType.JSON)
        .body("""
            {
              "addDrones": ["missing"],
              "removeDrones": []
            }
            """)
        .when()
        .put(BASE_PATH + "/authorities/not-a-uuid")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));

    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/authorities/not-a-uuid")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));
  }

  @Test
  void adapter_lifecycle_exposesHttpCrudAndValidation() {
    String name = uniqueName("adapter");
    String body = """
        {
          "enabled": true,
          "topic": "/it/adapter"
        }
        """;

    try {
      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body)
          .when()
          .post(BASE_PATH + "/adapters/" + name)
          .then()
          .statusCode(201)
          .contentType(ContentType.JSON)
          .body("enabled", equalTo(true))
          .body("topic", equalTo("/it/adapter"));

      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body)
          .when()
          .post(BASE_PATH + "/adapters/" + name)
          .then()
          .statusCode(409)
          .contentType(ContentType.JSON)
          .body("status", not(isEmptyOrNullString()));

      givenAuthenticated()
          .when()
          .get(BASE_PATH + "/adapters/" + name)
          .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body("topic", equalTo("/it/adapter"));

      givenAuthenticated()
          .contentType(ContentType.JSON)
          .body(body.replace("/it/adapter", "/it/adapter/updated"))
          .when()
          .put(BASE_PATH + "/adapters/" + name)
          .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body("topic", equalTo("/it/adapter/updated"));
    } finally {
      deleteIfPresent("/adapters/" + name);
    }

    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/adapters/" + name)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));
  }

  private void putAndReadTak() {
    String body = """
        {
          "hostname": "127.0.0.1",
          "port": 8088,
          "sharedConnection": true,
          "topic": "tak/events"
        }
        """;

    putSection("/tak", body)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("hostname", equalTo("127.0.0.1"))
        .body("port", equalTo(8088))
        .body("sharedConnection", equalTo(true));

    getSection("/tak")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("hostname", equalTo("127.0.0.1"));

    deleteSection("/tak");
  }

  private void putAndReadPublish() {
    String body = """
        {
          "enabled": true,
          "topicTemplate": "/it/twins/{twinId}"
        }
        """;

    putSection("/publish", body)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("enabled", equalTo(true))
        .body("topicTemplate", equalTo("/it/twins/{twinId}"));

    getSection("/publish")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("topicTemplate", equalTo("/it/twins/{twinId}"));

    deleteSection("/publish");
  }

  private void putAndReadN2k() {
    String body = """
        {
          "enable": true,
          "publishMavlinkDrones": false,
          "topic": "/it/n2k/json/#",
          "name": "it-n2k",
          "vehicleClass": "USV"
        }
        """;

    putSection("/n2k", body)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("enable", equalTo(true))
        .body("topic", equalTo("/it/n2k/json/#"))
        .body("name", equalTo("it-n2k"));

    getSection("/n2k")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("topic", equalTo("/it/n2k/json/#"));

    deleteSection("/n2k");
  }

  private Response getSection(String path) {
    return givenAuthenticated()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + path)
        .then()
        .extract()
        .response();
  }

  private Response getSectionNoValidation(String path) {
    return givenAuthenticatedNoValidation()
        .contentType(ContentType.JSON)
        .when()
        .get(BASE_PATH + path)
        .then()
        .extract()
        .response();
  }

  private Response putSection(String path, String body) {
    return givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .put(BASE_PATH + path)
        .then()
        .extract()
        .response();
  }

  private void deleteSection(String path) {
    givenAuthenticated()
        .when()
        .delete(BASE_PATH + path)
        .then()
        .statusCode(204);
  }

  private void deleteIfPresent(String path) {
    givenAuthenticatedNoValidation()
        .when()
        .delete(BASE_PATH + path)
        .then()
        .statusCode(anyOf(is(204), is(404)));
  }

  private void restoreOptionalSection(String path, Response originalResponse) {
    if (originalResponse.statusCode() == 200) {
      putSection(path, originalResponse.asString());
      return;
    }
    if (originalResponse.statusCode() == 404) {
      deleteIfPresent(path);
    }
  }

  private String uniqueName(String type) {
    return "it_" + type + "_" + Instant.now().toEpochMilli();
  }
}
