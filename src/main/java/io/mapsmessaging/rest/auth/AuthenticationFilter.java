/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.rest.auth;

import com.sun.jersey.core.util.Base64;
import io.mapsmessaging.auth.AuthManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import javax.security.auth.Subject;
import java.io.IOException;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {

  private static final String USERNAME = "username";

  private static final Response unauthorizedResponse = Response
      .status(Response.Status.UNAUTHORIZED)
      .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"realm\"")
      .entity("<html><body>Page requires login.</body></html>").build();


  // Exception thrown if user is unauthorized.
  private static final WebApplicationException unauthorized =
      new WebApplicationException(
          Response.status(Response.Status.UNAUTHORIZED)
              .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"realm\"")
              .entity("<html><body>Page requires login.</body></html>").build());

  @Context
  private HttpServletRequest httpRequest;

  @Override
  public void filter(ContainerRequestContext containerRequest) throws IOException {
    if(!AuthManager.getInstance().isAuthenticationEnabled())return;

    if (containerRequest.getUriInfo().getRequestUri().getPath().contains("openapi.json")) {
      return;
    }
    // Get the authentication passed in HTTP headers parameters
    String auth = containerRequest.getHeaderString("authorization");
    if (auth == null) {
      containerRequest.abortWith(unauthorizedResponse);
      throw unauthorized;
    }

    auth = auth.replaceFirst("[Bb]asic ", "");
    String userColonPass = Base64.base64Decode(auth);
    String[] split = userColonPass.split(":");
    String username = split[0];
    char[] password = split[1].toCharArray();

    HttpSession session = httpRequest.getSession(false);
    if (session != null) {
      Subject subject = (Subject) session.getAttribute("subject");
      if (subject != null && session.getAttribute(USERNAME) != null && session.getAttribute(USERNAME).equals(username)){
        return; // all ok
      }
    }
    if (AuthManager.getInstance().validate(username, password)) {
      session = httpRequest.getSession(true);
      Subject subject = AuthManager.getInstance().getUserSubject(username);
      session.setAttribute(USERNAME, username);
      session.setAttribute("subject", subject);
      return;
    }
    containerRequest.abortWith(unauthorizedResponse);
  }

}
