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

package io.mapsmessaging.rest.data.messaging;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SubscriptionRequest {
  @NotNull
  @Schema(description = "Name of the subscription")
  private String name;

  @NotNull
  @Schema(description = "Name of the destination (topic or queue) for subscription")
  private String destinationName;

  @Schema(description = "Name of the named subscription, if any")
  private String namedSubscription;

  @Schema(description = "Filter expression using JMS selector syntax")
  private String filter; // JMS selector syntax filter

  @Schema(description = "Maximum depth of messages queued for the subscription", defaultValue = "1")
  private int maxDepth = 1; // Default value set here

  @Schema(description = "Should the message be retained on the destination", defaultValue = "false")
  private boolean retainMessage = false;

}
