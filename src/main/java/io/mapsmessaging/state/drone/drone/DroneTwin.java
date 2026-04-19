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
import io.mapsmessaging.state.drone.model.EnvironmentalState;
import io.mapsmessaging.state.drone.model.SystemState;
import io.mapsmessaging.state.drone.model.TimeState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

import static io.mapsmessaging.state.drone.util.SyntheticMmsiGenerator.generateSyntheticMmsi;

/**
 * Twin representing an unmanned aircraft or vehicle.
 */
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DroneTwin extends EntityTwin {

  // --- Identity / correlation ---
  private Integer systemId;
  private Integer componentId;
  private Long mmsi;

  private String vehicleClass;
  private String registrationId;
  private String description;
  private VehicleClass vehicleClassType;

  // --- Operational state ---
  private Boolean armed;
  private String flightMode;
  private Boolean failsafe;
  private Boolean gpsValid;

  private String missionState;

  // --- Navigation (emitter-friendly fields) ---
  private Double headingDegrees;
  private Double courseOverGroundDegrees;
  private Double groundSpeedMetersPerSecond;
  private Double verticalSpeedMetersPerSecond;
  private Double climbRateMetersPerSecond;

  // --- Control ---
  private String controllingStationId;
  private Boolean commandLinkActive;

  // --- Extended state ---
  private TimeState timeState;
  private SystemState systemState;
  private EnvironmentalState environmentalState;
  private String landedState;
  private String vtolState;

  private Integer currentMissionSequence;

  private Instant operationalUpdatedAt;

  public DroneTwin(String twinId) {
    setTwinId(twinId);
    setTwinType("DRONE");
    setMmsi(generateSyntheticMmsi(twinId));
  }
}