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
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@Schema(
    title = "User",
    description =
        "Represents a user within the system, including username, unique ID, group memberships, and user-specific attributes.")
public class UserDTO {

  @Schema(
      title = "Username",
      description = "The unique name assigned to the user.",
      example = "myUserName",
      nullable = false)
  private final String username;

  @Schema(
      title = "User Unique ID",
      description =
          "The UUID representing this specific user, ensuring unique identification across the system.",
      example = "83db8741-57ca-4147-a973-49789d9150bb",
      nullable = false)
  private final UUID uniqueId;

  @Schema(
      title = "User Group Memberships",
      description =
          "A list of group names to which the user belongs, providing role-based access and permissions.",
      type = "array",
      example = "[\"admin\", \"everyone\"]",
      nullable = true)
  private final List<String> groupList;

  @Schema(
      title = "User Attributes",
      description =
          "A map of user-specific attributes, such as home directory or other key-value pairs for configuration.",
      example = "{\"homeDir\": \"/home/user1\", \"shell\": \"/bin/bash\"}",
      nullable = true)
  private final Map<String, String> attributes;
}
