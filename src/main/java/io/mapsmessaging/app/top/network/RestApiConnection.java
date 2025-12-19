/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.app.top.network;

import com.google.gson.*;
import io.mapsmessaging.utilities.GsonFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public abstract class RestApiConnection {
  protected final Gson gson;
  protected final String url;
  protected final String endpoint;

  protected RestApiConnection(String url, String endpoint) {
    this.url = url;
    this.endpoint = endpoint;
    this.gson = GsonFactory.getInstance().getPrettyGson();
  }

  public abstract Object parse(JsonElement jsonElement) throws JsonParseException;

  public Object getData() throws IOException {
    RestRequestManager manager = RestRequestManager.getInstance();
    manager.ensureValidSession();
    String fullUrl = buildUrl(url, endpoint);

    String cookieHeader = manager.buildCookieHeader();

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(fullUrl))
        .timeout(Duration.ofSeconds(15))
        .header("Accept", "application/json")
        .header("Cookie", cookieHeader)
        .GET()
        .build();

    HttpResponse<String> response = send(manager, request);

    int statusCode = response.statusCode();
    if (statusCode == 200) {
      String jsonString = response.body();
      JsonElement jsonElement = parseResponse(jsonString);
      return parse(jsonElement);
    }
    if (statusCode == 403) {
      throw new IOException("Access denied");
    }
    throw new IOException("Unexpected error: " + statusCode);
  }

  private JsonElement parseResponse(String jsonString) {
    if (jsonString == null) {
      return new JsonObject();
    }

    String trimmed = jsonString.trim();
    if (trimmed.startsWith("[")) {
      JsonArray jsonArray = JsonParser.parseString(trimmed).getAsJsonArray();
      JsonObject jsonObject = new JsonObject();
      jsonObject.add("data", jsonArray);
      return jsonObject;
    }

    if (trimmed.isEmpty()) {
      return new JsonObject();
    }

    return JsonParser.parseString(trimmed);
  }

  private HttpResponse<String> send(RestRequestManager manager, HttpRequest request) throws IOException {
    try {
      return manager.getClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("HTTP request interrupted", e);
    }
  }

  private String buildUrl(String baseUrl, String path) {
    if (baseUrl.endsWith("/") && path.startsWith("/")) {
      return baseUrl.substring(0, baseUrl.length() - 1) + path;
    }
    if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
      return baseUrl + "/" + path;
    }
    return baseUrl + path;
  }


}
