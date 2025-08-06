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

package io.mapsmessaging.ml;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.selector.model.ModelStore;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@SuppressWarnings("java:S1075")
public class MapsModelStore implements ModelStore {

  private static final String URI_PATH="/server/model/";

  private final HttpClient client;
  private final String baseUrl;
  private final String username;
  private final String password;
  private String sessionCookie;

  public MapsModelStore(String baseUrl, String username, String password) throws IOException {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.username = username;
    this.password = password;
    this.client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    if (username != null && password != null) {
      login();
    }
  }

  private void login() throws IOException {
    JsonObject body = new JsonObject();
    body.addProperty("username", username);
    body.addProperty("password", password);
    body.addProperty("longLived", false);
    body.addProperty("persistent", false);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/login"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
        .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IOException("Login failed: " + response.statusCode() + " - " + response.body());
      }

      sessionCookie = response.headers()
          .firstValue("Set-Cookie")
          .map(cookie -> cookie.split(";", 2)[0]) // get "JSESSIONID=xyz"
          .orElseThrow(() -> new IOException("Missing Set-Cookie header from login"));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Login interrupted", e);
    }
  }

  private HttpRequest.Builder withAuth(HttpRequest.Builder builder) {
    if (sessionCookie != null) {
      builder.header("Cookie", sessionCookie);
    }
    return builder;
  }

  @Override
  public void saveModel(String modelName, byte[] data) throws IOException {
    String boundary = "----MapsBoundary" + System.currentTimeMillis();
    String body = "--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"file\"; filename=\"" + modelName + "\"\r\n"
        + "Content-Type: application/octet-stream\r\n\r\n";

    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    byteStream.write(body.getBytes(StandardCharsets.UTF_8));
    byteStream.write(data);
    byteStream.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

    HttpRequest request = withAuth(HttpRequest.newBuilder())
        .uri(URI.create(baseUrl + URI_PATH + modelName))
        .timeout(Duration.ofSeconds(15))
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .POST(HttpRequest.BodyPublishers.ofByteArray(byteStream.toByteArray()))
        .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IOException("Upload failed: " + response.statusCode() + " - " + response.body());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Upload interrupted", e);
    }
  }

  @Override
  public byte[] loadModel(String modelName) throws IOException {
    HttpRequest request = withAuth(HttpRequest.newBuilder())
        .uri(URI.create(baseUrl + URI_PATH + modelName))
        .timeout(Duration.ofSeconds(10))
        .GET()
        .build();

    try {
      HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() == 200) {
        try (InputStream in = response.body(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
          in.transferTo(out);
          return out.toByteArray();
        }
      }
      throw new IOException("Download failed: " + response.statusCode());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Download interrupted", e);
    }
  }

  @Override
  public boolean modelExists(String modelName) throws IOException {
    HttpRequest request = withAuth(HttpRequest.newBuilder())
        .uri(URI.create(baseUrl + URI_PATH + modelName))
        .timeout(Duration.ofSeconds(5))
        .method("HEAD", HttpRequest.BodyPublishers.noBody())
        .build();

    try {
      HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
      return response.statusCode() == 200;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  @Override
  public boolean deleteModel(String modelName) throws IOException {
    HttpRequest request = withAuth(HttpRequest.newBuilder())
        .uri(URI.create(baseUrl + URI_PATH + modelName))
        .timeout(Duration.ofSeconds(5))
        .DELETE()
        .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return response.statusCode() == 200;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  @Override
  public List<String> listModels() throws IOException {
    HttpRequest request = withAuth(HttpRequest.newBuilder())
        .uri(URI.create(baseUrl + "/server/models"))
        .timeout(Duration.ofSeconds(5))
        .GET()
        .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IOException("Failed to list models: " + response.statusCode() + " - " + response.body());
      }

      String body = response.body();
      Gson gson = new Gson();
      return gson.fromJson(body, List.class); // generic List<String>

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("List interrupted", e);
    }
  }

}