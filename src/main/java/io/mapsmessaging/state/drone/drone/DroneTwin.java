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

import static io.mapsmessaging.state.drone.util.SyntheticMmsiGenerator.generateSyntheticMmsi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.mapsmessaging.state.config.StopActionEnum;
import io.mapsmessaging.state.config.capability.TaskCapabilities;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.model.Contact;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.model.DroneContactManager;
import io.mapsmessaging.state.drone.model.EnvironmentalState;
import io.mapsmessaging.state.drone.model.SystemState;
import io.mapsmessaging.state.drone.model.TimeState;
import io.mapsmessaging.state.drone.model.autopilot.AutopilotState;
import io.mapsmessaging.state.mavlink.sender.MavlinkEventListSender;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Twin representing an unmanned aircraft or vehicle. */
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Digital state model representing a drone or unmanned vehicle.")
public class DroneTwin extends EntityTwin {

  @Schema(description = "MAVLink or protocol-specific system identifier.", example = "1", nullable = true)
  private Integer systemId;

  @Schema(description = "MAVLink or protocol-specific component identifier.", example = "1", nullable = true)
  private Integer componentId;

  @Schema(description = "Configured UxV model name used to resolve the command model implementation.", example = "generic-px4-uav", nullable = true)
  private String modelName;

  @Schema(description = "Synthetic or assigned MMSI used for external maritime-style identity mapping.", example = "999001234", nullable = true)
  private Long mmsi;

  @Schema(description = "Registration or tail identifier of the vehicle.", example = "VH-DRN-01", nullable = true)
  private String registrationId;

  @Schema(description = "Human-readable description of the drone or vehicle.", example = "Primary survey drone", nullable = true)
  private String descriptionString;

  @Schema(description = "Short 7 char string used for the call sign of the vessel.", example = "drone01", nullable = true)
  private String callSign;

  @Schema(description = "Task capabilities supported by this drone or unmanned vehicle.", nullable = true)
  private TaskCapabilities capabilities = new TaskCapabilities();

  @JsonIgnore
  @Schema(hidden = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private final DroneContactManager contactManager = new DroneContactManager();

  @JsonIgnore
  @Schema(hidden = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Getter(AccessLevel.NONE)
  private transient final AtomicReference<MavlinkEventListSender> activeMavlinkSender = new AtomicReference<>();

  @Schema(description = "Decoded autopilot information for the vehicle.", nullable = true)
  private AutopilotState autopilotState;

  @Schema(description = "Indicates whether the vehicle is armed.", example = "true", nullable = true)
  private Boolean armed;

  @Schema(description = "Current flight mode reported by the vehicle.", example = "AUTO", nullable = true)
  private String flightMode;

  @Schema(description = "Indicates whether the vehicle is in a failsafe state.", example = "false", nullable = true)
  private Boolean failsafe;

  @Schema(description = "Indicates whether GPS is currently valid and usable.", example = "true", nullable = true)
  private Boolean gpsValid;

  @Schema(description = "Current mission execution state.", example = "ACTIVE", nullable = true)
  private String missionState;

  @Schema(description = "Vehicle heading in degrees.", example = "182.4", nullable = true)
  private Double headingDegrees;

  @Schema(description = "Course over ground in degrees.", example = "180.0", nullable = true)
  private Double courseOverGroundDegrees;

  @Schema(description = "Ground speed in meters per second.", example = "14.8", nullable = true)
  private Double groundSpeedMetersPerSecond;

  @Schema(description = "Vertical speed in meters per second.", example = "-0.6", nullable = true)
  private Double verticalSpeedMetersPerSecond;

  @Schema(description = "Climb rate in meters per second.", example = "1.2", nullable = true)
  private Double climbRateMetersPerSecond;

  @Schema(description = "Identifier of the controlling station or operator endpoint.", example = "gcs-01", nullable = true)
  private String controllingStationId;

  @Schema(description = "Indicates whether the command and control link is active.", example = "true", nullable = true)
  private Boolean commandLinkActive;

  @Schema(description = "Time-related state for the vehicle.", nullable = true)
  private TimeState timeState;

  @Schema(description = "Overall system and health state for the vehicle.", nullable = true)
  private SystemState systemState;

  @Schema(description = "Environmental conditions associated with the vehicle.", nullable = true)
  private EnvironmentalState environmentalState;

  @Schema(description = "Current landed state.", example = "IN_AIR", nullable = true)
  private String landedState;

  @Schema(description = "Current VTOL state.", example = "FIXED_WING", nullable = true)
  private String vtolState;

  @Schema(description = "Current mission sequence number.", example = "12", nullable = true)
  private Integer currentMissionSequence;

  @Schema(description = "Total mission item count reported by MAVLink.", example = "20", nullable = true)
  private Integer currentMissionTotal;

  @Schema(description = "Raw MAVLink MAV_MISSION_STATE value.", example = "3", nullable = true)
  private Integer currentMissionStateCode;

  @Schema(description = "Mission identifier reported by MAVLink.", example = "14523", nullable = true)
  private Long currentMissionId;

  @Schema(description = "Timestamp of the most recent MISSION_CURRENT observation.", example = "2026-08-17T04:10:00Z", nullable = true)
  private Instant currentMissionUpdatedAt;

  @Schema(description = "Sequence number of the most recently reached MAVLink mission item.", example = "12", nullable = true)
  private Integer lastMissionItemReachedSequence;

  @Schema(description = "Timestamp when the most recent MAVLink mission item was reported reached.", example = "2026-07-30T03:05:00Z", nullable = true)
  private Instant lastMissionItemReachedAt;

  @Schema(description = "Timestamp of the last operational state update.", example = "2026-04-20T05:42:00Z", nullable = true)
  private Instant operationalUpdatedAt;

  @Schema(description = "Last status text message reported by the vehicle.", example = "GPS Glitch", nullable = true)
  private String lastStatusText;

  @Schema(description = "Last command id acknowledged by the vehicle.", example = "400", nullable = true)
  private Integer lastAcknowledgedCommand;

  @Schema(description = "Last command acknowledgement result name.", example = "ACCEPTED", nullable = true)
  private String lastCommandAcknowledgement;

  @Schema(description = "Last command acknowledgement numeric MAVLink result.", example = "0", nullable = true)
  private Integer lastCommandAcknowledgementResult;

  @Schema(description = "MAVLink target system id from the last command acknowledgement.", example = "255", nullable = true)
  private Integer lastCommandAcknowledgementTargetSystemId;

  @Schema(description = "MAVLink target component id from the last command acknowledgement.", example = "0", nullable = true)
  private Integer lastCommandAcknowledgementTargetComponentId;

  @Schema(description = "Timestamp of the last command acknowledgement.", example = "2026-05-26T05:10:00Z", nullable = true)
  private Instant lastCommandAcknowledgementAt;

  @Schema(description = "Current readiness state of the vehicle twin.", example = "REGISTRATION_READY", nullable = true)
  private String readinessState;

  @Schema(description = "Indicates whether this twin has enough information to be registered upstream.", example = "true", nullable = true)
  private Boolean registrationReady;

  @Schema(description = "Indicates whether this twin has enough information to accept command execution.", example = "false", nullable = true)
  private Boolean commandReady;

  @Schema(description = "Machine-readable missing readiness items.", nullable = true)
  private List<String> missingReadinessItems;

  @Schema(description = "Machine-readable degraded readiness items.", nullable = true)
  private List<String> degradedReadinessItems;

  @Schema(description = "Machine-readable blocking readiness items.", nullable = true)
  private List<String> blockingReadinessItems;

  @Schema(description = "Open map to define description data", nullable = true)
  private Map<String, Object> description = new HashMap<>();

  @Schema(description = "Timestamp of the last readiness evaluation.", example = "2026-05-26T05:50:00Z", nullable = true)
  private Instant readinessUpdatedAt;

  @Schema(description = "Battery capacity in hours", example = "48", nullable = true)
  private double batteryCapacityHours;

  @Schema(
      description =
          "Survey coverage radius in metres measured from the vehicle centreline. "
              + "The effective survey width is twice this value.",
      example = "200.0",
      nullable = true)
  private Double surveyRadiusMeters;

  private StopActionEnum stopAction;

  public DroneTwin(String twinId) {
    this(twinId, null);
  }

  public DroneTwin(String twinId, UUID uuid) {
    super(twinId, uuid);
    setTwinType(TwinType.DRONE);
    setMmsi(generateSyntheticMmsi(twinId));
  }

  public List<Contact> getContactList() {
    return contactManager.getContactList();
  }

  public boolean hasContacts() {
    return contactManager.size() > 0;
  }

  @JsonIgnore
  @Schema(hidden = true)
  public DroneContactManager getContactManager() {
    return contactManager;
  }

  @JsonIgnore
  @Schema(hidden = true)
  public MavlinkEventListSender getActiveMavlinkSender() {
    return activeMavlinkSender.get();
  }

  @JsonIgnore
  @Schema(hidden = true)
  public boolean hasActiveMavlinkSender() {
    return activeMavlinkSender.get() != null;
  }

  @JsonIgnore
  @Schema(hidden = true)
  public boolean registerMavlinkSender(MavlinkEventListSender sender) {
    return activeMavlinkSender.compareAndSet(null, Objects.requireNonNull(sender, "sender must not be null"));
  }

  @JsonIgnore
  @Schema(hidden = true)
  public boolean removeMavlinkSender(MavlinkEventListSender sender) {
    return activeMavlinkSender.compareAndSet(Objects.requireNonNull(sender, "sender must not be null"), null);
  }

  @JsonIgnore
  @Schema(hidden = true)
  public boolean cancelActiveMavlinkSender() {
    MavlinkEventListSender sender = activeMavlinkSender.getAndSet(null);
    if (sender == null) {
      return false;
    }

    sender.cancel();
    return true;
  }

  @JsonIgnore
  @Schema(hidden = true)
  public boolean closeActiveMavlinkSender() {
    MavlinkEventListSender sender = activeMavlinkSender.getAndSet(null);
    if (sender == null) {
      return false;
    }

    sender.close();
    return true;
  }

  @Schema(description = "Current contacts detected by this drone. Expired contacts are removed before the list is returned.", accessMode = Schema.AccessMode.READ_ONLY)
  public String getProtocolSourceId() {
    if (systemId == null || componentId == null) {
      return null;
    }
    return "mavlink:" + systemId + ":" + componentId;
  }

  public Long getOperationalUpdatedAtSeconds() {
    return operationalUpdatedAt != null ? operationalUpdatedAt.getEpochSecond() : null;
  }
}
