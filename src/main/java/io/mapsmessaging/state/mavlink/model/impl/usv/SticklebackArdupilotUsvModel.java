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

package io.mapsmessaging.state.mavlink.model.impl.usv;

import io.mapsmessaging.configuration.SystemProperties;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.model.DetectionEventType;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.*;
import io.mapsmessaging.state.mavlink.model.*;
import io.mapsmessaging.state.mavlink.model.impl.ardupilot.GenericArduPilotUxvModel;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.NamedValueFloatPacket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class SticklebackArdupilotUsvModel extends GenericArduPilotUxvModel implements UsvModel {

  public static final String MODEL_NAME = "stickleback-ardupilot-usv";

  public static final double DEFAULT_ALTITUDE_METERS = 10.0d;

  /** @deprecated Use {@link #DEFAULT_ALTITUDE_METERS}. */
  @Deprecated
  public static final double MAX_ALTITUDE_METERS = DEFAULT_ALTITUDE_METERS;
  private static final long CONTACT_TTL_MILLIS = getDetectionTime();
  private static final int DETECTION_LOST = 0;
  private static final int DETECTION_PRESENT = 1;
  private static final float DETECTION_STATE_EPSILON = 0.001f;

  public SticklebackArdupilotUsvModel() {
    super(
        MODEL_NAME,
        UxvVehicleType.USV,
        operations(
            UxvOperation.ARM,
            UxvOperation.DISARM,
            UxvOperation.SET_HOME,
            UxvOperation.REPOSITION,
            UxvOperation.HOLD_POSITION,
            UxvOperation.STOP,
            UxvOperation.PAUSE_VEHICLE,
            UxvOperation.RESUME_VEHICLE,
            UxvOperation.BUILD_MISSION,
            UxvOperation.START_MISSION,
            UxvOperation.NAVIGATE,
            UxvOperation.SET_SPEED,
            UxvOperation.SET_HEADING,
            UxvOperation.LOITER));
  }

  @Override
  public UxvModelCommandSet reposition(UxvCommandContext context, RepositionRequest request) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(request, "request must not be null");
    rejectSpeed(request.speedMetersPerSecond(), UxvOperation.REPOSITION);

    GeoPosition position = Objects.requireNonNull(request.position(), "position must not be null");
    validateCoordinates(position, "position");
    double altitudeMeters = resolveAltitude(request.altitudeMeters());

    List<MavlinkMessage> messages =
        List.of(
            MavlinkCommandIntFactory.repositionRelativeAltitude(
                context.targetSystem(),
                context.targetComponent(),
                position,
                altitudeMeters,
                context.sequence()),
            MavlinkCommandLongFactory.guidedMode(
                context.targetSystem(),
                context.targetComponent(),
                context.sequence()),
            MavlinkMissionItemFactory.guidedWaypointRelativeAltitude(
                context.targetSystem(),
                context.targetComponent(),
                position,
                altitudeMeters));

    return UxvModelCommandSet.of(UxvOperation.REPOSITION, getModelName(), messages);
  }

  @Override
  public UxvModelCommandSet resumeVehicle(UxvCommandContext context) {
    Objects.requireNonNull(context, "context must not be null");
    return UxvModelCommandSet.of(
        UxvOperation.RESUME_VEHICLE,
        getModelName(),
        MavlinkCommandLongFactory.missionStart(
            context.targetSystem(), context.targetComponent(), context.sequence()));
  }

  @Override
  public UxvModelCommandSet loiter(UxvCommandContext context, LoiterRequest request) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(request, "request must not be null");
    requirePositiveOrZero(request.radiusMeters(), "radiusMeters");
    rejectDepth(request.depthMeters(), UxvOperation.LOITER);
    rejectYaw(request.yawDegrees(), UxvOperation.LOITER);

    GeoPosition position = Objects.requireNonNull(request.position(), "position must not be null");
    validateCoordinates(position, "position");
    double altitudeMeters = resolveAltitude(request.altitudeMeters());

    Duration duration = toDuration(request.duration(), "duration");
    MavlinkMessage message;
    if (duration.isZero()) {
      message =
          MavlinkCommandIntFactory.loiterUnlimitedRelativeAltitude(
              context.targetSystem(),
              context.targetComponent(),
              position,
              altitudeMeters,
              request.radiusMeters(),
              Float.NaN,
              context.sequence());
    } else {
      message =
          MavlinkCommandIntFactory.loiterTimeRelativeAltitude(
              context.targetSystem(),
              context.targetComponent(),
              position,
              altitudeMeters,
              request.radiusMeters(),
              duration,
              context.sequence());
    }

    return UxvModelCommandSet.of(UxvOperation.LOITER, getModelName(), message);
  }

  @Override
  protected void rejectYaw(Float yawDegrees, UxvOperation operation) {
  }

  @Override
  protected MavlinkMessage toMissionMessage(UxvCommandContext context, int sequence, PlanItem item) {
    return switch (item.type()) {
      case WAYPOINT ->
          MavlinkMissionItemIntFactory.waypointRelativeAltitude(
              context.targetSystem(),
              context.targetComponent(),
              sequence,
              item.position(),
              resolveAltitude(item.altitudeMeters()),
              toSeconds(item.holdDuration()),
              item.radiusMeters() == null ? DEFAULT_ACCEPTANCE_RADIUS_METERS : item.radiusMeters().floatValue(),
              DEFAULT_PASS_RADIUS_METERS,
              item.yawDegrees() == null ? Float.NaN : normaliseDegrees(item.yawDegrees()));
      case LOITER -> toMissionLoiter(context, sequence, item);
      case RETURN_TO_HOME -> MavlinkMissionItemIntFactory.returnToLaunch(context.targetSystem(), context.targetComponent(), sequence);
      case ORBIT, HOLD_POSITION ->
          throw new UnsupportedUxvOperationException(
              getModelName(),
              UxvOperation.BUILD_MISSION,
              "Mission item type " + item.type() + " is not supported by this Stickleback ArduPilot USV model");
    };
  }

  private MavlinkMessage toMissionLoiter(UxvCommandContext context, int sequence, PlanItem item) {
    double radiusMeters = item.radiusMeters() == null ? DEFAULT_ACCEPTANCE_RADIUS_METERS : item.radiusMeters();
    requirePositiveOrZero(radiusMeters, "radiusMeters");
    double altitudeMeters = resolveAltitude(item.altitudeMeters());

    Duration duration = toDuration(item.holdDuration(), "holdDuration");
    if (duration.isZero()) {
      return MavlinkMissionItemIntFactory.loiterUnlimitedRelativeAltitude(
          context.targetSystem(),
          context.targetComponent(),
          sequence,
          item.position(),
          altitudeMeters,
          radiusMeters);
    }
    return MavlinkMissionItemIntFactory.loiterTimeRelativeAltitude(
        context.targetSystem(),
        context.targetComponent(),
        sequence,
        item.position(),
        altitudeMeters,
        duration,
        radiusMeters);
  }

  @Override
  protected void validatePlanItem(int index, PlanItem item, List<PlanValidationIssue> issues) {
    if (item.type() == PlanItemType.ORBIT || item.type() == PlanItemType.HOLD_POSITION) {
      issues.add(
          new PlanValidationIssue(
              UxvOperation.BUILD_MISSION,
              "Mission item " + index + " type " + item.type() + " is not supported by this Stickleback ArduPilot USV model"));
    }

    if (item.type() == PlanItemType.LOITER && item.yawDegrees() != null) {
      issues.add(
          new PlanValidationIssue(
              UxvOperation.BUILD_MISSION,
              "Mission item " + index + " yawDegrees is not currently mapped for loiter by this Stickleback ArduPilot USV model"));
    }

    if (item.speedMetersPerSecond() != null) {
      issues.add(
          new PlanValidationIssue(
              UxvOperation.BUILD_MISSION,
              "Mission item " + index + " speedMetersPerSecond is not currently mapped by this Stickleback ArduPilot USV model"));
    }

    if (item.altitudeMeters() != null
        && !Double.isFinite(item.altitudeMeters())) {
      issues.add(
          new PlanValidationIssue(
              UxvOperation.BUILD_MISSION,
              "Mission item " + index + " altitudeMeters must be a finite value"));
    }

    if (item.depthMeters() != null) {
      issues.add(
          new PlanValidationIssue(
              UxvOperation.BUILD_MISSION,
              "Mission item " + index + " depthMeters is not valid for this Stickleback ArduPilot USV model"));
    }
  }

  @Override
  public boolean supportsPassiveDetection() {
    return true;
  }

  @Override
  public Optional<DetectionEvent> interpretDetection(DroneTwin droneTwin, MavlinkPacket event) {
    if (!(event instanceof NamedValueFloatPacket packet)) {
      return Optional.empty();
    }

    if (!packet.isValid() || !packet.hasName() || !packet.hasValue()) {
      return Optional.empty();
    }

    OptionalInt detectionState = detectionState(packet.getValue());
    if (detectionState.isEmpty()) {
      return Optional.empty();
    }

    UUID contactId = UUID.nameUUIDFromBytes(packet.getName().getBytes(StandardCharsets.UTF_8));
    return switch (detectionState.getAsInt()) {
      case DETECTION_PRESENT -> Optional.of(createDetectedEvent(droneTwin, packet, contactId));
      case DETECTION_LOST -> Optional.of(createLostEvent(packet, contactId));
      default -> Optional.empty();
    };
  }

  private OptionalInt detectionState(double value) {
    int state = (int) Math.round(value);
    if (Math.abs(value - state) > DETECTION_STATE_EPSILON) {
      return OptionalInt.empty();
    }
    if (state != DETECTION_PRESENT && state != DETECTION_LOST) {
      return OptionalInt.empty();
    }
    return OptionalInt.of(state);
  }

  private DetectionEvent createDetectedEvent(DroneTwin droneTwin, NamedValueFloatPacket packet, UUID contactId) {
    DetectionEvent detectionEvent = new DetectionEvent(contactId, packet.getName(), DetectionEventType.DETECTED);
    detectionEvent.setPosition(droneTwin.getGeoPosition());
    detectionEvent.setTtlMillis(CONTACT_TTL_MILLIS);
    addNamedValueFloatAttributes(detectionEvent, packet);
    return detectionEvent;
  }

  private DetectionEvent createLostEvent(NamedValueFloatPacket packet, UUID contactId) {
    DetectionEvent detectionEvent = new DetectionEvent(contactId, packet.getName(), DetectionEventType.LOST);
    addNamedValueFloatAttributes(detectionEvent, packet);
    return detectionEvent;
  }

  private void addNamedValueFloatAttributes(DetectionEvent detectionEvent, NamedValueFloatPacket packet) {
    detectionEvent.addAttribute("mavlink.message", "NAMED_VALUE_FLOAT");
    detectionEvent.addAttribute("mavlink.name", packet.getName());
    detectionEvent.addAttribute("mavlink.value", packet.getValue());
  }

  private double resolveAltitude(Double altitudeMeters) {
    if (altitudeMeters == null) {
      return DEFAULT_ALTITUDE_METERS;
    }
    if (!Double.isFinite(altitudeMeters)) {
      throw new IllegalArgumentException("altitudeMeters must be a finite value");
    }
    return altitudeMeters;
  }

  private static long getDetectionTime() {
    long defaultValue = 60_000L;
    String loaded = SystemProperties.getInstance().getProperty("STICKLEBACK_DETECTION", Long.toString(defaultValue));
    try {
      long value = Long.parseLong(loaded);
      return value > 0L ? value : defaultValue;
    } catch (RuntimeException exception) {
      return defaultValue;
    }
  }
}
