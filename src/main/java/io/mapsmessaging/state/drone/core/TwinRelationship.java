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

package io.mapsmessaging.state.drone.core;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.Objects;

/**
 * Relationship between twins (e.g. CONTROLS, RELAYS_FOR, PART_OF).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TwinRelationship {

  private String sourceTwinId;
  private String targetTwinId;
  private String relationshipType;
  private boolean bidirectional;
  private Instant updatedAt;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TwinRelationship that)) return false;
    return Objects.equals(sourceTwinId, that.sourceTwinId) &&
        Objects.equals(targetTwinId, that.targetTwinId) &&
        Objects.equals(relationshipType, that.relationshipType);
  }

  public boolean isActive(){
    return bidirectional;
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceTwinId, targetTwinId, relationshipType);
  }
}