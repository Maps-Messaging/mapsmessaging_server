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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import lombok.Getter;
import lombok.Setter;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.UUID;

public abstract class BaseAuthenticationFilter implements ContainerRequestFilter {

  @Getter
  @Setter
  protected static int maxInactiveInterval = 600;
  protected static final String USERNAME = "username";
  private static final String[] OPEN_PATHS = new String[] { "openapi.json" , "/health", "/api/v1/ping"};
  private static final String[] FULL_PATHS = new String[] { "/api/v1/server/log/sse/stream/" };

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

  protected void setupSession(HttpServletRequest httpRequest, String username, UUID uuid, Subject subject, String jwtCookie) {
    String scheme = httpRequest.getScheme();
    String remoteIp = httpRequest.getRemoteAddr();
    String name = scheme+"_/"+remoteIp+":"+httpRequest.getRemotePort();

    HttpSession session = httpRequest.getSession(true);
    session.setAttribute("name", name);
    session.setMaxInactiveInterval(maxInactiveInterval);
    session.setAttribute(USERNAME, username);
    session.setAttribute("subject", subject);
    session.setAttribute("uuid", uuid);
    session.setAttribute("jwtCookie", jwtCookie);
  }

  protected abstract void processAuthentication(ContainerRequestContext containerRequest) throws IOException;
}