package io.mapsmessaging.network.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
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
  public TokenGenerator getInstance(ConfigurationProperties properties) {
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

  private Auth0TokenGenerator(ConfigurationProperties properties) {
    domain = properties.getProperty("domain").trim();
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

    public Auth0TokenBody(ConfigurationProperties properties) {
      clientId = properties.getProperty("client_id").trim();
      clientSecret = properties.getProperty("client_secret").trim();
      audience = "https://" + properties.getProperty("domain").trim() + "/api/v2/";
      grantType = properties.getProperty("grant_type").trim();
    }
  }

}