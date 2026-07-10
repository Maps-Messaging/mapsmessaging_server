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

package io.mapsmessaging.state.drone.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@Schema(description = "Model-interpreted detection/contact event produced from vehicle telemetry.")
public class DetectionEvent {

  @Schema(description = "Stable contact identifier used to add, update, or remove a contact.", nullable = true)
  private UUID contactId;

  @Schema(description = "Human-readable detection or contact name.", example = "target", nullable = true)
  private String name;

  @Schema(description = "Detection event type.", example = "DETECTED", nullable = true)
  private DetectionEventType eventType;

  @Schema(description = "Contact or detection position, if known.", nullable = true)
  private GeoPosition position;

  @Schema(description = "Contact time-to-live in milliseconds.", example = "60000", nullable = true)
  private Long ttlMillis;

  @Schema(description = "Timestamp when the detection event was interpreted or received.", example = "2026-07-06T00:00:00Z", nullable = true)
  private Instant timestamp;

  @Schema(description = "Model or protocol-specific detection attributes.", nullable = true)
  private Map<String, Object> attributes = new HashMap<>();

  public DetectionEvent(UUID contactId, String name, DetectionEventType eventType) {
    this.contactId = contactId;
    this.name = name;
    this.eventType = eventType;
  }

  public void addAttribute(String name, Object value) {
    if (name != null && value != null) {
      attributes.put(name, value);
    }
  }

  public boolean isDetectedOrUpdated() {
    return eventType == DetectionEventType.DETECTED || eventType == DetectionEventType.UPDATED;
  }

  public boolean isLost() {
    return eventType == DetectionEventType.LOST;
  }
}