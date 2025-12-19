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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import lombok.Getter;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RestRequestManager implements Runnable {
  private static final RestRequestManager INSTANCE = new RestRequestManager();
  private final List<Object> queue;
  @Getter
  private final HttpClient client;
  private final CookieManager cookieManager;
  private final String loginUrl = "/api/v1/login";
  private final String refreshUrl = "/api/v1/refreshToken";
  private String serverUrl;
  private String username;
  private String password;

  private final List<RestApiConnection> requests;


  private AtomicBoolean running = new AtomicBoolean(true);
  private AtomicBoolean connected = new AtomicBoolean(false);


  private RestRequestManager() {
    this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    CookieHandler.setDefault(cookieManager);
    this.client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    requests = new LinkedList<>();
    queue = new LinkedList<>();
  }

  public static RestRequestManager getInstance() {
    return INSTANCE;
  }

  public void initialize(String serverUrl, String username, String password) throws IOException {
    this.serverUrl = serverUrl;
    this.username = username;
    this.password = password;
    requests.add(new ServerDestinationStatusRequest(serverUrl));
    requests.add(new ServerDetailsRequest(serverUrl));
    requests.add(new ServerInfoRequest(serverUrl));
    requests.add(new ServerInterfaceStatusRequest(serverUrl));
    login();
    Thread t = new Thread(this);
    t.start();
  }

  public synchronized void ensureValidSession() throws IOException {
    if (getJwtRemainingSeconds() < 60) {
      refreshToken();
    }
  }

  private void login() throws IOException {
    JsonObject json = new JsonObject();
    json.addProperty("username", username);
    json.addProperty("password", password);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(serverUrl + loginUrl))
        .timeout(Duration.ofSeconds(15))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
        .build();

    HttpResponse<String> response = send(request);
    if (response.statusCode() != 200) {
      throw new IOException("Token refresh failed: " + response.statusCode());
    }
    connected.set(true);
  }


  private HttpResponse<String> send(HttpRequest request) throws IOException {
    try {
      return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("HTTP request interrupted", e);
    }
  }

  private void refreshToken() throws IOException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(serverUrl + refreshUrl))
        .timeout(Duration.ofSeconds(15))
        .header("Accept", "application/json")
        .GET()
        .build();

    HttpResponse<String> response = send(request);

    if (response.statusCode() != 200) {
      throw new IOException("Token refresh failed: " + response.statusCode());
    }
  }

  private long getJwtRemainingSeconds() {
    return cookieManager.getCookieStore().getCookies().stream()
        .filter(c -> c.getName().equals("access_token"))
        .findFirst()
        .map(c -> {
          String[] parts = c.getValue().split("\\.");
          if (parts.length < 2) return 0L;
          String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
          JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
          long exp = obj.get("exp").getAsLong();
          return exp - Instant.now().getEpochSecond();
        })
        .orElse(0L);
  }

  public void run(){
    while(running.get()){
      boolean test = true;
      for(RestApiConnection request : requests){
        try {
          Object result = request.getData();
          if(result != null){
            queue.add(result);
          }
        } catch (Throwable e) {
          connected.set(false);
          test = false;
          // Ignore
        }
      }
      connected.set(test);
      try {
        Thread.sleep(6000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        // ignore
      }
    }
  }

  public Object getUpdate() {
    return queue.removeFirst();
  }

  public boolean isQueueEmpty() {
    return queue.isEmpty();
  }

  public boolean isConnected() {
    return connected.get();
  }

  public void close() {
    running.set(false);
    connected.set(false);
  }

  public String buildCookieHeader() {
    StringBuilder builder = new StringBuilder();
    for (var cookie : cookieManager.getCookieStore().getCookies()) {
      if (cookie.getName() == null || cookie.getValue() == null) {
        continue;
      }

      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(cookie.getName()).append("=").append(cookie.getValue());
    }
    return builder.toString();
  }
}
