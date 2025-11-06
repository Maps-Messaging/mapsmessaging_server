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

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Message",
    description =
        "Represents a messaging entity with configurable quality, priority, and metadata attributes.")
public class MessageDTO {
  @Schema(
      title = "Message Identifier",
      description = "The event identifier")
  private long identifier;

  @NotNull
  @Schema(
      title = "Payload",
      description = "The main payload content of the message, represented as a byte64 string.",
      example = "VGhpcyBpcyBhIGV4YW1wbGUgZGF0YS4="
      )
  private String payload;

  @Schema(
      title = "Content Type",
      description = "The MIME type of the message payload, indicating its format.",
      example = "application/json")
  private String contentType;

  @Schema(
      title = "Correlation Data",
      description = "Additional data used for correlating messages, provided as a byte array.",
      example = "[1, 2, 3, 4]")
  private byte[] correlationData;

  @Schema(
      title = "Expiry Time",
      description =
          "The expiry time for the message in milliseconds. Default is -1, indicating no expiry.",
      example = "60000",
      defaultValue = "-1")
  private long expiry = -1;

  @Schema(
      title = "Priority",
      description =
          "The priority level of the message, ranging from 0 (lowest) to 10 (highest). Default is 4 (normal).",
      example = "4",
      defaultValue = "4")
  private int priority = Priority.NORMAL.getValue();

  @Schema(
      title = "Quality of Service",
      description =
          "The Quality of Service level for the message: 0 (at most once), 1 (at least once), or 2 (exactly once).",
      example = "1",
      defaultValue = "0")
  private int qualityOfService = QualityOfService.AT_MOST_ONCE.getLevel();

  @Schema(
      title = "Creation Date/Time",
      description = "The time the server received this event")
  private LocalDateTime creation;

  @Schema(
      title = "Message Parameters",
      description = "A map containing optional key-value pairs associated with the message.",
      example = "{\"key1\": \"value1\", \"key2\": 42}")
  private Map<String, Object> dataMap;

  @Schema(
      title = "Event Meta Data",
      description = "A map of string, string values that the server has added to the event as it was processed",
      example = "{\"key1\": \"value1\", \"key2\": 42}")
  private Map<String, String> metaData;

  @Schema(
      title = "Headers",
      description = "A map of headers/properties associated with the message.",
      example = "{\"correlationId\": \"123\", \"source\": \"sensor1\"}")
  private Map<String, String> headers;

  @Schema(
      title = "Transaction Id",
      description = "The transaction identifier if this message is part of a transaction.",
      example = "txn-12345",
      nullable = true)
  private String transactionId;

  @Schema(
      title = "Delivery Status",
      description = "The delivery status of the message (e.g., 'PENDING', 'DELIVERED', 'FAILED').",
      example = "DELIVERED",
      nullable = true)
  private String deliveryStatus;

}
