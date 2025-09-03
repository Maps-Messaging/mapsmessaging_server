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

package io.mapsmessaging.network.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class Auth0TokenGenerator implements TokenGenerator {

  private String domain;
  private Auth0TokenBody body;

  public Auth0TokenGenerator() {
  }

  public String getName() {
    return "auth0";
  }

  public String getDescription() {
    return "auth0 token generator https://auth0.com/";
  }

  @Override
  public TokenGenerator getInstance(Map<String, Object> properties) {
    return new Auth0TokenGenerator(properties);
  }

  @Override
  public String generate() throws IOException {
    try {
      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
      String json = ow.writeValueAsString(body);

      HttpResponse<String> response;
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://" + domain + "/oauth/token"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

      response = client.send(request, HttpResponse.BodyHandlers.ofString());


      if (response.statusCode() != 200) {
        throw new IOException("Failed to fetch token: " + response.body());
      }

      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readTree(response.body()).get("access_token").asText();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Request interrupted", e);
    }
  }

  private Auth0TokenGenerator(Map<String, Object> properties) {
    domain = ((String) properties.get("domain")).trim();
    body = new Auth0TokenBody(properties);
  }

  @Getter
  private static final class Auth0TokenBody {

    private final String clientId;
    private final String clientSecret;
    private final String audience;
    private final String grantType;

    public Auth0TokenBody(Map<String, Object> properties) {
      clientId = ((String) properties.get("client_id")).trim();
      clientSecret = ((String) properties.get("client_secret")).trim();
      audience = "https://" + ((String) properties.get("domain")).trim() + "/api/v2/";
      grantType = ((String) properties.get("grant_type")).trim();
    }
  }
}
