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

package io.mapsmessaging.dto.rest.system;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "Status Enum",
    description = "Enumeration of possible statuses for a subsystem."
)
public enum Status {
  @Schema(description = "The subsystem is operating normally.", example = "OK")
  OK,

  @Schema(description = "The subsystem is stopped.", example = "STOPPED")
  STOPPED,

  @Schema(description = "The subsystem is paused.", example = "PAUSED")
  PAUSED,

  @Schema(description = "The subsystem is disabled.", example = "DISABLED")
  DISABLED,

  @Schema(description = "The subsystem has a warning status.", example = "WARN")
  WARN,

  @Schema(description = "The subsystem is in an error state.", example = "ERROR")
  ERROR
}