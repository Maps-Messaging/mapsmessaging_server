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

import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "License feature configuration. All fields are required.")
public class Features {

  @Schema(
      description = "Name of the licensed feature set.",
      example = "Enterprise",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private String name;

  @Schema(
      description = "Indicates if machine learning features are enabled.",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean ml;

  @Schema(
      description = "If true, explicitly overrides default feature configuration.",
      example = "false",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean overrideFeatures;

  @Schema(
      description = "Network feature configuration.",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private Network network;

  @Schema(
      description = "Supported protocol configuration.",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private Protocols protocols;

  @Schema(
      description = "Management and control feature configuration.",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private Management management;

  @Schema(
      description = "Interconnection capabilities between systems.",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private InterConnections interConnections;

  @Schema(
      description = "Storage feature configuration.",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private Storage storage;

  @Schema(
      description = "Hardware integration capabilities.",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private Hardware hardware;

  @Schema(
      description = "Core messaging engine configuration.",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private Engine engine;
}