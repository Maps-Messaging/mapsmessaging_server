package io.mapsmessaging.engine.security.jwt;

import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class AwsCognitoRSAKeyProvider implements RSAKeyProvider {

  private final URL aws_kid_store_url;
  private final JwkProvider provider;

  public AwsCognitoRSAKeyProvider(String aws_cognito_region, String aws_user_pools_id) {
    String url = String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json", aws_cognito_region, aws_user_pools_id);
    try {
      aws_kid_store_url = new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(String.format("Invalid URL provided, URL=%s", url));
    }
    provider = new JwkProviderBuilder(aws_kid_store_url).build();
  }


  @Override
  public RSAPublicKey getPublicKeyById(String kid) {
    try {
      return (RSAPublicKey) provider.get(kid).getPublicKey();
    } catch (JwkException e) {
      throw new RuntimeException(String.format("Failed to get JWT kid=%s from aws_kid_store_url=%s", kid, aws_kid_store_url));
    }
  }

  @Override
  public RSAPrivateKey getPrivateKey() {
    return null;
  }

  @Override
  public String getPrivateKeyId() {
    return null;
  }
}
