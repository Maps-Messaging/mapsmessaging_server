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
    title = "Subscription Request",
    description = "Represents a request to create a subscription to a specific destination, with optional filtering and message retention."
)
public class SubscriptionRequestDTO {

  @NotNull
  @Schema(
      title = "Subscription Name",
      description = "A unique name for the subscription.",
      example = "sensorUpdates",
      nullable = false
  )
  private String name;

  @NotNull
  @Schema(
      title = "Destination Name",
      description = "The name of the destination (e.g., topic or queue) to which the subscription is bound.",
      example = "sensor/data",
      nullable = false
  )
  private String destinationName;

  @Schema(
      title = "Named Subscription",
      description = "An optional name for a named subscription, allowing clients to re-use existing subscriptions if provided.",
      example = "temperatureAlerts"
  )
  private String namedSubscription;

  @Schema(
      title = "Filter Expression",
      description = "An optional filter expression written in JMS selector syntax to filter messages received by the subscription.",
      example = "temperature > 25"
  )
  private String filter;

  @Schema(
      title = "Maximum Queue Depth",
      description = "The maximum number of messages that can be queued for the subscription before new messages are dropped.",
      example = "10",
      defaultValue = "1"
  )
  private int maxDepth = 1;

  @Schema(
      title = "Retain Message",
      description = "Indicates if messages should be retained on the destination for this subscription, meaning they will be stored and made available to future subscribers.",
      example = "false",
      defaultValue = "false"
  )
  private boolean retainMessage = false;
}
