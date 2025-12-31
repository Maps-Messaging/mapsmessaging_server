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

package io.mapsmessaging.dto.rest.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;
@Data
@AllArgsConstructor
@Schema(
    title = "GroupInfo",
    description =
        "Group information only, no user lists")
public class GroupInfoDTO {

  @Schema(
      title = "Group Name",
      description = "The name of the group, such as an administrative or user-defined role.",
      example = "admin",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false)
  private final String name;

  @Schema(
      title = "Group Unique ID",
      description = "The unique identifier for the group, generated as a UUID.",
      example = "e808afcb-1ff9-46cd-a322-3119dbf1d071",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false)
  private final UUID uniqueId;
}
