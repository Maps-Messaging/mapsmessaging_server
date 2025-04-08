/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

import com.sun.security.auth.UserPrincipal;
import io.mapsmessaging.security.uuid.UuidGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

import javax.security.auth.Subject;
import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

public class NoAuthenticationFilter extends BaseAuthenticationFilter {

  @Context
  private HttpServletRequest httpRequest;

  @Override
  public void processAuthentication(ContainerRequestContext containerRequest) throws IOException {
    HttpSession session = httpRequest.getSession(true);
    if (session.isNew()) {
      Set<Principal> principals = new HashSet<>();
      principals.add(new UserPrincipal("anonymous"));
      Subject subject = new Subject(true, principals, new HashSet<>(), new HashSet<>());
      setupSession(httpRequest, "anonymous", UuidGenerator.getInstance().generate(), subject);
    }
  }
}
