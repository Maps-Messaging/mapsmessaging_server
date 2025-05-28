package io.mapsmessaging.rest.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.mapsmessaging.security.SubjectHelper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class SessionTokenHandler {

  private static final String secret = "very-secret-key-that-should-be-strong";
  private static final Algorithm algorithm = Algorithm.HMAC256(secret);
  private static final String USERNAME = "username";
  private static int maxInactiveInterval = 600;
  private static int TOKEN_LIFETIME = 15 * 60; // 15 minutes
  private static long TOKEN_EXPIRATION_THRESHOLD = 2L * 60L * 1000L;


  public static HttpSession setupCookieAndSession(String username, Subject subject, HttpServletRequest httpRequest, HttpServletResponse httpResponse, int maxAge) {
    String token = generateToken(username, maxAge);
    UUID uuid = SubjectHelper.getUniqueId(subject);
    buildAccessCookie(token, maxAge, httpResponse);
    String sessionId = httpRequest.getSession().getId();  // Or get it from response if freshly created
    String jsessionCookie = "JSESSIONID=" + sessionId + "; Path=/; HttpOnly; Secure; SameSite=None";
    httpResponse.addHeader("Set-Cookie", jsessionCookie);
    return setupSession(httpRequest, username, uuid, subject);
  }

  public static void buildAccessCookie(String token, int maxAge, HttpServletResponse httpResponse) {
    Cookie cookie = new Cookie("access_token", token);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);
    String cookieValue = "access_token=" + token + "; Path=/; HttpOnly; Secure; SameSite=None; Max-Age=" + maxAge;
    httpResponse.addHeader("Set-Cookie", cookieValue);
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
    return session;
  }

  public static String validateToken(String accessToken, HttpSession session, HttpServletResponse httpResponse) throws IOException {
    try {
      DecodedJWT jwt = JWT.require(algorithm).build().verify(accessToken);
      Date threshold = new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_THRESHOLD); // if we are 2 minutes to expiry, refresh
      Date expiry = jwt.getExpiresAt();
      if (expiry.before(threshold)) {
        renewSession(session, httpResponse);
      }
      return jwt.getSubject();
    } catch (JWTVerificationException ex) {
      httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return "";
    }
  }

  public static void renewSession(HttpSession session, HttpServletResponse response) throws IOException {
    String username = session.getAttribute(USERNAME).toString();
    String token = generateToken(username, TOKEN_LIFETIME);
    buildAccessCookie(token, TOKEN_LIFETIME, response);
  }

  public static String getAccessCookie(HttpServletRequest httpRequest) {
    Cookie[] cookies = httpRequest.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("access_token".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

}
