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

import io.mapsmessaging.state.drone.model.BatteryState;
import io.mapsmessaging.state.drone.model.FixInfo;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.model.LinkState;
import io.mapsmessaging.state.drone.model.Orientation;
import io.mapsmessaging.state.drone.model.VelocityVector;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base protocol-agnostic digital twin aggregate.
 * Grouped freshness fields are intentionally coarse to support multi-protocol updaters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Base protocol-agnostic digital twin aggregate.")
public abstract class EntityTwin {

  @Schema(
      description = "Unique identifier for the twin.",
      example = "drone-001",
      nullable = true
  )
  private String twinId;

  @Schema(
      description = "Hierarchical path of the twin within the model namespace.",
      example = "/fleet/alpha/drone-001",
      nullable = true
  )
  private String twinPath;

  @Schema(
      description = "Type of twin.",
      example = "DRONE",
      nullable = true
  )
  private String twinType;

  @Schema(
      description = "Human-readable display name for the twin.",
      example = "Survey Drone 1",
      nullable = true
  )
  private String displayName;

  @Schema(
      description = "Current geographic position of the twin.",
      nullable = true
  )
  private GeoPosition geoPosition;

  @Schema(
      description = "Home or launch position of the twin.",
      nullable = true
  )
  private GeoPosition homePosition;

  @Schema(
      description = "Current velocity vector of the twin.",
      nullable = true
  )
  private VelocityVector velocityVector;

  @Schema(
      description = "Current orientation of the twin.",
      nullable = true
  )
  private Orientation orientation;

  @Schema(
      description = "Current navigation fix information.",
      nullable = true
  )
  private FixInfo fixInfo;

  @Schema(
      description = "Current battery and power state.",
      nullable = true
  )
  private BatteryState batteryState;

  @Schema(
      description = "Current communications link state.",
      nullable = true
  )
  private LinkState linkState;

  @Schema(
      description = "Lifecycle status of the twin.",
      nullable = false
  )
  private TwinLifecycleStatus lifecycleStatus = TwinLifecycleStatus.ACTIVE;

  @Schema(
      description = "Relationship edges owned by this twin.",
      nullable = false
  )
  private Set<TwinRelationship> relationships = new HashSet<>();

  @Schema(
      description = "Extensible non-protocol metadata attributes associated with the twin.",
      nullable = false
  )
  private Map<String, String> attributes = new ConcurrentHashMap<>();

  @Schema(
      description = "Timestamp when the twin was created.",
      example = "2026-04-20T06:30:00Z",
      nullable = true
  )
  private Instant createdAt;

  @Schema(
      description = "Timestamp when the twin was last observed.",
      example = "2026-04-20T06:35:12Z",
      nullable = true
  )
  private Instant lastSeenAt;

  @Schema(
      description = "Timestamp when identity-related fields were last updated.",
      example = "2026-04-20T06:31:00Z",
      nullable = true
  )
  private Instant identityUpdatedAt;

  @Schema(
      description = "Timestamp when navigation-related fields were last updated.",
      example = "2026-04-20T06:34:10Z",
      nullable = true
  )
  private Instant navigationUpdatedAt;

  @Schema(
      description = "Timestamp when motion-related fields were last updated.",
      example = "2026-04-20T06:34:11Z",
      nullable = true
  )
  private Instant motionUpdatedAt;

  @Schema(
      description = "Timestamp when power-related fields were last updated.",
      example = "2026-04-20T06:33:45Z",
      nullable = true
  )
  private Instant powerUpdatedAt;

  @Schema(
      description = "Timestamp when connectivity-related fields were last updated.",
      example = "2026-04-20T06:34:20Z",
      nullable = true
  )
  private Instant connectivityUpdatedAt;

  @Schema(
      description = "Timestamp when relationship data was last updated.",
      example = "2026-04-20T06:32:00Z",
      nullable = true
  )
  private Instant relationshipsUpdatedAt;

}