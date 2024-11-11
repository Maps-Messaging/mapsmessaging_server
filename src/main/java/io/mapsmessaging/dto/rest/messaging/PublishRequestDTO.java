/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging]
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

package io.mapsmessaging.dto.rest.messaging;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Publish Request",
    description = "Represents a request to publish a message to a specified topic with optional retention."
)
public class PublishRequestDTO {

  @NotNull
  @Schema(
      title = "Destination Topic",
      description = "The topic to which the message will be published. This should be a valid topic name recognized by the messaging system.",
      example = "sensor/data"
  )
  private String destinationName;

  @NotNull
  @Schema(
      title = "Message Data",
      description = "The message object containing data, payload, and metadata to be published.",
      nullable = false
  )
  private MessageDTO message;

  @Schema(
      title = "Retain Message",
      description = "Indicates if the message should be retained on the destination. If true, the message will be stored and sent to new subscribers on the topic.",
      example = "false",
      defaultValue = "false"
  )
  private boolean retain = false;
}
