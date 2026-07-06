/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License.
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

package io.mapsmessaging.state.mavlink.model.impl.uuv;

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.model.DetectionEventType;
import io.mapsmessaging.state.drone.model.DroneContactManager;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandIntFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.HomeRequest;
import io.mapsmessaging.state.mavlink.model.MissionPlan;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanItemType;
import io.mapsmessaging.state.mavlink.model.PlanValidation;
import io.mapsmessaging.state.mavlink.model.PlanValidationIssue;
import io.mapsmessaging.state.mavlink.model.RepositionRequest;
import io.mapsmessaging.state.mavlink.model.UuvModel;
import io.mapsmessaging.state.mavlink.model.UnsupportedUxvOperationException;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.model.UxvVehicleType;
import io.mapsmessaging.state.mavlink.model.impl.AbstractUxvModel;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.NamedValueFloatPacket;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class SticklebackArdupilotUuvModel extends AbstractUxvModel implements UuvModel {

    public static final String MODEL_NAME = "stickleback-ardupilot-uuv";

    private static final int DETECTION_LOST = 0;
    private static final int DETECTION_PRESENT = 1;
    private static final float DETECTION_STATE_EPSILON = 0.001f;
    private static final long CONTACT_TTL_MILLIS = 60_000L;



    private static final int MAV_CMD_DO_SET_HOME = 179;
    private static final int MAV_CMD_DO_CHANGE_SPEED = 178;
    private static final int MAV_CMD_CONDITION_YAW = 115;

    private static final float USE_CURRENT_POSITION = 1.0f;
    private static final float USE_SPECIFIED_POSITION = 0.0f;
    private static final float GROUND_SPEED = 1.0f;
    private static final float UNCHANGED_THROTTLE = -1.0f;
    private static final float CLOCKWISE_YAW_DIRECTION = 1.0f;
    private static final float RELATIVE_YAW_OFFSET = 0.0f;
    private static final float DEFAULT_ACCEPTANCE_RADIUS_METERS = 2.0f;
    private static final float DEFAULT_PASS_RADIUS_METERS = 0.0f;
    private static final float DEFAULT_HOLD_SECONDS = 0.0f;

    public SticklebackArdupilotUuvModel() {
        super(
                MODEL_NAME,
                UxvVehicleType.UUV,
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
                        UxvOperation.SET_SPEED,
                        UxvOperation.SET_HEADING));
    }

    @Override
    public UxvModelCommandSet arm(UxvCommandContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return UxvModelCommandSet.of(UxvOperation.ARM, getModelName(), MavlinkCommandLongFactory.arm(context.targetSystem(), context.targetComponent(), context.sequence()));
    }

    @Override
    public UxvModelCommandSet disarm(UxvCommandContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return UxvModelCommandSet.of(UxvOperation.DISARM, getModelName(), MavlinkCommandLongFactory.disarm(context.targetSystem(), context.targetComponent(), context.sequence()));
    }

    @Override
    public UxvModelCommandSet setHome(UxvCommandContext context, HomeRequest request) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(request, "request must not be null");

        MavlinkCommandLong commandLong = MavlinkCommandLongFactory.command(context.targetSystem(), context.targetComponent(), MAV_CMD_DO_SET_HOME, context.sequence());
        commandLong.setParam1(request.useCurrentPosition() ? USE_CURRENT_POSITION : USE_SPECIFIED_POSITION);

        if (!request.useCurrentPosition()) {
            GeoPosition position = Objects.requireNonNull(request.position(), "position must not be null when useCurrentPosition is false");
            commandLong.setParam5(toFloatCoordinate(position.getLatitude(), "latitude"));
            commandLong.setParam6(toFloatCoordinate(position.getLongitude(), "longitude"));
            commandLong.setParam7(toAltitude(position));
        }

        return UxvModelCommandSet.of(UxvOperation.SET_HOME, getModelName(), commandLong);
    }

    @Override
    public UxvModelCommandSet reposition(UxvCommandContext context, RepositionRequest request) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(request, "request must not be null");
        rejectSpeed(request.speedMetersPerSecond(), UxvOperation.REPOSITION);

        float yawDegrees = request.yawDegrees() == null ? Float.NaN : normaliseDegrees(request.yawDegrees());
        return UxvModelCommandSet.of(
                UxvOperation.REPOSITION,
                getModelName(),
                MavlinkCommandIntFactory.reposition(context.targetSystem(), context.targetComponent(), request.position(), yawDegrees, context.sequence()));
    }

    @Override
    public UxvModelCommandSet holdPosition(UxvCommandContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return UxvModelCommandSet.of(UxvOperation.HOLD_POSITION, getModelName(), pauseCommand(context));
    }

    @Override
    public UxvModelCommandSet stop(UxvCommandContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return UxvModelCommandSet.of(UxvOperation.STOP, getModelName(), pauseCommand(context));
    }

    @Override
    public UxvModelCommandSet pauseVehicle(UxvCommandContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return UxvModelCommandSet.of(UxvOperation.PAUSE_VEHICLE, getModelName(), pauseCommand(context));
    }

    @Override
    public UxvModelCommandSet resumeVehicle(UxvCommandContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return UxvModelCommandSet.of(UxvOperation.RESUME_VEHICLE, getModelName(), MavlinkCommandLongFactory.resume(context.targetSystem(), context.targetComponent(), context.sequence()));
    }

    @Override
    public PlanValidation validateMission(MissionPlan missionPlan) {
        Objects.requireNonNull(missionPlan, "missionPlan must not be null");

        List<PlanValidationIssue> issues = new ArrayList<>();
        if (missionPlan.items().isEmpty()) {
            issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission plan must contain at least one item"));
        }

        for (int i = 0; i < missionPlan.items().size(); i++) {
            validatePlanItem(i, missionPlan.items().get(i), issues);
        }

        if (issues.isEmpty()) {
            return PlanValidation.success();
        }
        return PlanValidation.failure(issues);
    }

    @Override
    public UxvModelCommandSet buildMission(UxvCommandContext context, MissionPlan missionPlan) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(missionPlan, "missionPlan must not be null");

        PlanValidation validation = validateMission(missionPlan);
        if (!validation.valid()) {
            throw new IllegalArgumentException("Invalid mission plan: " + validation.issues());
        }

        List<MavlinkMessage> messages = new ArrayList<>();
        List<PlanItem> items = missionPlan.items();

        for (int i = 0; i < items.size(); i++) {
            messages.add(toMissionMessage(context, i, items.get(i)));
        }

        return UxvModelCommandSet.of(UxvOperation.BUILD_MISSION, getModelName(), messages);
    }

    @Override
    public UxvModelCommandSet startMission(UxvCommandContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return UxvModelCommandSet.of(UxvOperation.START_MISSION, getModelName(), MavlinkCommandLongFactory.missionStart(context.targetSystem(), context.targetComponent(), context.sequence()));
    }

    @Override
    public UxvModelCommandSet setSpeed(UxvCommandContext context, double speedMetersPerSecond) {
        Objects.requireNonNull(context, "context must not be null");
        requirePositiveOrZero(speedMetersPerSecond, "speedMetersPerSecond");

        MavlinkCommandLong commandLong = MavlinkCommandLongFactory.command(context.targetSystem(), context.targetComponent(), MAV_CMD_DO_CHANGE_SPEED, context.sequence());
        commandLong.setParam1(GROUND_SPEED);
        commandLong.setParam2((float) speedMetersPerSecond);
        commandLong.setParam3(UNCHANGED_THROTTLE);

        return UxvModelCommandSet.of(UxvOperation.SET_SPEED, getModelName(), commandLong);
    }

    @Override
    public UxvModelCommandSet setHeading(UxvCommandContext context, float headingDegrees) {
        Objects.requireNonNull(context, "context must not be null");

        MavlinkCommandLong commandLong = MavlinkCommandLongFactory.command(context.targetSystem(), context.targetComponent(), MAV_CMD_CONDITION_YAW, context.sequence());
        commandLong.setParam1(normaliseDegrees(headingDegrees));
        commandLong.setParam2(0.0f);
        commandLong.setParam3(CLOCKWISE_YAW_DIRECTION);
        commandLong.setParam4(RELATIVE_YAW_OFFSET);

        return UxvModelCommandSet.of(UxvOperation.SET_HEADING, getModelName(), commandLong);
    }

    private MavlinkCommandLong pauseCommand(UxvCommandContext context) {
        return MavlinkCommandLongFactory.pause(context.targetSystem(), context.targetComponent(), context.sequence());
    }

    private MavlinkMessage toMissionMessage(UxvCommandContext context, int sequence, PlanItem item) {
        return switch (item.type()) {
            case WAYPOINT -> MavlinkMissionItemIntFactory.waypoint(
                    context.targetSystem(),
                    context.targetComponent(),
                    sequence,
                    item.position(),
                    toSeconds(item.holdDuration()),
                    item.radiusMeters() == null ? DEFAULT_ACCEPTANCE_RADIUS_METERS : item.radiusMeters().floatValue(),
                    DEFAULT_PASS_RADIUS_METERS,
                    item.yawDegrees() == null ? Float.NaN : normaliseDegrees(item.yawDegrees()));
            case RETURN_TO_HOME -> MavlinkMissionItemIntFactory.returnToLaunch(context.targetSystem(), context.targetComponent(), sequence);
            case LOITER, ORBIT, HOLD_POSITION -> throw new UnsupportedUxvOperationException(
                    getModelName(),
                    UxvOperation.BUILD_MISSION,
                    "Mission item type " + item.type() + " is not supported by this Stickleback ArduPilot UUV model");
        };
    }

    private void validatePlanItem(int index, PlanItem item, List<PlanValidationIssue> issues) {
        if (item.type() == PlanItemType.WAYPOINT && item.position() == null) {
            issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " requires a position"));
        }

        if (item.type() == PlanItemType.LOITER || item.type() == PlanItemType.ORBIT || item.type() == PlanItemType.HOLD_POSITION) {
            issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " type " + item.type() + " is not supported by this Stickleback ArduPilot UUV model"));
        }

        if (item.radiusMeters() != null && !isPositiveOrZero(item.radiusMeters())) {
            issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " radiusMeters must be finite and must not be negative"));
        }

        if (item.speedMetersPerSecond() != null) {
            issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " speedMetersPerSecond is not currently mapped by this Stickleback ArduPilot UUV model"));
        }

        if (item.altitudeMeters() != null) {
            issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " altitudeMeters is not currently mapped by this Stickleback ArduPilot UUV model"));
        }

        if (item.depthMeters() != null) {
            issues.add(new PlanValidationIssue(UxvOperation.BUILD_MISSION, "Mission item " + index + " depthMeters is not currently mapped by this Stickleback ArduPilot UUV model"));
        }
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


    private float toAltitude(GeoPosition position) {
        Double altitudeMeters = position.getPreferredAltitudeMeters();
        if (altitudeMeters == null) {
            return 0.0f;
        }
        requireFinite(altitudeMeters, "altitudeMeters");
        return altitudeMeters.floatValue();
    }

    private float toFloatCoordinate(Double coordinate, String name) {
        if (coordinate == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        requireFinite(coordinate, name);
        return coordinate.floatValue();
    }

    private float toSeconds(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return DEFAULT_HOLD_SECONDS;
        }
        return (float) duration.toSeconds();
    }

    private void rejectSpeed(Double speedMetersPerSecond, UxvOperation operation) {
        if (speedMetersPerSecond != null) {
            throw new IllegalArgumentException(operation + " does not currently map speedMetersPerSecond for model " + getModelName());
        }
    }

    private void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private void requirePositiveOrZero(double value, String name) {
        requireFinite(value, name);
        if (value < 0.0d) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    private boolean isPositiveOrZero(double value) {
        return Double.isFinite(value) && value >= 0.0d;
    }

    private float normaliseDegrees(float degrees) {
        requireFinite(degrees, "degrees");

        float normalised = degrees % 360.0f;
        if (normalised < 0.0f) {
            normalised += 360.0f;
        }

        return normalised;
    }
}