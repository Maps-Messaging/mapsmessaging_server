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
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Publish Request",
    description =
        "Represents a request to publish a message to a specified topic with optional retention, headers, and delivery options.")
public class PublishRequestDTO {

  @NotNull
  @Schema(
      title = "Destination Topic",
      description =
          "The topic to which the message will be published. This should be a valid topic name recognized by the messaging system.",
      example = "sensor/data")
  private String destinationName;

  @NotNull
  @Schema(
      title = "Message Data",
      description = "The message object containing data, payload, and metadata to be published.",
      nullable = false)
  private MessageDTO message;

  @Schema(
      title = "Retain Message",
      description =
          "Indicates if the message should be retained on the destination. If true, the message will be stored and sent to new subscribers on the topic.",
      example = "false",
      defaultValue = "false")
  private boolean retain = false;

  @Schema(
      title = "Headers",
      description = "Optional headers/properties to include with the published message.",
      example = "{\"correlationId\": \"123\", \"source\": \"sensor1\"}")
  private Map<String, String> headers;

  @Schema(
      title = "Delivery Options",
      description = "Optional delivery configuration options for the message.",
      example = "{\"timeout\": \"5000\", \"retryCount\": \"3\"}")
  private Map<String, String> deliveryOptions;

  @Schema(
      title = "Session Name",
      description = "Optional named session identifier for transactional publish operations.",
      example = "session1",
      nullable = true)
  private String sessionName;
}
