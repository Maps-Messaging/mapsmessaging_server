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

package io.mapsmessaging.rest.data;

import io.mapsmessaging.engine.destination.DestinationImpl;
import lombok.Data;

import java.io.IOException;
import java.io.Serializable;

@Data
public class Destination implements Serializable {

  private final String name;
  private final String type;
  private final long storedMessages;
  private final long delayedMessages;
  private final long pendingMessages;
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
