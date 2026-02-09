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

package io.mapsmessaging.rest.api.impl.destination.context;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    name = "DestinationEntry",
    description = "A single immediate child under a prefix. Can represent a folder or a destination.",
    requiredMode = Schema.RequiredMode.REQUIRED,
    nullable = false
)
public class Entry {

  @Schema(
      description = "Child name (single segment).",
      example = "fred",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false

  )
  private String name;

  @Schema(
      description = "Fully qualified path for this entry.",
      example = "/a/b/fred",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  private String fullPath;

  @Schema(
      description = "Destination type when kind is DESTINATION; null when kind is FOLDER.",
      example = "TOPIC",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private Type destinationType;

  @Schema(
      description = "For folders: The number of children within, for destinations will always be 0",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private int childCount;

}
