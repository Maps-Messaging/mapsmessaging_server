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

package io.mapsmessaging.api.auth;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.security.access.Identity;
import io.mapsmessaging.security.authorisation.AuthRequest;
import io.mapsmessaging.security.authorisation.ProtectedResource;

import java.util.ArrayList;
import java.util.List;

public class PublishAuthorisationCheck implements DestinationAuthorisationCheck {

  private final Identity identity;

  public PublishAuthorisationCheck(Identity identity) {
    this.identity = identity;
  }

  @Override
  public boolean check(String name, DestinationType type, boolean create) {
    List<AuthRequest> authRequests = new ArrayList<>();
    if(create) {
      authRequests.add(new AuthRequest  (identity, ServerPermissions.CREATE_DESTINATION, buildResource(name, type)));
    }
    authRequests.add(new AuthRequest(identity, ServerPermissions.PUBLISH, buildResource(name, type)));
    return AuthManager.getInstance().hasAllAccess(authRequests);
  }

  private ProtectedResource buildResource(String name, DestinationType type){
    return new ProtectedResource(type.getName(), name, null);
  }
}