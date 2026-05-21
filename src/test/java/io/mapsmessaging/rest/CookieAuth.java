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


import io.restassured.RestAssured;
import io.restassured.http.Cookies;
import io.restassured.response.Response;

import java.util.Objects;

public final class CookieAuth {

  private CookieAuth() {
  }

  public static Cookies login(String baseUrl, String username, String password) {
    Objects.requireNonNull(baseUrl, "baseUrl");
    Objects.requireNonNull(username, "username");
    Objects.requireNonNull(password, "password");

    // Adjust to your login endpoint + payload format.
    Response response = RestAssured
        .given()
        .baseUri(baseUrl)
        .contentType("application/json")
        .body("{\"username\":\"" + escapeJson(username) + "\",\"password\":\"" + escapeJson(password) + "\"}")
        .post("/api/v1/login")
        .then()
        .statusCode(200)
        .extract()
        .response();

    Cookies cookies = response.detailedCookies();
    if (cookies == null || cookies.asList().isEmpty()) {
      throw new IllegalStateException("Login returned 200 but no cookies were set");
    }
    return cookies;
  }

  private static String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}