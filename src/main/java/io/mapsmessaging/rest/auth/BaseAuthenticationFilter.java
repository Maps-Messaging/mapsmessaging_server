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
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.rest.api.impl.messaging.impl.RestMessageListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import lombok.Getter;
import lombok.Setter;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.UUID;

import static io.mapsmessaging.rest.auth.SessionTokenHandler.getAccessCookie;
import static io.mapsmessaging.rest.auth.SessionTokenHandler.validateToken;

public abstract class BaseAuthenticationFilter implements ContainerRequestFilter {

  @Context
  private HttpServletRequest httpRequest;

  @Context
  private HttpServletResponse httpResponse;


  @Getter
  @Setter
  protected static int maxInactiveInterval = 600;
  private static final String[] OPEN_PATHS = new String[] { "openapi.json" , "/health", "/api/v1/ping", "/api/v1/login"};
  private static final String[] FULL_PATHS = new String[] { "/api/v1/server/log/sse/stream/", "/api/v1/messaging/sse/stream" };

  @Override
  public void filter(ContainerRequestContext containerRequest) throws IOException {
    for(String path : OPEN_PATHS) {
      if (containerRequest.getUriInfo().getRequestUri().getPath().endsWith(path)) {
        return;
      }
    }
    for(String path : FULL_PATHS) {
      if (containerRequest.getUriInfo().getRequestUri().getPath().contains(path)) {
        return;
      }
    }
    processAuthentication(containerRequest);
  }

  protected void processAuthentication(ContainerRequestContext containerRequest) throws IOException {
    try {
      if(!AuthManager.getInstance().isAuthenticationEnabled())return;
      String accessToken = getAccessCookie(httpRequest);
      if (accessToken == null) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
          session.invalidate();
        }
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }

      HttpSession session = httpRequest.getSession(false);
      if (session == null) {
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
      String usernameFromToken = validateToken(accessToken, session, httpResponse);
      Object sessionUsername = session.getAttribute("username");
      if (sessionUsername == null || !sessionUsername.equals(usernameFromToken)) {
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      }
    } catch (Throwable e) {
      throw e;
    }
  }
}