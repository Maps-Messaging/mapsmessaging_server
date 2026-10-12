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

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.engine.destination.NatsJetStreamStoreTask;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.schemas.config.SchemaConfig;

import java.io.IOException;

/**
 * Dedicated NATS JetStream publish entry point. Ordinary Destination.storeMessage
 * remains unchanged. Performs the same schema and authorization checks as a
 * normal publish before asking MAPS to return the physically assigned ID.
 */
public final class NatsJetStreamStorage {

  private NatsJetStreamStorage() {
  }

  public static long store(Destination destination, Message message) throws IOException {
    if (destination.destinationImpl.getSchema() != null) {
      String schemaId = destination.destinationImpl.getSchema().getUniqueId();
      if (schemaId.equals(SchemaManager.DEFAULT_RAW_UUID.toString())) {
        try {
          SchemaConfig configured = SchemaManager.getInstance().locateSchema(
              destination.destinationImpl.getFullyQualifiedNamespace());
          if (configured != null && !configured.getUniqueId().equals(SchemaManager.DEFAULT_RAW_UUID.toString())) {
            destination.destinationImpl.updateSchema(configured, null);
          }
        } catch (Throwable failure) {
          // Mirror the ordinary publish path: failing to look up a schema does not bypass auth.
          // The destination schema remains the authoritative source of its identifier.
          io.mapsmessaging.logging.LoggerFactory.getLogger(NatsJetStreamStorage.class)
              .log(ServerLogMessages.DESTINATION_ERROR_SETTING_SCHEMA,
                  destination.destinationImpl.getFullyQualifiedNamespace(), failure);
        }
      }
      message.setSchemaId(destination.destinationImpl.getSchema().getUniqueId());
    }

    ServerPermissions permission = message.isRetain() ? ServerPermissions.RETAIN : ServerPermissions.PUBLISH;
    if (!AuthManager.getInstance().canAccess(
        destination.securityContext.getIdentity(), permission, destination.protectedResource)) {
      throw new IOException("You don't have permission to publish to this resource");
    }
    return NatsJetStreamStoreTask.store(destination.destinationImpl, message);
  }
}
