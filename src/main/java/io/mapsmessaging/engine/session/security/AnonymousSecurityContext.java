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

package io.mapsmessaging.engine.session.security;

import io.mapsmessaging.engine.audit.AuditEvent;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.security.Principal;

public class AnonymousSecurityContext extends SecurityContext {

  final Logger logger = LoggerFactory.getLogger(AnonymousSecurityContext.class);

  private final Principal endPointPrincipal;
  public AnonymousSecurityContext(Principal endPointPrincipal) {
    super("anonymous");
    this.endPointPrincipal = endPointPrincipal;
  }

  @Override
  public String getUsername() {
    return username;
  }
  @Override
  public void login() {
    subject = buildSubject(username, endPointPrincipal);
    logger.log(AuditEvent.SUCCESSFUL_LOGIN, username);
    isLoggedIn = true;
  }

  @Override
  public void logout() {
    logger.log(AuditEvent.SUCCESSFUL_LOGOUT, username);
    isLoggedIn = false;
  }
}
