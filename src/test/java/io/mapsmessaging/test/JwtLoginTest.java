package io.mapsmessaging.test;


import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.mapsmessaging.network.auth.Auth0TokenGenerator;
import io.mapsmessaging.network.auth.TokenGenerator;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
import org.json.JSONObject;

public class JwtLoginTest {


  public static void main(String[] args) throws UnirestException, IOException {
    testLib();
    test();
  }

  public static void testLib() throws IOException {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("client_id", "cvSiNWKvfWMBirVX36JgkhzztIl89wpO");
    properties.put("client_secret", "436jRiLkXkLZ1FJMtn4IBKpTJuyfx2Vc5Jl3-XOnExHUJ1479ATbSYPgcEU1Vy21");
    properties.put("domain", "dev-krmpy6-z.au.auth0.com");
    properties.put("grant_type", "client_credentials");
    TokenGenerator tokenGenerator = new Auth0TokenGenerator();
    tokenGenerator = tokenGenerator.getInstance(properties);
    System.err.println(tokenGenerator.generate());

  }
  public static void test() throws UnirestException {
    HttpResponse<String> response = Unirest.post("https://dev-krmpy6-z.au.auth0.com/oauth/token")
        .header("content-type", "application/json")
        .body("{\"client_id\":\"cvSiNWKvfWMBirVX36JgkhzztIl89wpO\",\"client_secret\":\"436jRiLkXkLZ1FJMtn4IBKpTJuyfx2Vc5Jl3-XOnExHUJ1479ATbSYPgcEU1Vy21\",\"audience\":\"https://dev-krmpy6-z.au.auth0.com/api/v2/\",\"grant_type\":\"client_credentials\"}")
        .asString();
    JSONObject json = new JSONObject(response.getBody());

    System.err.println(json.toString(2));

  }
}
