/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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
import com.sun.jersey.core.util.Priority;
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
@Priority(1)
public class AuthenticationFilter implements ContainerRequestFilter {

  private static final String USERNAME = "username";

  // Exception thrown if user is unauthorized.
  private static final WebApplicationException unauthorized =
      new WebApplicationException(
          Response.status(Response.Status.UNAUTHORIZED)
              .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"realm\"")
              .entity("Page requires login.").build());

  @Context
  private HttpServletRequest httpRequest;

  @Override
  public void filter(ContainerRequestContext containerRequest) throws IOException {

    // Get the authentication passed in HTTP headers parameters
    String auth = containerRequest.getHeaderString("authorization");
    if (auth == null)
      throw unauthorized;


    auth = auth.replaceFirst("[Bb]asic ", "");
    String userColonPass = Base64.base64Decode(auth);
    String[] split = userColonPass.split(":");
    String username = split[0];
    String password = split[1];

    Subject subject;
    HttpSession session = httpRequest.getSession(true);
    if (!session.isNew()) {
      subject = (Subject) session.getAttribute("subject");
      if (subject == null ||
          session.getAttribute(USERNAME) == null ||
          !session.getAttribute(USERNAME).equals(username)
      ) {
        throw unauthorized;
      }
    } else {
      if (AuthManager.getInstance().isAuthenticationEnabled() && !AuthManager.getInstance().validate(username, password)) {
        throw unauthorized;
      }
      subject = AuthManager.getInstance().getUserSubject(username);
      session.setAttribute(USERNAME, username);
      session.setAttribute("subject", subject);
    }

    if (subject == null) {
      throw unauthorized;
    }

    boolean isWrite = false;
    switch (containerRequest.getMethod()) {
      case "GET":
      case "HEAD":
      case "OPTIONS":
        break;

      case "PUT":
      case "POST":
      case "DELETE":
        isWrite = true;
        break;

      default:
        throw unauthorized;
    }

    /*
    if(!isWrite && AuthManager.getInstance().isAuthorisationEnabled() && !AuthManager.getInstance().isAuthorised(subject, RestAccessControl.READ_ONLY)){
      throw unauthorized;
    }
    if(isWrite && AuthManager.getInstance().isAuthorisationEnabled() && !AuthManager.getInstance().isAuthorised(subject, RestAccessControl.WRITE)){
      throw unauthorized;
    }

     */
  }

}
