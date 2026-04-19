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

package io.mapsmessaging.license;

import io.mapsmessaging.license.features.Features;
import lombok.Data;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Detailed license feature definition including expiry and metadata. All fields are required.")
public class FeatureDetails {

  @Schema(
      description = "Licensed feature configuration.",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private Features feature;

  @Schema(
      description = "Expiry date and time for the license.",
      example = "2026-12-31T23:59:59",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private LocalDateTime expiry;

  @Schema(
      description = "Additional information about the license.",
      example = "Enterprise license with full feature set",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private String info;
}