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

package io.mapsmessaging.auth.registry.principal;

import io.mapsmessaging.auth.registry.priviliges.session.SessionPrivileges;

import java.security.Principal;

public class SessionPrivilegePrincipal implements Principal {

  private final SessionPrivileges privileges;

  public SessionPrivilegePrincipal(SessionPrivileges privileges) {
    this.privileges = privileges;
  }

  @Override
  public String getName() {
    return "SessionPrivilegePrincipal";
  }

  @Override
  public String toString() {
    return "Session Privileges : " + privileges.toString();
  }

}
