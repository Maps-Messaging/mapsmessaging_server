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

package io.mapsmessaging.dto.rest.destination;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
  private String name;

  @Schema(
      title = "Destination Type",
      description =
          "The type of the destination, indicating whether it is a queue or a topic, for example.",
      example = "queue",
      allowableValues = {"queue", "topic"})
  private String type;

  @Schema(
      title = "Stored Messages",
      description = "The total count of messages currently stored in the destination.",
      example = "123",
      minimum = "0")
  private long storedMessages;

  @Schema(
      title = "Delayed Messages",
      description =
          "The number of messages delayed for delivery, which might occur due to timing or prioritization settings.",
      example = "123",
      minimum = "0")
  private long delayedMessages;

  @Schema(
      title = "Pending Messages",
      description = "The count of messages pending processing in the destination.",
      example = "123",
      minimum = "0")
  private long pendingMessages;

  @Schema(
      title = "Schema ID",
      description =
          "The identifier for the schema associated with this destination, which may define the structure or rules for messages.",
      example = "schema-123")
  private String schemaId;

  @Schema(
      title = "No Interest Messages",
      description = "The count of messages dropped due to lack of interest by consumers.",
      example = "5",
      minimum = "0")
  private long noInterestMessages;

  @Schema(
      title = "Published Messages",
      description = "Total count of messages published to this destination.",
      example = "1000",
      minimum = "0")
  private long publishedMessages;

  @Schema(
      title = "Retrieved Messages",
      description = "The total number of messages retrieved from the destination by consumers.",
      example = "980",
      minimum = "0")
  private long retrievedMessages;

  @Schema(
      title = "Expired Messages",
      description = "The count of messages that expired before being delivered.",
      example = "10",
      minimum = "0")
  private long expiredMessages;

  @Schema(
      title = "Delivered Messages",
      description = "The number of messages successfully delivered to consumers.",
      example = "970",
      minimum = "0")
  private long deliveredMessages;

  @Schema(
      title = "Average Read Time",
      description = "The average time, in nanoseconds, to read messages from the store.",
      example = "1500",
      minimum = "0")
  private long readTimeAveNs;

  @Schema(
      title = "Average Write Time",
      description = "The average time, in nanoseconds, to write messages to the store.",
      example = "2000",
      minimum = "0")
  private long writeTimeAveNs;

  @Schema(
      title = "Average Delete Time",
      description = "The average time, in nanoseconds, to delete messages from the store.",
      example = "1200",
      minimum = "0")
  private long deleteTimeAveNs;
}
