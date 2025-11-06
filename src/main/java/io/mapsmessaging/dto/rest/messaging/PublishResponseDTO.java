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

package io.mapsmessaging.dto.rest.messaging;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Publish Response",
    description = "Response from publishing a message, including telemetry and transaction status.")
public class PublishResponseDTO {

  @Schema(
      title = "Message Identifier",
      description = "The unique identifier assigned to the published message.",
      example = "msg-12345")
  private String messageId;

  @Schema(
      title = "Success",
      description = "Indicates whether the publish operation was successful.",
      example = "true")
  private boolean success;

  @Schema(
      title = "Status Message",
      description = "A human-readable status message describing the result.",
      example = "Message published successfully")
  private String message;

  @Schema(
      title = "Publish Time",
      description = "The timestamp when the message was published.",
      example = "2024-01-15T10:30:00")
  private LocalDateTime publishTime;

  @Schema(
      title = "Delivery Latency (ms)",
      description = "The time in milliseconds taken to deliver the message.",
      example = "45")
  private long deliveryLatency;

  @Schema(
      title = "Transaction ID",
      description = "The transaction identifier if the publish was transactional.",
      example = "txn-12345",
      nullable = true)
  private String transactionId;

  @Schema(
      title = "Transaction Status",
      description = "The status of the transaction (e.g., 'COMMITTED', 'PENDING', 'FAILED').",
      example = "COMMITTED",
      nullable = true)
  private String transactionStatus;

  @Schema(
      title = "Error Details",
      description = "Details about any error that occurred during publishing.",
      example = "Topic not found",
      nullable = true)
  private String errorDetails;

}
