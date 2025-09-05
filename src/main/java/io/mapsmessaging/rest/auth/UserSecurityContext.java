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

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.security.SubjectHelper;
import jakarta.ws.rs.core.SecurityContext;
import lombok.Getter;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.UUID;

public class UserSecurityContext implements SecurityContext {

  private final AuthenticatedUserPrincipal userPrincipal;
  @Getter
  private final Subject subject;
  @Getter
  private final UUID uuid;

  public UserSecurityContext(String username) {
    userPrincipal = new AuthenticatedUserPrincipal(username);
    subject = AuthManager.getInstance().getUserSubject(username);
    uuid = SubjectHelper.getUniqueId(subject);
  }

  @Override
  public Principal getUserPrincipal() {
    return userPrincipal;
  }

  @Override
  public boolean isUserInRole(String role) {
    return false;
  }

  @Override
  public boolean isSecure() {
    return false;
  }

  @Override
  public String getAuthenticationScheme() {
    return SecurityContext.BASIC_AUTH;
  }
}