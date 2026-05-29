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
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.model.autopilot.AutopilotState;
import io.mapsmessaging.state.drone.model.EnvironmentalState;
import io.mapsmessaging.state.drone.model.SystemState;
import io.mapsmessaging.state.drone.model.TimeState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

import static io.mapsmessaging.state.drone.util.SyntheticMmsiGenerator.generateSyntheticMmsi;

/**
 * Twin representing an unmanned aircraft or vehicle.
 */
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Digital state model representing a drone or unmanned vehicle.")
public class DroneTwin extends EntityTwin {

  @Schema(
      description = "MAVLink or protocol-specific system identifier.",
      example = "1",
      nullable = true
  )
  private Integer systemId;

  @Schema(
      description = "MAVLink or protocol-specific component identifier.",
      example = "1",
      nullable = true
  )
  private Integer componentId;

  @Schema(
      description = "Synthetic or assigned MMSI used for external maritime-style identity mapping.",
      example = "999001234",
      nullable = true
  )
  private Long mmsi;

  @Schema(
      description = "Registration or tail identifier of the vehicle.",
      example = "VH-DRN-01",
      nullable = true
  )
  private String registrationId;

  @Schema(
      description = "Human-readable description of the drone or vehicle.",
      example = "Primary survey drone",
      nullable = true
  )
  private String description;

  @Schema(
      description = "Short 7 char string used for the call sign of the vessel.",
      example = "drone01",
      nullable = true
  )
  private String callSign;


  @Schema(
      description = "Decoded autopilot information for the vehicle.",
      nullable = true
  )
  private AutopilotState autopilotState;


  @Schema(
      description = "Indicates whether the vehicle is armed.",
      example = "true",
      nullable = true
  )
  private Boolean armed;

  @Schema(
      description = "Current flight mode reported by the vehicle.",
      example = "AUTO",
      nullable = true
  )
  private String flightMode;

  @Schema(
      description = "Indicates whether the vehicle is in a failsafe state.",
      example = "false",
      nullable = true
  )
  private Boolean failsafe;

  @Schema(
      description = "Indicates whether GPS is currently valid and usable.",
      example = "true",
      nullable = true
  )
  private Boolean gpsValid;

  @Schema(
      description = "Current mission execution state.",
      example = "ACTIVE",
      nullable = true
  )
  private String missionState;

  @Schema(
      description = "Vehicle heading in degrees.",
      example = "182.4",
      nullable = true
  )
  private Double headingDegrees;

  @Schema(
      description = "Course over ground in degrees.",
      example = "180.0",
      nullable = true
  )
  private Double courseOverGroundDegrees;

  @Schema(
      description = "Ground speed in meters per second.",
      example = "14.8",
      nullable = true
  )
  private Double groundSpeedMetersPerSecond;

  @Schema(
      description = "Vertical speed in meters per second.",
      example = "-0.6",
      nullable = true
  )
  private Double verticalSpeedMetersPerSecond;

  @Schema(
      description = "Climb rate in meters per second.",
      example = "1.2",
      nullable = true
  )
  private Double climbRateMetersPerSecond;

  @Schema(
      description = "Identifier of the controlling station or operator endpoint.",
      example = "gcs-01",
      nullable = true
  )
  private String controllingStationId;

  @Schema(
      description = "Indicates whether the command and control link is active.",
      example = "true",
      nullable = true
  )
  private Boolean commandLinkActive;

  @Schema(
      description = "Time-related state for the vehicle.",
      nullable = true
  )
  private TimeState timeState;

  @Schema(
      description = "Overall system and health state for the vehicle.",
      nullable = true
  )
  private SystemState systemState;

  @Schema(
      description = "Environmental conditions associated with the vehicle.",
      nullable = true
  )
  private EnvironmentalState environmentalState;

  @Schema(
      description = "Current landed state.",
      example = "IN_AIR",
      nullable = true
  )
  private String landedState;

  @Schema(
      description = "Current VTOL state.",
      example = "FIXED_WING",
      nullable = true
  )
  private String vtolState;

  @Schema(
      description = "Current mission sequence number.",
      example = "12",
      nullable = true
  )
  private Integer currentMissionSequence;

  @Schema(
      description = "Timestamp of the last operational state update.",
      example = "2026-04-20T05:42:00Z",
      nullable = true
  )
  private Instant operationalUpdatedAt;

  @Schema(
      description = "Last status text message reported by the vehicle.",
      example = "GPS Glitch",
      nullable = true
  )
  private String lastStatusText;

  @Schema(
      description = "Last command id acknowledged by the vehicle.",
      example = "400",
      nullable = true
  )
  private Integer lastAcknowledgedCommand;

  @Schema(
      description = "Last command acknowledgement result.",
      example = "ACCEPTED",
      nullable = true
  )
  private String lastCommandAcknowledgement;

  @Schema(
      description = "Timestamp of the last command acknowledgement.",
      example = "2026-05-26T05:10:00Z",
      nullable = true
  )
  private Instant lastCommandAcknowledgementAt;


  @Schema(
      description = "Current readiness state of the vehicle twin.",
      example = "REGISTRATION_READY",
      nullable = true
  )
  private String readinessState;

  @Schema(
      description = "Indicates whether this twin has enough information to be registered upstream.",
      example = "true",
      nullable = true
  )
  private Boolean registrationReady;

  @Schema(
      description = "Indicates whether this twin has enough information to accept command execution.",
      example = "false",
      nullable = true
  )
  private Boolean commandReady;

  @Schema(
      description = "Machine-readable missing readiness items.",
      nullable = true
  )
  private List<String> missingReadinessItems;

  @Schema(
      description = "Machine-readable degraded readiness items.",
      nullable = true
  )
  private List<String> degradedReadinessItems;

  @Schema(
      description = "Machine-readable blocking readiness items.",
      nullable = true
  )
  private List<String> blockingReadinessItems;

  @Schema(
      description = "Timestamp of the last readiness evaluation.",
      example = "2026-05-26T05:50:00Z",
      nullable = true
  )
  private Instant readinessUpdatedAt;

  public String getProtocolSourceId() {
    if (systemId == null || componentId == null) {
      return null;
    }
    return "mavlink:" + systemId + ":" + componentId;
  }

  public Long getOperationalUpdatedAtSeconds() {
    return operationalUpdatedAt != null ? operationalUpdatedAt.getEpochSecond() : null;
  }

  public DroneTwin(String twinId) {
    super(twinId);
    setTwinType(TwinType.DRONE);
    setMmsi(generateSyntheticMmsi(twinId));
  }
}
