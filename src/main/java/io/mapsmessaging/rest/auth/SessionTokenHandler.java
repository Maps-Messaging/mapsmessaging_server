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
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.io.Constants;
import io.mapsmessaging.security.SubjectHelper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class SessionTokenHandler {

  private static final Algorithm algorithm = Algorithm.HMAC256(MessageDaemon.getInstance().getTokenSecret());
  private static final String TOKEN_NAME = "maps_access_token";
  private static final String USERNAME = "username";
  private static final int maxInactiveInterval = 600;
  private static final int TOKEN_LIFETIME = 15 * 60; // 15 minutes
  private static final long TOKEN_EXPIRATION_THRESHOLD = 2L * 60L * 1000L;


  public static HttpSession setupCookieAndSession(String username, Subject subject, HttpServletRequest httpRequest, HttpServletResponse httpResponse, int maxAge) {
    String token = generateToken(username, maxAge);
    UUID uuid = SubjectHelper.getUniqueId(subject);
    buildAccessCookie(token, maxAge, httpRequest, httpResponse);

    String sessionId = httpRequest.getSession().getId();  // Or get it from response if freshly created
    StringBuilder jsessionCookie = new StringBuilder("JSESSIONID=")
        .append(sessionId)
        .append("; Path=/; HttpOnly");
    if (isSecure(httpRequest)) {
      jsessionCookie.append("; SameSite=None; Secure");
    }
    httpResponse.addHeader("Set-Cookie", jsessionCookie.toString());

    return setupSession(httpRequest, username, uuid, subject);
  }

  private static boolean isSecure(HttpServletRequest request) {
    String scheme = request.getHeader("X-Forwarded-Proto");
    return "https".equalsIgnoreCase(scheme) || request.isSecure();
  }

  public static void buildAccessCookie(String token, int maxAge,HttpServletRequest request, HttpServletResponse response) {
    StringBuilder cookieValue = new StringBuilder(TOKEN_NAME)
        .append("=")
        .append(token)
        .append("; Path=/; HttpOnly; Max-Age=")
        .append(maxAge);
    if (isSecure(request)) {
      cookieValue.append("; SameSite=None; Secure");
    }
    response.addHeader("Set-Cookie", cookieValue.toString());
  }

  public static String generateToken(String username, int age) {
    return JWT.create()
        .withSubject(username)
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + (age * 1000L)))
        .sign(algorithm);
  }

  public static HttpSession setupSession(HttpServletRequest httpRequest, String username, UUID uuid, Subject subject) {
    String scheme = httpRequest.getScheme();
    String remoteIp = httpRequest.getHeader("X-Forwarded-For");
    if (remoteIp != null && remoteIp.contains(",")) {
      remoteIp = remoteIp.split(",")[0].trim();
    }
    if (remoteIp == null) {
      remoteIp = httpRequest.getRemoteAddr();
    }
    String name = scheme + "_/" + remoteIp + ":" + httpRequest.getRemotePort();

    HttpSession session = httpRequest.getSession(true);
    session.setAttribute("name", name);
    session.setMaxInactiveInterval(maxInactiveInterval);
    session.setAttribute(USERNAME, username);
    session.setAttribute("subject", subject);
    session.setAttribute("uuid", uuid);
    session.setAttribute("connectionId",  Constants.getNextId());
    return session;
  }

  public static String validateToken(String accessToken, HttpSession session,HttpServletRequest request, HttpServletResponse httpResponse) throws IOException {
    try {
      DecodedJWT jwt = JWT.require(algorithm).build().verify(accessToken);
      Date threshold = new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_THRESHOLD); // if we are 2 minutes to expiry, refresh
      Date expiry = jwt.getExpiresAt();
      if (expiry.before(threshold)) {
        renewSession(session, request, httpResponse);
      }
      return jwt.getSubject();
    } catch (JWTVerificationException ex) {
      httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return "";
    }
  }

  public static void renewSession(HttpSession session, HttpServletRequest request, HttpServletResponse response){
    String username = session.getAttribute(USERNAME).toString();
    String token = generateToken(username, TOKEN_LIFETIME);
    buildAccessCookie(token, TOKEN_LIFETIME, request, response);
  }

  public static void clearToken(HttpServletRequest request, HttpServletResponse response) {
    buildAccessCookie("", 0, request, response);
  }

  public static String getAccessCookie(HttpServletRequest httpRequest) {
    Cookie[] cookies = httpRequest.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (TOKEN_NAME.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  private SessionTokenHandler(){}
}
