/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.NatsStreamMembership;

import java.io.IOException;

/** NATS-specific logical stream administration; never deletes a MAPS resource. */
public final class NatsJetStreamDestinations {

  private NatsJetStreamDestinations() {
  }

  public static void enable(Destination destination) throws IOException {
    check(destination, ServerPermissions.SUBSCRIBE);
    NatsStreamMembership.setEnabled(destination.destinationImpl, true);
    NamespaceManager.getInstance().created(destination.destinationImpl);
  }

  public static void checkRemoval(Destination destination) throws IOException {
    check(destination, ServerPermissions.DELETE);
  }

  public static void disable(Destination destination) throws IOException {
    checkRemoval(destination);
    NatsStreamMembership.setEnabled(destination.destinationImpl, false);
    NamespaceManager.getInstance().deleted(destination.destinationImpl);
  }

  private static void check(Destination destination, ServerPermissions permission) throws IOException {
    if (!AuthManager.getInstance().canAccess(destination.securityContext.getIdentity(),
        permission, destination.protectedResource)) {
      throw new IOException("You don't have permission to change JetStream membership for this resource");
    }
  }
}
