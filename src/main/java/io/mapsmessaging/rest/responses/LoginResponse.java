/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.rest.responses;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.rest.auth.AuthenticationContext;
import io.mapsmessaging.rest.auth.RestAccessControl;
import io.mapsmessaging.security.access.Identity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.security.auth.Subject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginResponse {

  private String status;
  private String username;
  private Map<String, String> accessMap;
  private UUID uniqueId;

  public LoginResponse(String status){
    this.status = status;
    accessMap = new HashMap<>();
  }

  public LoginResponse(String status, Subject subject, String username){
    this.status = status;
    if(username != null && AuthManager.getInstance().isAuthorisationEnabled()) {
      Identity userIdMap = AuthManager.getInstance().getUserIdentity(username);
      RestAccessControl accessControl = AuthenticationContext.getInstance().getAccessControl();

      this.username = username;
      uniqueId = userIdMap.getId();
      accessMap = accessControl.getAccess(subject);
    }
  }
}
