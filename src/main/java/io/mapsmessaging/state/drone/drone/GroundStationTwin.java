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

import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.model.EnvironmentalState;
import io.mapsmessaging.state.drone.model.SystemState;
import io.mapsmessaging.state.drone.model.TimeState;
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
public class GroundStationTwin extends EntityTwin {

  private String stationId;
  private String stationRole;
  private String operatorName;
  private String siteCode;

  private Boolean controlLinkActive;
  private Double uplinkLatencyMilliseconds;

  private String primaryVehicleId;
  private Set<String> activeVehicleIds = new HashSet<>();

  private TimeState timeState;
  private SystemState systemState;
  private EnvironmentalState environmentalState;

  public GroundStationTwin(String twinId) {
    setTwinId(twinId);
    setTwinType("GROUND_STATION");
  }
}