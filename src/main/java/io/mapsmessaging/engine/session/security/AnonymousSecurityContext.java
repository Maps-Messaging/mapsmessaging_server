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

package io.mapsmessaging.engine.session.security;

import io.mapsmessaging.engine.audit.AuditEvent;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;

public class AnonymousSecurityContext extends SecurityContext {
  final Logger logger = LoggerFactory.getLogger(AnonymousSecurityContext.class);

  public AnonymousSecurityContext() {
    super("Anonymous");
  }

  @Override
  public String getUsername() {
    return username;
  }
  @Override
  public void login() {
    logger.log(AuditEvent.SUCCESSFUL_LOGIN, username);
    isLoggedIn = true;
    Set<Principal> principalSet = new HashSet<>();
    Set<String> credentials = new HashSet<>();
    Set<String> privileges = new HashSet<>();
    subject = new Subject(true, principalSet, credentials, privileges);
  }

  @Override
  public void logout() {
    logger.log(AuditEvent.SUCCESSFUL_LOGOUT, username);
    isLoggedIn = false;
  }
}
