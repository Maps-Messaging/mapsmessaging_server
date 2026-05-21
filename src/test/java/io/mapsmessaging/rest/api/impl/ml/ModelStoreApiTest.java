package io.mapsmessaging.rest.api.impl.ml;

import io.mapsmessaging.rest.ApiTestBase;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ModelStoreApiTest extends ApiTestBase {

  private static final String BASE_PATH = "/api/v1/server/models";

  @Test
  void listModels_whenMlNotSupported_returns406WithJsonBody() {
    Response response =
        givenAuthenticated()
            .when()
            .get(BASE_PATH)
            .then()
            .statusCode(anyOf(is(200), is(406)))
            .extract()
            .response();

    if (response.statusCode() == 406) {
      response.then()
          .contentType(ContentType.JSON)
          .body("status", not(isEmptyOrNullString()));
    }
  }

  @Test
  void uploadModel_blankName_returns400() {
    givenAuthenticated()
        .contentType("multipart/form-data")
        .multiPart("file", "model.bin", "abc".getBytes(StandardCharsets.UTF_8), "application/octet-stream")
        .when()
        .post(BASE_PATH + "/ ")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));
  }

  @Test
  void uploadModel_missingFile_returns400() {
    String modelName = "it_" + Instant.now().toEpochMilli() + "_nofile";

    assumeMlSupported();

    givenAuthenticated()
        .contentType("multipart/form-data")
        .when()
        .post(BASE_PATH + "/" + modelName)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));
  }

  @Test
  void getModel_blankName_returns400() {
    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/ ")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));
  }

  @Test
  void deleteModel_blankName_returns400() {
    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/ ")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));
  }

  @Test
  void modelLifecycle_upload_get_head_list_delete_happyPath() {
    assumeMlSupported();

    String modelName = "it_" + Instant.now().toEpochMilli() + "_model.bin";
    byte[] payload = ("hello-" + modelName).getBytes(StandardCharsets.UTF_8);

    // upload
    givenAuthenticated()
        .contentType("multipart/form-data")
        .multiPart("file", modelName, payload, "application/octet-stream")
        .when()
        .post(BASE_PATH + "/" + modelName)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));

    // head exists
    givenAuthenticated()
        .when()
        .head(BASE_PATH + "/" + modelName)
        .then()
        .statusCode(200);

    // list contains
    Response listResponse =
        givenAuthenticated()
            .when()
            .get(BASE_PATH)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .response();

    String[] modelNames = listResponse.as(String[].class);
    assertThat(modelNames, hasItemInArray(modelName));

    // download
    Response downloadResponse =
        givenAuthenticated()
            .when()
            .get(BASE_PATH + "/" + modelName)
            .then()
            .statusCode(200)
            .extract()
            .response();

    assertThat(downloadResponse.contentType(), startsWith("application/octet-stream"));
    assertThat(downloadResponse.getHeader("Content-Disposition"), containsString(modelName));
    byte[] downloaded = downloadResponse.asByteArray();
    assertThat(downloaded, equalTo(payload));

    // delete (safe)
    deleteIfSafe(modelName);

    // head not found
    givenAuthenticated()
        .when()
        .head(BASE_PATH + "/" + modelName)
        .then()
        .statusCode(404);

    // get not found (json)
    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/" + modelName)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));
  }

  @Test
  void getModel_nonExistent_returns404() {
    assumeMlSupported();

    String modelName = "it_" + Instant.now().toEpochMilli() + "_missing.bin";

    givenAuthenticated()
        .when()
        .get(BASE_PATH + "/" + modelName)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));
  }

  @Test
  void deleteModel_nonExistent_returns404() {
    assumeMlSupported();

    String modelName = "it_" + Instant.now().toEpochMilli() + "_missing.bin";

    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + modelName)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("status", not(isEmptyOrNullString()));
  }

  private void deleteIfSafe(String modelName) {
    if (modelName == null || !modelName.startsWith("it_")) {
      throw new IllegalArgumentException("Refusing to delete non-test model: " + modelName);
    }

    givenAuthenticated()
        .when()
        .delete(BASE_PATH + "/" + modelName)
        .then()
        .statusCode(anyOf(is(200), is(404)));
  }

  private void assumeMlSupported() {
    Response response =
        givenAuthenticated()
            .when()
            .get(BASE_PATH)
            .then()
            .statusCode(anyOf(is(200), is(406)))
            .extract()
            .response();

    Assumptions.assumeTrue(
        response.statusCode() == 200,
        "ML not supported in this environment (GET " + BASE_PATH + " returned 406)"
    );
  }
}
