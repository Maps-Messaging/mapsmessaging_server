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
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Destination Status",
    description =
        "Provides statistics and operational metrics about a specific messaging destination, such as message counts and average processing times.")
public class DestinationStatusDTO implements Serializable {

  @Schema(
      title = "Destination Name",
      description = "The unique name of the destination within the messaging system.",
      example = "myDestination",
      nullable = false)
  private String name;

  @Schema(
      title = "Stored Messages",
      description = "The number of messages currently stored in the destination.",
      example = "150",
      minimum = "0")
  private long storedMessages;

  @Schema(
      title = "Delayed Messages",
      description = "The count of messages that are delayed in processing or delivery.",
      example = "20",
      minimum = "0")
  private long delayedMessages;

  @Schema(
      title = "Pending Transactions",
      description = "The number of messages pending transactional confirmation.",
      example = "45",
      minimum = "0")
  private long pendingTransactions;

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
