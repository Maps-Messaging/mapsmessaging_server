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

package io.mapsmessaging.rest.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.security.SubjectHelper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Provider
public class AuthenticationFilter extends BaseAuthenticationFilter {

  private static final String secret = "very-secret-key-that-should-be-strong";
  private static final Algorithm algorithm = Algorithm.HMAC256(secret);


  @Context
  private HttpServletRequest httpRequest;

  @Context
  private HttpServletResponse httpResponse;

  protected void processAuthentication(ContainerRequestContext containerRequest) throws IOException {
    try {
      String auth = containerRequest.getHeaderString("authorization");
      if (auth == null) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
          session.invalidate();
        }
        return;
      }

      auth = auth.replaceFirst("[Bb]asic ", "");
      Base64.Decoder decoder = Base64.getDecoder();
      String userColonPass = new String(decoder.decode(auth));
      String[] split = userColonPass.split(":");
      String username = split[0];
      char[] password = split[1].toCharArray();

      HttpSession session = httpRequest.getSession(false);
      if (session != null) {
        Subject subject = (Subject) session.getAttribute("subject");
        if (subject != null && session.getAttribute(USERNAME) != null && session.getAttribute(USERNAME).equals(username)) {
          return; // all ok
        }
      }
      if (AuthManager.getInstance().validate(username, password)) {
        Subject subject = AuthManager.getInstance().getUserSubject(username);
        UUID uuid = SubjectHelper.getUniqueId(subject);
        String token = generateToken(username);

        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        httpRequest.setAttribute("Set-Cookie", cookie.toString());
        setupSession(httpRequest,username, uuid, subject, cookie.toString());
        httpResponse.addCookie(cookie);
      }
      else if (session != null) {
        session.invalidate();
      }
    } catch (Throwable e) {
      e.printStackTrace();
      throw e;
    }
  }

  public static String generateToken(String username) {
    return JWT.create()
        .withSubject(username)
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 mins
        .sign(algorithm);
  }

  public static String validateAndGetSubject(String token) {
    JWTVerifier verifier = JWT.require(algorithm).build();
    DecodedJWT jwt = verifier.verify(token);
    return jwt.getSubject();
  }
}
