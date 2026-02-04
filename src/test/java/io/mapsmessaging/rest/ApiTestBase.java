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
import java.time.Duration;
import java.time.Instant;

public abstract class ApiTestBase extends BaseTestConfig {

  protected static String baseUrl;
  protected static Cookies authCookies;
  protected static OpenApiValidationFilter openApi;

  @BeforeAll
  static void initClient() throws IOException {
    baseUrl = System.getProperty("BASE_URL", System.getenv().getOrDefault("BASE_URL", "http://localhost:8080"));

    openApi = new OpenApiValidationFilter("src/test/resources/openapi.json");

    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

    waitUntilHealthy(Duration.ofSeconds(30));

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
