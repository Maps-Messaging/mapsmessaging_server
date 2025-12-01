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

package io.mapsmessaging.api;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.auth.CreationAuthorisationCheck;
import io.mapsmessaging.api.auth.SubscriptionAuthorisationCheck;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.security.access.Identity;
import io.mapsmessaging.security.authorisation.ProtectedResource;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class AuthSession extends Session {

  private final Identity identity;

  AuthSession(@NonNull @NotNull SessionImpl session, @NonNull @NotNull MessageListener listener) {
    super(session, listener);
    identity = session.getSecurityContext().getIdentity();
  }

  @Override
  public SubscribedEventManager addSubscription(@NonNull @NotNull SubscriptionContext context) throws IOException {
    try {
      // Check to see if wild card subscription is allowed IF it is a wildcard subscription
      ProtectedResource serverResource  = new ProtectedResource("server", MessageDaemon.getInstance().getId(), null);
      if(context.containsWildcard() && !AuthManager.getInstance().canAccess(identity, ServerPermissions.WILD_CARD_SUBSCRIBE ,serverResource)){
        throw new IOException("Access denied");
      }
      context.setAuthCheck(new SubscriptionAuthorisationCheck(identity, context));
      return super.addSubscription(context);
    } catch (IOException e) {
      throw e;
    }
  }

  @Override
  public CompletableFuture<Destination> findDestination(@NonNull @NotNull String destinationName, @NonNull @NotNull DestinationType type) {
    return internalFindDestination(destinationName, type, new CreationAuthorisationCheck(identity));
  }


  @Override
  public CompletableFuture<Void> deleteDestinationImpl(DestinationImpl destination)  {
    ProtectedResource resource  = new ProtectedResource(destination.getResourceType().getName(), destination.getFullyQualifiedNamespace(), null);
    if(!AuthManager.getInstance().canAccess(identity, ServerPermissions.DELETE ,resource)){
      CompletableFuture<Void> failure = new CompletableFuture<>();
      failure.completeExceptionally(new IOException("Access denied"));
      return failure;
    }
    return super.deleteDestinationImpl(destination);
  }


}
