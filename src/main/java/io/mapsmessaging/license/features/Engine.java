/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.license.features;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Core messaging engine feature configuration for the license. All fields are required.")
public class Engine {

  @Schema(
      description = "Enable queue support.",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean queueSupport;

  @Schema(
      description = "Enable topic (pub/sub) support.",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean topicSupport;

  @Schema(
      description = "Enable temporary queue support.",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean tempQueueSupport;

  @Schema(
      description = "Enable temporary topic support.",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean tempTopicSupport;

  @Schema(
      description = "Enable named subscription support.",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean namedSubscriptionSupport;

  @Schema(
      description = "Enable message filtering support.",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean filteringSupport;

  @Schema(
      description = "Enable schema-based validation and routing.",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean schemaSupport;

  @Schema(
      description = "Maximum number of topics allowed.",
      example = "1000000",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private int maxTopics;

  @Schema(
      description = "Maximum number of queues allowed.",
      example = "100000",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private int maxQueues;
}