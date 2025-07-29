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

package io.mapsmessaging.network.protocol.impl.orbcomm.ogws;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.data.*;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.io.OrbCommOgwsEndPointServer;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class OrbcommOgwsClient {
  private final Gson gson = new GsonBuilder()
      .registerTypeAdapter(ElementType.class, new ElementTypeAdapter())
      .create();
  private final Logger logger = LoggerFactory.getLogger(OrbCommOgwsEndPointServer.class);

  private static final String BASE_URL = "https://ogws.orbcomm.com/api/v1.0";
  private final HttpClient httpClient;
  private final String clientId;
  private final String clientSecret;
  private String bearerToken;

  private long reAuthenticateTime;

  public OrbcommOgwsClient(String clientId, String clientSecret) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    if((clientId == null || clientId.isEmpty() ) || clientSecret == null || clientSecret.isEmpty()) {
      throw new IllegalArgumentException("Client id or secret cannot be null or empty");
    }
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
  }

  public boolean authenticate() throws InterruptedException, IOException {
    var body = "client_id=" + encode(clientId) +
        "&client_secret=" + encode(clientSecret) +
        "&grant_type=client_credentials";

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(BASE_URL + "/auth/token"))
        .timeout(Duration.ofSeconds(10))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() != 200) return false;

    GetTokenResponse tokenResponse = gson.fromJson(response.body(), GetTokenResponse.class);
    this.bearerToken = tokenResponse.getAccessToken();
    reAuthenticateTime = System.currentTimeMillis()+ (tokenResponse.getExpiresIn()*1000L);
    return tokenResponse.isSuccess();
  }

  public GetTerminalsInfoResponse getTerminals() throws IOException, InterruptedException {
    reauthenticate();
    HttpRequest request = authorizedGet("/info/terminals");
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    return gson.fromJson(response.body(), GetTerminalsInfoResponse.class);
  }

  public FromMobileMessagesResponse getFromMobileMessages(@NotNull String fromUtc) throws Exception {
    reauthenticate();
    String url = BASE_URL + "/get/re_messages?FromUTC=" + encode(fromUtc);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Authorization", "Bearer " + bearerToken)
        .GET()
        .build();

    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    return gson.fromJson(response.body(), FromMobileMessagesResponse.class);
  }

  public FwStatusResponse getFwStatuses(List<Long> ids) throws Exception {
    reauthenticate();
    String idParams = String.join("&IDList=", ids.stream().map(String::valueOf).toList());
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(BASE_URL + "/get/fw_statuses?IDList=" + idParams))
        .header("Authorization", "Bearer " + bearerToken)
        .GET()
        .build();

    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    return gson.fromJson(response.body(), FwStatusResponse.class);
  }

  public CancelMessagesResponse submitCancellations(List<Long> messageIds) throws Exception {
    reauthenticate();
    String body = gson.toJson(messageIds);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(BASE_URL + "/submit/cancellations"))
        .header("Authorization", "Bearer " + bearerToken)
        .header("Content-Type", "application/json")
        .POST(BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    return gson.fromJson(response.body(), CancelMessagesResponse.class);
  }

  public ServiceInfoResponse getServiceInfo(boolean includeErrorCodes) throws Exception {
    reauthenticate();
    String url = BASE_URL + "/info/service" + (includeErrorCodes ? "?GetErrorCodes=true" : "");
    HttpRequest request = authorizedGet(url);
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    return gson.fromJson(response.body(), ServiceInfoResponse.class);
  }

  public GetTerminalInfoResponse getTerminal(String primeId) throws Exception {
    reauthenticate();
    String url = BASE_URL + "/info/terminal?PrimeID=" + encode(primeId);
    HttpRequest request = authorizedGet(url);
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    return gson.fromJson(response.body(), GetTerminalInfoResponse.class);
  }


  public SubmitMessagesResponse submitMessage(List<SubmitMessage> submitMessages) throws Exception {
    reauthenticate();
    String jsonMessages = gson.toJson(submitMessages);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(BASE_URL + "/submit/messages"))
        .header("Authorization", "Bearer " + bearerToken)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonMessages))
        .build();

    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    return gson.fromJson(response.body(), SubmitMessagesResponse.class);
  }

  private HttpRequest authorizedGet(String path) {
    return HttpRequest.newBuilder()
        .uri(URI.create(BASE_URL + path))
        .header("Authorization", "Bearer " + bearerToken)
        .GET()
        .build();
  }

  private String encode(String value) {
    return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private void reauthenticate() throws IOException, InterruptedException {
    if(System.currentTimeMillis() > reAuthenticateTime){
      authenticate();
    }
  }
}
