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

package io.mapsmessaging.rest;


import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import io.mapsmessaging.test.BaseTestConfig;
import io.restassured.RestAssured;
import io.restassured.http.Cookies;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public abstract class ApiTestBase extends BaseTestConfig {

  protected static String baseUrl;
  protected static Cookies authCookies;
  protected static OpenApiValidationFilter openApi;

  protected static boolean LOADED_OPENAPI = false;
  private static final Path OPENAPI_PATH =
      Path.of("src", "test", "resources", "openapi.json");

  @BeforeAll
  static void initClient() throws IOException {
    baseUrl = System.getProperty("BASE_URL", System.getenv().getOrDefault("BASE_URL", "http://localhost:8080"));
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    waitUntilHealthy(Duration.ofSeconds(30));
    fetchOpenApiSpec();
    openApi = new OpenApiValidationFilter(OPENAPI_PATH.toString());
    authCookies = CookieAuth.login(baseUrl, "admin", getPassword("admin"));
  }

  protected static io.restassured.specification.RequestSpecification givenAnonymous() {
    return RestAssured
        .given()
        .baseUri(baseUrl)
        .filter(openApi);
  }

  protected static io.restassured.specification.RequestSpecification givenAuthenticated() {
    return RestAssured
        .given()
        .baseUri(baseUrl)
        .cookies(authCookies)
        .filter(openApi);
  }

  protected RequestSpecification givenAuthenticatedNoValidation() {
    return RestAssured.given()
        .baseUri(baseUrl)
        .cookies(authCookies);
  }

  private static void fetchOpenApiSpec() throws IOException {
    if(LOADED_OPENAPI) return;
    try(HttpClient client = HttpClient.newHttpClient() ){

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/openapi.json"))
          .timeout(Duration.ofSeconds(60))
          .GET()
          .build();

      try {
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
          throw new IOException("Failed to fetch OpenAPI spec. HTTP " + response.statusCode());
        }

        Files.createDirectories(OPENAPI_PATH.getParent());

        try (InputStream body = response.body()) {
          Files.copy(body, OPENAPI_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        LOADED_OPENAPI = true;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while fetching OpenAPI spec", e);
      }
    }
  }

  private static void waitUntilHealthy(Duration timeout) {
    Instant deadline = Instant.now().plus(timeout);
    Throwable lastError = null;

    while (Instant.now().isBefore(deadline)) {
      try {
        // Change "/health" to whatever your server exposes.
        int status = RestAssured
            .given()
            .baseUri(baseUrl)
            .get("/health")
            .getStatusCode();

        if (status >= 200 && status < 500) {
          return;
        }
      } catch (Throwable t) {
        lastError = t;
      }

      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted while waiting for server health", e);
      }
    }

    RuntimeException e = new RuntimeException("Server not healthy at " + baseUrl + " within " + timeout);
    if (lastError != null) {
      e.addSuppressed(lastError);
    }
    throw e;
  }
}
