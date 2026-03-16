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

package io.mapsmessaging.dto.rest.config.aggregator;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "Aggregator Window Close Mode",
    description = "Defines how an aggregation window is closed."
)
public enum WindowCloseMode {

  @Schema(
      description = "Close the aggregation window only when all configured inputs have received at least one event."
  )
  ALL_INPUTS,

  @Schema(
      description = "Close the aggregation window only when the configured timeout expires, regardless of which inputs have arrived."
  )
  TIMEOUT_ONLY,

  @Schema(
      description = "Close the aggregation window when all configured inputs have arrived, or when the timeout expires, whichever happens first."
  )
  ALL_INPUTS_OR_TIMEOUT
}