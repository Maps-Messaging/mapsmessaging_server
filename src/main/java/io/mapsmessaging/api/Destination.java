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

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.engine.destination.BaseDestination;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.schema.Schema;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.security.authorisation.ProtectedResource;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Generic destination class
 */
public class Destination implements BaseDestination {

  protected final DestinationImpl destinationImpl;
  protected final SecurityContext securityContext;
  protected final ProtectedResource protectedResource;

  Destination(@NonNull @NotNull DestinationImpl impl, SecurityContext context) {
    destinationImpl = impl;
    this.securityContext = context;
    protectedResource = new ProtectedResource(impl.getResourceType().getName(), impl.getFullyQualifiedNamespace(), null);
  }

  @Override
  public int storeMessage(@NonNull @NotNull Message message) throws IOException {
    if(destinationImpl.getSchema() != null) {
      // Ensure the schema is applied to the incoming message
      String schemaId = destinationImpl.getSchema().getUniqueId();
      if (schemaId.equals(SchemaManager.DEFAULT_RAW_UUID.toString())) {
        try {
          SchemaConfig schemaConfig = SchemaManager.getInstance().locateSchema(destinationImpl.getFullyQualifiedNamespace());
          if (schemaConfig != null && !schemaConfig.getUniqueId().equals(SchemaManager.DEFAULT_RAW_UUID.toString())) {
            destinationImpl.updateSchema(schemaConfig, null);
          }
        }
        catch(Throwable t) {
          t.printStackTrace();
        }
      }
      message.setSchemaId(destinationImpl.getSchema().getUniqueId());
    }
    if(!AuthManager.getInstance().canAccess(securityContext.getIdentity(), ServerPermissions.PUBLISH, protectedResource)) {
      throw new IOException("You don't have permission to publish to this resource");
    }
    return destinationImpl.storeMessage(message);
  }

  @Override
  public String getFullyQualifiedNamespace() {
    return destinationImpl.getFullyQualifiedNamespace();
  }

  public long getStoredMessages() throws IOException {
    return destinationImpl.getStoredMessages();
  }

  public DestinationType getResourceType() {
    return destinationImpl.getResourceType();
  }

  public Message getRetained() throws IOException {
    return destinationImpl.getMessage(destinationImpl.getRetainedIdentifier());
  }

  public Schema getSchema(){
    return destinationImpl.getSchema();
  }


  public void updateSchema(SchemaConfig schemaConfig, Message message) throws IOException {
    destinationImpl.updateSchema(schemaConfig, message);
  }
}
