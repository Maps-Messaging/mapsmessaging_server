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

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.security.access.Identity;
import io.mapsmessaging.security.authorisation.AuthRequest;
import io.mapsmessaging.security.authorisation.ProtectedResource;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionAuthorisationCheck implements DestinationAuthorisationCheck {

  private final Identity identity;
  private final SubscriptionContext subscriptionContext;

  public SubscriptionAuthorisationCheck(Identity identity, SubscriptionContext subscriptionContext) {
    this.identity = identity;
    this.subscriptionContext = subscriptionContext;
  }

  @Override
  public boolean check(String name, DestinationType type, boolean create) {
    try {
      if(identity == null){
        return !AuthManager.getInstance().isAuthorisationEnabled();
      }
      List<AuthRequest> authRequests = new ArrayList<>();
      if (create) {
        authRequests.add(new AuthRequest(identity, ServerPermissions.CREATE_DESTINATION, new ProtectedResource("Server", MessageDaemon.getInstance().getId(), null)));
      }
      boolean isSchema = (subscriptionContext.getDestinationMode() == DestinationMode.SCHEMA);
      ProtectedResource destinationResource = buildResource(name, type);
      // Now check the type of subscription with the resource requested
      if (isSchema) {
        authRequests.add(new AuthRequest(identity, ServerPermissions.SCHEMA_SUBSCRIBE, destinationResource));
      } else {
        if (subscriptionContext.isBrowser()) {
          authRequests.add(new AuthRequest(identity, ServerPermissions.VIEW, destinationResource));
        }
        else if(subscriptionContext.isSharedSubscription()) {
          authRequests.add(new AuthRequest(identity, ServerPermissions.DURABLE, destinationResource));
        }
        else {
          authRequests.add(new AuthRequest(identity, ServerPermissions.SUBSCRIBE, destinationResource));
        }
      }
      return AuthManager.getInstance().hasAllAccess(authRequests);
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
    return false;
  }

  private ProtectedResource buildResource(String name, DestinationType type){
    return new ProtectedResource(type.getName(), name, null);
  }
}
