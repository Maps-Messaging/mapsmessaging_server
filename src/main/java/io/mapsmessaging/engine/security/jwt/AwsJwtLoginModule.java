/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */
package io.mapsmessaging.engine.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import io.mapsmessaging.engine.security.BaseLoginModule;
import java.io.IOException;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

public class AwsJwtLoginModule extends BaseLoginModule {

  private String region;
  private String poolId;
  private String clientId;

  @Override
  public void initialize(
      Subject subject,
      CallbackHandler callbackHandler,
      Map<String, ?> sharedState,
      Map<String, ?> options) {
    super.initialize(subject, callbackHandler, sharedState, options);
    region = (String) options.get("region");
    poolId = (String) options.get("poolId");
    clientId = (String) options.get("clientId");
  }

  @Override
  public boolean login() throws LoginException {

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
      RSAKeyProvider keyProvider = new AwsCognitoRSAKeyProvider(region, poolId);
      Algorithm algorithm = Algorithm.RSA256(keyProvider);
      JWTVerifier jwtVerifier = JWT.require(algorithm)
          .withAudience(clientId)
          .build();
      jwtVerifier.verify(token);
      return true;
    } catch (IOException ioe) {
      throw new LoginException(ioe.toString());
    } catch (UnsupportedCallbackException uce) {
      throw new LoginException(
          "Error: "
              + uce.getCallback().toString()
              + " not available to garner authentication information "
              + "from the user");
    }
  }

    @Override
  public boolean commit() throws LoginException {
    return false;
  }

  @Override
  public boolean logout() throws LoginException {
    return super.logout();
  }

}
