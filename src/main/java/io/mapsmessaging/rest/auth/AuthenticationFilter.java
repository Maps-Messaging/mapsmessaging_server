/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.security.SubjectHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Base64;
import javax.security.auth.Subject;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {

  private static final String USERNAME = "username";

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
      HttpSession session = httpRequest.getSession(false);
      if(session != null) {
        session.invalidate();
      }
      return;
    }

    auth = auth.replaceFirst("[Bb]asic ", "");
    Base64.Decoder decoder = Base64.getDecoder();
    String userColonPass =new String(decoder.decode(auth));
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
      session.setAttribute("uuid",  SubjectHelper.getUniqueId(subject));
      return;
    }
    if(session != null) session.invalidate();
  }

}
