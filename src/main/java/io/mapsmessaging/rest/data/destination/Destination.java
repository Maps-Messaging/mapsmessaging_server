/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.rest.data.destination;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.IOException;
import java.io.Serializable;

@Data
public class Destination implements Serializable {

  @Schema(description = "The name of the destination", example = "myDestination")
  private final String name;
  @Schema(description = "The type of the destination", example = "queue")
  private final String type;
  @Schema(description = "The number of messages stored in the destination", example = "123")
  private final long storedMessages;
  @Schema(description = "The number of messages delayed in the destination", example = "123")
  private final long delayedMessages;
  @Schema(description = "The number of messages pending in the destination", example = "123")
  private final long pendingMessages;
  @Schema(description = "The schema id of the destination", example = "123")
  private final String schemaId;

  public Destination(DestinationImpl destinationImpl) throws IOException {
    this.name = destinationImpl.getFullyQualifiedNamespace();
    storedMessages = destinationImpl.getStoredMessages();
    type = destinationImpl.getResourceType().getName();
    schemaId = destinationImpl.getSchema().getUniqueId();
    delayedMessages = destinationImpl.getDelayedMessages();
    pendingMessages = destinationImpl.getPendingTransactions();
  }
}
