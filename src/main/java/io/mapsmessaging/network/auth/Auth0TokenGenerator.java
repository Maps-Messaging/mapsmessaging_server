/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.util.Map;
import lombok.Getter;

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
      HttpResponse<JsonNode> response = Unirest.post("https://" + domain + "/oauth/token")
          .header("content-type", "application/json")
          .body(json)
          .asJson();
      return response.getBody().getObject().getString("access_token");
    } catch (UnirestException e) {
      throw new IOException(e);
    }
  }

  private Auth0TokenGenerator(Map<String, Object>  properties) {
    domain = ((String)properties.get("domain")).trim();
    body = new Auth0TokenBody(properties);
  }

  private static final class Auth0TokenBody {

    @Getter
    private final String clientId;
    @Getter
    private final String clientSecret;
    @Getter
    private final String audience;
    @Getter
    private final String grantType;

    public Auth0TokenBody(Map<String, Object>  properties) {
      clientId = ((String)properties.get("client_id")).trim();
      clientSecret = ((String)properties.get("client_secret")).trim();
      audience = "https://" + ((String)properties.get("domain")).trim() + "/api/v2/";
      grantType = ((String)properties.get("grant_type")).trim();
    }
  }

}
