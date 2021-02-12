/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.messaging.engine.session;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;
import org.maps.logging.LogMessages;

public class AnonymousSecurityContext extends SecurityContext {

  private final Subject subject;

  public AnonymousSecurityContext() {
    super("anonymous", null);
    Set<Principal> principalSet = new HashSet<>();
    Set<String> credentials = new HashSet<>();
    Set<String> privileges = new HashSet<>();

    subject = new Subject(true, principalSet, credentials, privileges);
  }

  @Override
  public Subject getSubject() {
    return subject;
  }

  @Override
  public boolean isLoggedIn() {
    return isLoggedIn;
  }

  @Override
  public void login() {
    logger.log(LogMessages.ANONYMOUS_SECURITY_LOG_IN, username);
    isLoggedIn = true;
  }

  @Override
  public void logout() {
    logger.log(LogMessages.ANONYMOUS_SECURITY_LOG_OFF, username);
    isLoggedIn = false;
  }
}
