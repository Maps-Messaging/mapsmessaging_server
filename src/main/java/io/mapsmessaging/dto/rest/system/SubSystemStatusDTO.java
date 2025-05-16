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
import lombok.Data;

@Data
@Schema(
    title = "SubSystem Status",
    description = "Represents the status of a subsystem in the messaging server.")
public class SubSystemStatusDTO {

  @Schema(
      title = "Name",
      description = "The name of the subsystem.",
      example = "Messaging Service",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private String name;

  @Schema(
      title = "Comment",
      description = "A comment or additional information about the subsystem's status.",
      example = "System is operating normally.")
  private String comment;

  @Schema(
      title = "Status",
      description = "The current status of the subsystem.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "OK")
  private Status status;
}