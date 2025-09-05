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

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@Schema(
    title = "Group",
    description =
        "Represents a group of users within the system, identified by a unique name and ID.")
public class GroupDTO {

  @Schema(
      title = "Group Name",
      description = "The name of the group, such as an administrative or user-defined role.",
      example = "admin",
      nullable = false)
  private final String name;

  @Schema(
      title = "Group Unique ID",
      description = "The unique identifier for the group, generated as a UUID.",
      example = "e808afcb-1ff9-46cd-a322-3119dbf1d071",
      nullable = false)
  private final UUID uniqueId;

  @Schema(
      title = "Group Members",
      description = "A list of unique IDs representing the members of this group.",
      type = "array",
      example =
          "[\"83db8741-57ca-4147-a973-49789d9150bb\",\"32708878-2eba-4dec-b5f5-94e63fb45c0d\",\"e59b1a11-73d7-4962-b3be-65715d99b172\"]",
      nullable = true)
  private final List<UUID> usersList;
}
