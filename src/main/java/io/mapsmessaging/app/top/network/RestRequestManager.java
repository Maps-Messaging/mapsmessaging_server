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
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RestRequestManager implements Runnable {
  private static final RestRequestManager INSTANCE = new RestRequestManager();
  private final List<Object> queue;
  @Getter
  private final Client client;
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
    this.client = ClientBuilder.newClient();
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

    Response response = client
        .target(serverUrl + loginUrl)
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.json(json.toString()));

    if (response.getStatus() != 200) {
      throw new IOException("Login failed: " + response.getStatus());
    }
    connected.set(true);
  }

  private void refreshToken() throws IOException {
    Response response = client
        .target(serverUrl + refreshUrl)
        .request(MediaType.APPLICATION_JSON)
        .get();

    if (response.getStatus() != 200) {
      throw new IOException("Token refresh failed: " + response.getStatus());
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
    return queue.remove(0);
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
}
