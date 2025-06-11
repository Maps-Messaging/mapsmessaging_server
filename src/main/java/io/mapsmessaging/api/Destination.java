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
import io.mapsmessaging.engine.destination.BaseDestination;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.schema.Schema;
import io.mapsmessaging.schemas.config.SchemaConfig;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Generic destination class
 */
public class Destination implements BaseDestination {

  protected final DestinationImpl destinationImpl;

  Destination(@NonNull @NotNull DestinationImpl impl) {
    destinationImpl = impl;
  }

  @Override
  public int storeMessage(@NonNull @NotNull Message message) throws IOException {
    if(destinationImpl.getSchema() != null) {
      // Ensure the schema is applied to the incoming message
      message.setSchemaId(destinationImpl.getSchema().getUniqueId());
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
