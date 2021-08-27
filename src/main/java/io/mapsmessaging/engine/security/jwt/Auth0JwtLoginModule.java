package io.mapsmessaging.engine.security.jwt;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.mapsmessaging.engine.security.BaseLoginModule;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

public class Auth0JwtLoginModule extends BaseLoginModule {

  private String domain;

  @Override
  public void initialize(
      Subject subject,
      CallbackHandler callbackHandler,
      Map<String, ?> sharedState,
      Map<String, ?> options) {
    super.initialize(subject, callbackHandler, sharedState, options);
    domain = (String)options.get("auth0Domain");
  }

  @Override
  public boolean login() throws LoginException {
    succeeded = false;
    // prompt for a user name and password
    if (callbackHandler == null) {
      throw new LoginException("Error: no CallbackHandler available to garner authentication information from the user");
    }

    Callback[] callbacks = new Callback[2];
    callbacks[0] = new NameCallback("user name: ");
    callbacks[1] = new PasswordCallback("password: ", false);

    try {
      callbackHandler.handle(callbacks);
      username = ((NameCallback) callbacks[0]).getName();
      char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
      if (tmpPassword == null) {
        tmpPassword = new char[0];
      }
      String token = new String(tmpPassword);
      ((PasswordCallback) callbacks[1]).clearPassword();
      // Password should be a valid JWT
      JwkProvider provider = new UrlJwkProvider("https://"+domain+"/");
      DecodedJWT jwt = JWT.decode(token);
      // Get the kid from received JWT token
      Jwk jwk = provider.get(jwt.getKeyId());

      Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

      JWTVerifier verifier = JWT.require(algorithm)
          .withIssuer("https://"+domain+"/")
          .build();

      verifier.verify(token);
      succeeded = true;
    } catch (JwkException | IOException ioe) {
      throw new LoginException(ioe.toString());
    } catch (UnsupportedCallbackException uce) {
      throw new LoginException(
          "Error: "
              + uce.getCallback().toString()
              + " not available to garner authentication information "
              + "from the user");
    }
    return succeeded;
  }

  @Override
  public boolean commit() {
    if (!succeeded) {
      return succeeded;
    } else {
      username = null;
      if (password != null) {
        Arrays.fill(password, ' ');
        password = null;
      }
      commitSucceeded = true;
      return true;
    }
  }
}