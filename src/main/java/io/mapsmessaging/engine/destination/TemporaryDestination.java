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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.MessageOverrideDTO;
import io.mapsmessaging.engine.destination.subscription.Subscribable;
import io.mapsmessaging.engine.resources.Resource;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;

public class TemporaryDestination extends DestinationImpl {

  private boolean ownerDisconnected;


  public TemporaryDestination(@NonNull @NotNull String name, @NonNull @NotNull DestinationConfigDTO path, @NonNull @NotNull UUID uuid,
                              @NonNull @NotNull DestinationType destinationType) throws IOException {
    super(name, path, uuid, destinationType);
    ownerDisconnected = false;
  }

  public TemporaryDestination(@NonNull @NotNull String name, @NonNull @NotNull String directory, @NonNull @NotNull Resource resource,
      @NonNull @NotNull DestinationType destinationType, MessageOverrideDTO messageOverrides) throws IOException {
    super(name, directory, resource, destinationType, messageOverrides);
    ownerDisconnected = false;
  }

  public void setOwnerDisconnected() {
    ownerDisconnected = true;
  }

  @Override
  public Subscribable removeSubscription(@NonNull @NotNull String subscriptionId) {
    Subscribable subscribable = super.removeSubscription(subscriptionId);
    checkForDeletion();
    return subscribable;
  }

  public void checkForDeletion() {
    // No longer have subscriptions here, so we have no readers, lets see if the owner is still around
    if (!super.subscriptionManager.hasSubscriptions() && ownerDisconnected ||
        ownerDisconnected && super.getResourceType().isTemporary() && super.getResourceType().isQueue()) {
      MessageDaemon.getInstance().getDestinationManager().delete(this);
    }
  }
}
