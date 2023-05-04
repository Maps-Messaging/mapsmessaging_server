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
import java.io.IOException;
import java.security.Principal;

public class SaslSecurityContext extends SecurityContext {
  private final Logger logger = LoggerFactory.getLogger(SaslSecurityContext.class);

  public SaslSecurityContext(String username, Principal endPointPrincipal) {
    super(username);
    subject = buildSubject(username, endPointPrincipal);
    isLoggedIn = true;
  }

  @Override
  public void login() throws IOException {
    logger.log(AuditEvent.SUCCESSFUL_LOGIN, subject);
  }

  @Override
  public void logout() {
    isLoggedIn = false;
  }
}
