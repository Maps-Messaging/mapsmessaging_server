/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.dto.rest.destination;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(
    title = "Destination",
    description =
        "Represents a messaging destination, such as a queue or topic, within the system.",
    requiredProperties = {
      "name",
      "type",
      "storedMessages",
      "delayedMessages",
      "pendingMessages",
      "schemaId"
    })
public class DestinationDTO implements Serializable {

  @Schema(
      title = "Destination Name",
      description =
          "The unique name of the destination, which acts as an identifier within the messaging system.",
      example = "myDestination")
  private final String name;

  @Schema(
      title = "Destination Type",
      description =
          "The type of the destination, indicating whether it is a queue or a topic, for example.",
      example = "queue",
      allowableValues = {"queue", "topic"})
  private final String type;

  @Schema(
      title = "Stored Messages",
      description = "The total count of messages currently stored in the destination.",
      example = "123",
      minimum = "0")
  private final long storedMessages;

  @Schema(
      title = "Delayed Messages",
      description =
          "The number of messages delayed for delivery, which might occur due to timing or prioritization settings.",
      example = "123",
      minimum = "0")
  private final long delayedMessages;

  @Schema(
      title = "Pending Messages",
      description = "The count of messages pending processing in the destination.",
      example = "123",
      minimum = "0")
  private final long pendingMessages;

  @Schema(
      title = "Schema ID",
      description =
          "The identifier for the schema associated with this destination, which may define the structure or rules for messages.",
      example = "schema-123")
  private final String schemaId;
}
