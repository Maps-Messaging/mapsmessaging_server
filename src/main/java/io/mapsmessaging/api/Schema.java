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

import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class Schema extends Destination {

  Schema(@NonNull @NotNull DestinationImpl impl, SecurityContext context) {
    super(impl, context);
  }


  @Override
  public int storeMessage(@NonNull @NotNull Message message) throws IOException {
    // No we don't store events we need to parse the message to change this destinations schema
    try {
      SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(message.getOpaqueData());
      destinationImpl.updateSchema(config, message);
    } catch (Exception e) {
      throw new IOException("Invalid schema format");
    }
    return 1;
  }

  @Override
  public long getStoredMessages() throws IOException {
    return 1; // We only have 1 schema per destination
  }

  @Override
  public String getFullyQualifiedNamespace() {
    return DestinationMode.SCHEMA.getNamespace() + super.getFullyQualifiedNamespace();
  }
}