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

import io.mapsmessaging.state.drone.model.*;
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
public abstract class EntityTwin {

  private String twinId;
  private String twinPath;
  private String twinType;
  private String displayName;

  private GeoPosition geoPosition;
  private GeoPosition homePosition;
  private VelocityVector velocityVector;
  private Orientation orientation;
  private FixInfo fixInfo;
  private BatteryState batteryState;
  private LinkState linkState;
  private TwinLifecycleStatus lifecycleStatus = TwinLifecycleStatus.ACTIVE;
  /**
   * Relationship edges owned by this twin.
   */
  private Set<TwinRelationship> relationships = new HashSet<>();

  /**
   * Extensible non-protocol metadata.
   */
  private Map<String, String> attributes = new ConcurrentHashMap<>();

  private Instant createdAt;
  private Instant lastSeenAt;

  /**
   * Grouped freshness timestamps.
   */
  private Instant identityUpdatedAt;
  private Instant navigationUpdatedAt;
  private Instant motionUpdatedAt;
  private Instant powerUpdatedAt;
  private Instant connectivityUpdatedAt;
  private Instant relationshipsUpdatedAt;

}