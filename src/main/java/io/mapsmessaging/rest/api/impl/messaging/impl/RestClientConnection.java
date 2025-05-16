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

package io.mapsmessaging.rest.api.impl.messaging.impl;

import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.rest.auth.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;

import java.security.Principal;

public class RestClientConnection implements ClientConnection {

  @Getter
  private final String name;

  @Getter
  private final String uniqueName;

  private final String username;

  public RestClientConnection(HttpSession httpSession) {
    name = httpSession.getId();
    uniqueName = "RestClientConnection_" + name;
    String tmp = (String) httpSession.getAttribute("username");
    username = tmp != null ? tmp : name;
  }

  @Override
  public long getTimeOut() {
    return 0;
  }


  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public void sendKeepAlive() {
    // No Op, rest does not allow this
  }

  @Override
  public Principal getPrincipal() {
    return new AuthenticatedUserPrincipal(username);
  }

  @Override
  public String getAuthenticationConfig() {
    return "";
  }

}
