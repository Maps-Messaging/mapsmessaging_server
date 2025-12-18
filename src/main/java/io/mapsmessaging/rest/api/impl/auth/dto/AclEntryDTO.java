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

package io.mapsmessaging.rest.api.impl.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a single ACL entry for a principal on a resource")
public class AclEntryDTO {

  @Schema(description = "Type of principal", example = "IDENTITY")
  private PrincipalType principalType;

  @Schema(description = "Principal identifier (user id or group id)", example = "admin")
  private String principalId;

  @Schema(description = "Effect of this ACL entry", example = "ALLOW")
  private AclEffect effect;

  @Schema(description = "List of permission names granted or denied by this entry")
  private List<String> permissions;
}