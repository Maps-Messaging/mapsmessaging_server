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

package io.mapsmessaging.rest.api.impl.schema;

import io.mapsmessaging.rest.ApiTestBase;
import io.mapsmessaging.rest.responses.SchemaMapResponse;
import io.mapsmessaging.rest.responses.SchemaPostDTO;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.rest.responses.StringListResponse;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;
import static org.hamcrest.Matchers.notNullValue;

public class SchemaQueryApiTest extends ApiTestBase {

  private static final String BASE_PATH = URI_PATH + "/server/schemas";

  @Test
  void getKnownFormats_returns200_andHasBody() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/formats")
        .then()
        .statusCode(200)
        .body(notNullValue())
        .extract()
        .response();

    StringListResponse stringListResponse = response.as(StringListResponse.class);
    Assertions.assertNotNull(stringListResponse);
  }

  @Test
  void getLinkFormat_returns200_textPlain() {
    String body = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/link-format")
        .then()
        .statusCode(200)
        .extract()
        .asString();

    Assertions.assertNotNull(body);
  }

  @Test
  void getAllSchemas_returns200_andHasArrayBody() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .body(notNullValue())
        .extract()
        .response();

    SchemaConfig[] schemaConfigs = response.as(SchemaConfig[].class);
    Assertions.assertNotNull(schemaConfigs);
  }

  @Test
  void getSchemaMapping_returns200_andHasBody() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/map")
        .then()
        .statusCode(200)
        .body(notNullValue())
        .extract()
        .response();

    SchemaMapResponse schemaMapResponse = response.as(SchemaMapResponse.class);
    Assertions.assertNotNull(schemaMapResponse);
  }

  @Test
  void getSchemaById_notFound_returns404_withStatusResponse() {
    String schemaId = "it_missing_" + System.currentTimeMillis();

    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/" + schemaId)
        .then()
        .statusCode(404)
        .extract()
        .response();

    StatusResponse statusResponse = response.as(StatusResponse.class);
    Assertions.assertNotNull(statusResponse);
    Assertions.assertNotNull(statusResponse.getStatus());
  }

  @Test
  void deleteSchemaById_notFound_returns404_withStatusResponse() {
    String schemaId = "it_missing_" + System.currentTimeMillis();

    Response response = givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + schemaId)
        .then()
        .statusCode(404)
        .extract()
        .response();

    StatusResponse statusResponse = response.as(StatusResponse.class);
    Assertions.assertNotNull(statusResponse);
    Assertions.assertNotNull(statusResponse.getStatus());
  }

  @Test
  void getSchemaImplById_notFound_returns404_noBody() {
    String schemaId = "it_missing_" + System.currentTimeMillis();

    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/impl/" + schemaId)
        .then()
        .statusCode(404);
  }

  @Test
  void getAllSchemas_invalidFilter_returns400_withStatusResponse() {
    Response response = givenAuthenticated()
        .queryParam("filter", "this is not a selector expression {")
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(400)
        .extract()
        .response();

    StatusResponse statusResponse = response.as(StatusResponse.class);
    Assertions.assertNotNull(statusResponse);
    Assertions.assertNotNull(statusResponse.getStatus());
  }

  @Test
  void deleteAllSchemas_invalidFilter_returns400_withStatusResponse() {
    Response response = givenAuthenticated()
        .queryParam("filter", "this is not a selector expression {")
        .when()
        .delete(BASE_PATH)
        .then()
        .statusCode(400)
        .extract()
        .response();

    StatusResponse statusResponse = response.as(StatusResponse.class);
    Assertions.assertNotNull(statusResponse);
    Assertions.assertNotNull(statusResponse.getStatus());
  }

  @Test
  void addGetDeleteSchema_happyPath_usingTemplateSchemaPack() {
    String context = "it_ctx_" + System.currentTimeMillis();

    SchemaConfig templateSchemaConfig = getAnyExistingSchemaConfig();
    if (templateSchemaConfig == null) {
      Assertions.fail("No existing schema available to use as a template for integration tests");
      return;
    }

    String templateSchemaId = templateSchemaConfig.getUniqueId();
    Assertions.assertNotNull(templateSchemaId);

    String packedTemplate = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/" + templateSchemaId)
        .then()
        .statusCode(200)
        .extract()
        .asString();

    String newSchemaId = UUID.randomUUID().toString();
    String packedMutated = mutateSchemaIdInPackedSchema(packedTemplate, templateSchemaId, newSchemaId);

    SchemaPostDTO schemaPostDTO = new SchemaPostDTO();
    schemaPostDTO.setContext(context);
    schemaPostDTO.setSchema(packedMutated);

    String createResponse = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(schemaPostDTO)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(200)
        .extract()
        .asString();

    Assertions.assertNotNull(createResponse);

    String getResponse = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/" + newSchemaId)
        .then()
        .statusCode(200)
        .extract()
        .asString();

    Assertions.assertNotNull(getResponse);

    assertSafeToDeleteSchemaId(newSchemaId);

    String deleteResponse = givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + newSchemaId)
        .then()
        .statusCode(200)
        .extract()
        .asString();

    Assertions.assertNotNull(deleteResponse);
  }

  @Test
  void addSchema_blankContext_returns400_withStatusResponse() {
    SchemaPostDTO schemaPostDTO = new SchemaPostDTO();
    schemaPostDTO.setContext("   ");
    schemaPostDTO.setSchema("anything");

    Response response = givenAuthenticated()
        .contentType(ContentType.JSON)
        .body(schemaPostDTO)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400)
        .extract()
        .response();

    StatusResponse statusResponse = response.as(StatusResponse.class);
    Assertions.assertNotNull(statusResponse);
    Assertions.assertNotNull(statusResponse.getStatus());
  }

  @Test
  void getSchemaByContext_blankContext_returns400_withStatusResponse() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/context/%20%20%20")
        .then()
        .statusCode(400)
        .extract()
        .response();

    StatusResponse statusResponse = response.as(StatusResponse.class);
    Assertions.assertNotNull(statusResponse);
    Assertions.assertNotNull(statusResponse.getStatus());
  }

  @Test
  void getSchemaByType_blankType_returns400_withStatusResponse() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH + "/type/%20%20%20")
        .then()
        .statusCode(400)
        .extract()
        .response();

    StatusResponse statusResponse = response.as(StatusResponse.class);
    Assertions.assertNotNull(statusResponse);
    Assertions.assertNotNull(statusResponse.getStatus());
  }

  private SchemaConfig getAnyExistingSchemaConfig() {
    Response response = givenAuthenticated()
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .extract()
        .response();

    SchemaConfig[] schemaConfigs = response.as(SchemaConfig[].class);
    if (schemaConfigs == null || schemaConfigs.length == 0) {
      return null;
    }
    for (SchemaConfig schemaConfig : schemaConfigs) {
      if (schemaConfig != null && schemaConfig.getUniqueId() != null && !schemaConfig.getUniqueId().trim().isEmpty()) {
        return schemaConfig;
      }
    }
    return null;
  }

  private String mutateSchemaIdInPackedSchema(String packedSchema, String oldSchemaId, String newSchemaId) {
    if (packedSchema == null) {
      return null;
    }
    if (oldSchemaId == null || oldSchemaId.trim().isEmpty()) {
      return packedSchema;
    }
    if (newSchemaId == null || newSchemaId.trim().isEmpty()) {
      return packedSchema;
    }
    return packedSchema.replace(oldSchemaId, newSchemaId);
  }

  private void assertSafeToDeleteSchemaId(String schemaId) {
    if (schemaId == null) {
      Assertions.fail("Refusing to delete null schemaId");
      return;
    }
  }
}
