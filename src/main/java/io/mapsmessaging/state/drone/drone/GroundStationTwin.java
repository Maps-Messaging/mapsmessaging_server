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

package io.mapsmessaging.state.drone.drone;

import io.mapsmessaging.dto.rest.config.protocol.impl.VehicleClass;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.model.EnvironmentalState;
import io.mapsmessaging.state.drone.model.SystemState;
import io.mapsmessaging.state.drone.model.TimeState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * Twin representing a ground control station or operator node.
 */
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Digital twin representing a ground control station or operator node.")
public class GroundStationTwin extends EntityTwin {

  @Schema(
      description = "Unique identifier of the ground station.",
      example = "gcs-01",
      nullable = true
  )
  private String stationId;

  @Schema(
      description = "Role of the station (e.g. PRIMARY, SECONDARY, RELAY).",
      example = "PRIMARY",
      nullable = true
  )
  private String stationRole;

  @Schema(
      description = "Name of the operator or controlling entity.",
      example = "Operator A",
      nullable = true
  )
  private String operatorName;

  @Schema(
      description = "Site or location code of the ground station.",
      example = "SYD-BASE-01",
      nullable = true
  )
  private String siteCode;

  @Schema(
      description = "Indicates whether the control link to vehicles is active.",
      example = "true",
      nullable = true
  )
  private Boolean controlLinkActive;

  @Schema(
      description = "Measured uplink latency in milliseconds.",
      example = "35.5",
      nullable = true
  )
  private Double uplinkLatencyMilliseconds;

  @Schema(
      description = "Identifier of the primary vehicle currently controlled.",
      example = "drone-001",
      nullable = true
  )
  private String primaryVehicleId;

  @Schema(
      description = "Set of vehicle identifiers currently active or managed by this station.",
      nullable = false
  )
  private Set<String> activeVehicleIds = new HashSet<>();

  @Schema(
      description = "Time synchronization state of the station.",
      nullable = true
  )
  private TimeState timeState;

  @Schema(
      description = "System health and performance state of the station.",
      nullable = true
  )
  private SystemState systemState;

  @Schema(
      description = "Environmental conditions at the station location.",
      nullable = true
  )
  private EnvironmentalState environmentalState;

  public GroundStationTwin(String twinId) {
    super(twinId);
    setTwinType(TwinType.GROUND_CONTROL);
    setVehicleClass(VehicleClass.GCS);
  }
}