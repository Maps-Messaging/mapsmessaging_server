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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mapsmessaging.dto.rest.config.protocol.impl.SatelliteConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.io.SatelliteClient;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.io.StateManager;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.MessageData;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.RemoteDeviceInfo;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.data.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class OrbcommOgwsClient implements SatelliteClient {

  private final Gson gson = new GsonBuilder()
      .registerTypeAdapter(ElementType.class, new ElementTypeAdapter())
      .disableHtmlEscaping()
      .create();
  private final Logger logger = LoggerFactory.getLogger(OrbcommOgwsClient.class);

  private final String baseUrl;
  private final HttpClient httpClient;
  private final String clientId;
  private final String clientSecret;
  private final SatelliteConfigDTO config;
  private String bearerToken;
  private long reAuthenticateTime;
  private String lastMessageUtc;

  public OrbcommOgwsClient(SatelliteConfigDTO config) {
    this.config = config;
    this.baseUrl = config.getBaseUrl();
    this.clientId = config.getRemoteAuthConfig().getUsername();
    this.clientSecret = config.getRemoteAuthConfig().getPassword();
    if ((clientId == null || clientId.isEmpty()) || clientSecret == null || clientSecret.isEmpty()) {
      throw new IllegalArgumentException("Client id or secret cannot be null or empty");
    }
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(config.getHttpRequestTimeout()))
        .build();
    lastMessageUtc = StateManager.loadLastMessageUtc(clientId, clientSecret);
  }


  @Override
  public void close(){
  }


  public boolean authenticate() throws InterruptedException, IOException {
    var body = "client_id=" + encode(clientId) +
        "&client_secret=" + encode(clientSecret) +
        "&grant_type=client_credentials";

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/auth/token"))
        .timeout(Duration.ofSeconds(config.getHttpRequestTimeout()))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    logger.log(OGWS_SENDING_REQUEST, request.uri(), response.statusCode());
    if (response.statusCode() != 200) return false;

    GetTokenResponse tokenResponse = gson.fromJson(response.body(), GetTokenResponse.class);
    this.bearerToken = tokenResponse.getAccessToken();
    reAuthenticateTime = System.currentTimeMillis() + (tokenResponse.getExpiresIn() * 1000L);
    return tokenResponse.isSuccess();
  }

  public List<RemoteDeviceInfo> getTerminals(String deviceId) throws IOException, InterruptedException {
    reauthenticate();
    if (deviceId == null) {
      return getAllTerminals();
    }
    else{
      return getTerminalInfo(deviceId);
    }
  }

  private List<RemoteDeviceInfo> getTerminalInfo(String deviceId) throws IOException, InterruptedException {
    long time = System.currentTimeMillis();
    try {
      HttpRequest request = authorizedGet("/info/terminal?PrimeID="+deviceId);
      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      GetTerminalInfoResponse terminalInfoRemote = gson.fromJson(response.body(), GetTerminalInfoResponse.class);
      if(terminalInfoRemote.isSuccess()) {
        return List.of(terminalInfoRemote.getTerminal());
      }
      throw new IOException("Failed to get terminals");
    } finally {
      logger.log(OGWS_WEB_REQUEST_STATS, "getTerminalInfo", System.currentTimeMillis() - time);
    }
  }

  private List<RemoteDeviceInfo> getAllTerminals() throws IOException, InterruptedException {
    long time = System.currentTimeMillis();
    try {
      HttpRequest request = authorizedGet("/info/terminals");
      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      GetTerminalsInfoResponse terminalsInfoResponse = gson.fromJson(response.body(), GetTerminalsInfoResponse.class);
      if(terminalsInfoResponse.isSuccess()) {
        return new ArrayList<>(terminalsInfoResponse.getTerminals());
      }
      throw new IOException("Failed to get terminals");
    } finally {
      logger.log(OGWS_WEB_REQUEST_STATS, "getAllTerminals", System.currentTimeMillis() - time);
    }
  }

  private FromMobileMessagesResponse getFromMobileMessages(String fromUtc) throws Exception {
    long time = System.currentTimeMillis();
    try {
      reauthenticate();
      String url = baseUrl + "/get/re_messages?IncludeRawPayload=true";
      if (fromUtc != null) {
        url = url + "&FromUTC=" + encode(fromUtc);
      }
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Authorization", "Bearer " + bearerToken)
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      logger.log(OGWS_SENDING_REQUEST, request.uri(), response.statusCode());

      return gson.fromJson(response.body(), FromMobileMessagesResponse.class);
    } finally {
      logger.log(OGWS_WEB_REQUEST_STATS, "getFromMobileMessages", System.currentTimeMillis() - time);
    }
  }

  public Queue<MessageData> scanForIncoming(){
    Queue<MessageData> incomingEvents = new LinkedList<>();
    try {
      FromMobileMessagesResponse response = getFromMobileMessages(lastMessageUtc);
      if (response != null && response.isSuccess()) {
        if (!response.getMessages().isEmpty()) {
          lastMessageUtc = response.getNextFromUtc();
          for(ReturnMessage returnMessage: response.getMessages()){
            if(returnMessage.getPayload() == null) {
              MessageData messageData = new MessageData();
              messageData.setUniqueId(returnMessage.getMobileId());
              messageData.setPayload(Base64.getDecoder().decode(returnMessage.getRawPayload()));
              incomingEvents.add(messageData);
            }
            else {
              String json = gson.toJson(returnMessage);
              MessageData messageData = new MessageData();
              messageData.setMin(returnMessage.getPayload().getMin());
              messageData.setSin(returnMessage.getPayload().getSin());
              messageData.setUniqueId(returnMessage.getMobileId());
              messageData.setPayload(json.getBytes());
              messageData.setCommon(true);
              incomingEvents.add(messageData);
            }
          }
          if(lastMessageUtc != null) {
            StateManager.saveLastMessageUtc(clientId, clientSecret,lastMessageUtc);
          }
        }
      } else {
        logger.log(OGWS_FAILED_POLL, response != null ? response.getErrorId() : "<null error>");
      }
    } catch (Exception e) {
      logger.log(OGWS_REQUEST_FAILED, e);
    }
    return incomingEvents;
  }

  public void processPendingMessages(List<MessageData> pendingMessages) {
    if(pendingMessages.isEmpty())return;
    HttpResponse<String> response = null;
    List<SubmitMessage> pending = new LinkedList<>();
    try {
      reauthenticate();
      for(MessageData messageData: pendingMessages) {
        SubmitMessage submitMessage = new SubmitMessage();
        submitMessage.setRawPayload(Base64.getEncoder().encodeToString(messageData.getPayload()));
        submitMessage.setDestinationId(messageData.getUniqueId());
        submitMessage.setTransportType(0);
        submitMessage.setCompletionCallback(messageData.getCompletionCallback());
        pending.add(submitMessage);
      }
      String jsonMessages = gson.toJson(pending);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/submit/messages"))
          .header("Authorization", "Bearer " + bearerToken)
          .header("Content-Type", "application/json")
          .POST(BodyPublishers.ofString(jsonMessages))
          .build();

      response = httpClient.send(request, BodyHandlers.ofString());
      logger.log(OGWS_SENDING_REQUEST, request.uri(), response.statusCode());
    } catch (IOException e) {
      // Log this
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    finally {
      if(!pending.isEmpty()) {
        for (SubmitMessage message : pending) {
          if (message.getCompletionCallback() != null) {
            message.getCompletionCallback().run();
          }
        }
      }
    }
    SubmitMessagesResponse submitMessagesResponse = gson.fromJson(response.body(), SubmitMessagesResponse.class);
  }

  @Override
  public void unmute(String deviceId) {
    // No Op for Orbcomm
  }

  @Override
  public void mute(String deviceId) {
    // No Op for Orbcomm
  }

  private HttpRequest authorizedGet(String path) {
    return HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + path))
        .header("Authorization", "Bearer " + bearerToken)
        .GET()
        .build();
  }

  private String encode(String value) {
    return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private void reauthenticate() throws IOException, InterruptedException {
    if (System.currentTimeMillis() > reAuthenticateTime) {
      authenticate();
    }
  }


}
