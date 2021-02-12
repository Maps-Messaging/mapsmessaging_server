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

package org.maps.messaging.engine.destination;

import java.io.IOException;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.maps.messaging.MessageDaemon;
import org.maps.messaging.api.features.DestinationType;
import org.maps.messaging.engine.destination.subscription.Subscribable;
import org.maps.messaging.engine.resources.Resource;

public class TemporaryDestination extends DestinationImpl {

  private boolean ownerDisconnected;


  public TemporaryDestination(@NotNull String name, @NotNull String path, @NotNull UUID uuid,
      @NotNull DestinationType destinationType) throws IOException {
    super(name, path, uuid, destinationType);
    ownerDisconnected = false;
  }

  public TemporaryDestination(@NotNull Resource resource,
      @NotNull DestinationType destinationType) throws IOException {
    super(resource, destinationType);
    ownerDisconnected = false;
  }

  public TemporaryDestination(@NotNull String name, @NotNull DestinationType destinationType) {
    super(name, destinationType);
    ownerDisconnected = false;
  }

  public void setOwnerDisconnected(){
    ownerDisconnected = true;
  }

  @Override
  public Subscribable removeSubscription( @NotNull String subscriptionId) {
    Subscribable subscribable = super.removeSubscription(subscriptionId);
    checkForDeletion();
    return subscribable;
  }

  public void checkForDeletion(){
    // No longer have subscriptions here, so we have no readers, lets see if the owner is still around
    if(!super.subscriptionManager.hasSubscriptions() && ownerDisconnected){
      MessageDaemon.getInstance().getDestinationManager().delete(this);
    }
  }
}
