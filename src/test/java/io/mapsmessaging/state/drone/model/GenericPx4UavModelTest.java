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

import io.mapsmessaging.state.mavlink.messages.MavlinkCommandInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandIntFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemIntFactory;
import io.mapsmessaging.state.mavlink.model.HomeRequest;
import io.mapsmessaging.state.mavlink.model.LoiterRequest;
import io.mapsmessaging.state.mavlink.model.MissionPlan;
import io.mapsmessaging.state.mavlink.model.OrbitDirection;
import io.mapsmessaging.state.mavlink.model.OrbitRequest;
import io.mapsmessaging.state.mavlink.model.PlanItem;
import io.mapsmessaging.state.mavlink.model.PlanItemType;
import io.mapsmessaging.state.mavlink.model.RepositionRequest;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.model.impl.uav.GenericPx4UavModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenericPx4UavModelTest {

    private static final UUID VEHICLE_ID = UUID.fromString("b52a99f0-3c72-4e4b-bb7c-29f12390fb42");
    private static final int TARGET_SYSTEM = 3;
    private static final int TARGET_COMPONENT = 1;
    private static final int SOURCE_SYSTEM = 255;
    private static final int SOURCE_COMPONENT = 0;
    private static final int SEQUENCE = 42;

    private final GenericPx4UavModel model = new GenericPx4UavModel();

    @Test
    void arm_returnsArmCommandLong() {
        UxvModelCommandSet commandSet = model.arm(context());

        assertEquals(UxvOperation.ARM, commandSet.operation());
        assertEquals(GenericPx4UavModel.MODEL_NAME, commandSet.modelName());
        assertEquals(1, commandSet.messages().size());

        MavlinkCommandLong command = assertInstanceOf(MavlinkCommandLong.class, commandSet.messages().get(0));
        assertEquals(MavlinkCommandLong.MESSAGE_ID_COMMAND_LONG, command.getMessageId());
        assertEquals(MavlinkCommandLongFactory.MAV_CMD_COMPONENT_ARM_DISARM, command.getCommand());
        assertEquals(MavlinkCommandLongFactory.ARM, command.getParam1());
        assertEquals(TARGET_SYSTEM, command.getTargetSystem());
        assertEquals(TARGET_COMPONENT, command.getTargetComponent());
        assertEquals(SEQUENCE, command.getSequence());
    }

    @Test
    void disarm_returnsDisarmCommandLong() {
        UxvModelCommandSet commandSet = model.disarm(context());

        MavlinkCommandLong command = assertSingleCommandLong(commandSet, UxvOperation.DISARM);
        assertEquals(MavlinkCommandLongFactory.MAV_CMD_COMPONENT_ARM_DISARM, command.getCommand());
        assertEquals(MavlinkCommandLongFactory.DISARM, command.getParam1());
    }

    @Test
    void returnToHome_returnsReturnToLaunchCommandLong() {
        UxvModelCommandSet commandSet = model.returnToHome(context());

        MavlinkCommandLong command = assertSingleCommandLong(commandSet, UxvOperation.RETURN_TO_HOME);
        assertEquals(MavlinkCommandLongFactory.MAV_CMD_NAV_RETURN_TO_LAUNCH, command.getCommand());
    }

    @Test
    void pauseVehicle_returnsPauseContinueCommandLong() {
        UxvModelCommandSet commandSet = model.pauseVehicle(context());

        MavlinkCommandLong command = assertSingleCommandLong(commandSet, UxvOperation.PAUSE_VEHICLE);
        assertEquals(MavlinkCommandLongFactory.MAV_CMD_DO_PAUSE_CONTINUE, command.getCommand());
        assertEquals(MavlinkCommandLongFactory.PAUSE, command.getParam1());
    }

    @Test
    void resumeVehicle_returnsPauseContinueCommandLong() {
        UxvModelCommandSet commandSet = model.resumeVehicle(context());

        MavlinkCommandLong command = assertSingleCommandLong(commandSet, UxvOperation.RESUME_VEHICLE);
        assertEquals(MavlinkCommandLongFactory.MAV_CMD_DO_PAUSE_CONTINUE, command.getCommand());
        assertEquals(MavlinkCommandLongFactory.CONTINUE, command.getParam1());
    }

    @Test
    void stop_mapsToPauseVehicle() {
        UxvModelCommandSet commandSet = model.stop(context());

        MavlinkCommandLong command = assertSingleCommandLong(commandSet, UxvOperation.PAUSE_VEHICLE);
        assertEquals(MavlinkCommandLongFactory.MAV_CMD_DO_PAUSE_CONTINUE, command.getCommand());
        assertEquals(MavlinkCommandLongFactory.PAUSE, command.getParam1());
    }

    @Test
    void setHome_currentPosition_setsUseCurrentPosition() {
        UxvModelCommandSet commandSet = model.setHome(context(), HomeRequest.currentPosition());

        MavlinkCommandLong command = assertSingleCommandLong(commandSet, UxvOperation.SET_HOME);
        assertEquals(179, command.getCommand());
        assertEquals(1.0f, command.getParam1());
    }

    @Test
    void setHome_specificPosition_setsCoordinates() {
        GeoPosition position = position();

        UxvModelCommandSet commandSet = model.setHome(context(), HomeRequest.position(position));

        MavlinkCommandLong command = assertSingleCommandLong(commandSet, UxvOperation.SET_HOME);
        assertEquals(179, command.getCommand());
        assertEquals(0.0f, command.getParam1());
        assertEquals(position.getLatitude().floatValue(), command.getParam5());
        assertEquals(position.getLongitude().floatValue(), command.getParam6());
        assertEquals(position.getPreferredAltitudeMeters().floatValue(), command.getParam7());
    }

    @Test
    void reposition_returnsCommandIntDoReposition() {
        RepositionRequest request = new RepositionRequest(position(), 90.0f, null);

        UxvModelCommandSet commandSet = model.reposition(context(), request);

        assertEquals(UxvOperation.REPOSITION, commandSet.operation());
        assertEquals(GenericPx4UavModel.MODEL_NAME, commandSet.modelName());
        assertEquals(3, commandSet.messages().size());
        MavlinkCommandInt command = assertInstanceOf(MavlinkCommandInt.class, commandSet.messages().get(0));
        assertEquals(MavlinkCommandIntFactory.MAV_CMD_DO_REPOSITION, command.getCommand());
        assertEquals(MavlinkCommandIntFactory.MAV_FRAME_GLOBAL_INT, command.getFrame());
        assertEquals(toScaledCoordinate(position().getLatitude()), command.getLatitude());
        assertEquals(toScaledCoordinate(position().getLongitude()), command.getLongitude());
        assertEquals(position().getPreferredAltitudeMeters().floatValue(), command.getAltitude());
    }

    @Test
    void orbit_returnsCommandIntDoOrbit() {
        OrbitRequest request = new OrbitRequest(position(), 25.0d, 120.0d, null, null, OrbitDirection.CLOCKWISE, null);

        UxvModelCommandSet commandSet = model.orbit(context(), request);

        MavlinkCommandInt command = assertSingleCommandInt(commandSet, UxvOperation.ORBIT);
        assertEquals(MavlinkCommandIntFactory.MAV_CMD_DO_ORBIT, command.getCommand());
        assertEquals(25.0f, command.getParam1());
        assertEquals(120.0f, command.getAltitude());
    }

    @Test
    void orbit_counterClockwiseUsesNegativeRadius() {
        OrbitRequest request =
            new OrbitRequest(
                position(),
                25.0d,
                120.0d,
                null,
                null,
                OrbitDirection.COUNTER_CLOCKWISE,
                null);

        UxvModelCommandSet commandSet = model.orbit(context(), request);

        MavlinkCommandInt command = assertSingleCommandInt(commandSet, UxvOperation.ORBIT);
        assertEquals(MavlinkCommandIntFactory.MAV_CMD_DO_ORBIT, command.getCommand());
        assertEquals(-25.0f, command.getParam1());
        assertEquals(120.0f, command.getAltitude());
    }

    @Test
    void orbit_withDepth_throws() {
        OrbitRequest request = new OrbitRequest(position(), 25.0d, null, 5.0d, null, OrbitDirection.CLOCKWISE, null);

        assertThrows(IllegalArgumentException.class, () -> model.orbit(context(), request));
    }

    @Test
    void loiter_withoutDuration_returnsUnlimitedLoiter() {
        LoiterRequest request = new LoiterRequest(position(), 10.0d, null, 45.0f, 100.0d, null);

        UxvModelCommandSet commandSet = model.loiter(context(), request);

        MavlinkCommandInt command = assertSingleCommandInt(commandSet, UxvOperation.LOITER);
        assertEquals(MavlinkCommandIntFactory.MAV_CMD_NAV_LOITER_UNLIM, command.getCommand());
        assertEquals(10.0f, command.getParam3());
        assertEquals(100.0f, command.getAltitude());
    }

    @Test
    void loiter_withDuration_returnsTimedLoiter() {
        LoiterRequest request = new LoiterRequest(position(), 10.0d, Duration.ofSeconds(30), null, 100.0d, null);

        UxvModelCommandSet commandSet = model.loiter(context(), request);

        MavlinkCommandInt command = assertSingleCommandInt(commandSet, UxvOperation.LOITER);
        assertEquals(MavlinkCommandIntFactory.MAV_CMD_NAV_LOITER_TIME, command.getCommand());
        assertEquals(30.0f, command.getParam1());
    }

    @Test
    void loiter_withDurationAndYaw_throws() {
        LoiterRequest request = new LoiterRequest(position(), 10.0d, Duration.ofSeconds(30), 45.0f, 100.0d, null);

        assertThrows(IllegalArgumentException.class, () -> model.loiter(context(), request));
    }

    @Test
    void takeOff_returnsTakeoffCommandLong() {
        UxvModelCommandSet commandSet = model.takeOff(context(), 50.0d);

        MavlinkCommandLong command = assertSingleCommandLong(commandSet, UxvOperation.TAKE_OFF);
        assertEquals(22, command.getCommand());
        assertEquals(50.0f, command.getParam7());
    }

    @Test
    void land_returnsLandCommandLong() {
        UxvModelCommandSet commandSet = model.land(context());

        MavlinkCommandLong command = assertSingleCommandLong(commandSet, UxvOperation.LAND);
        assertEquals(21, command.getCommand());
    }

    @Test
    void setSpeed_returnsChangeSpeedCommandLong() {
        UxvModelCommandSet commandSet = model.setSpeed(context(), 4.5d);

        MavlinkCommandLong command = assertSingleCommandLong(commandSet, UxvOperation.SET_SPEED);
        assertEquals(178, command.getCommand());
        assertEquals(1.0f, command.getParam1());
        assertEquals(4.5f, command.getParam2());
        assertEquals(-1.0f, command.getParam3());
    }

    @Test
    void setHeading_returnsConditionYawCommandLong() {
        UxvModelCommandSet commandSet = model.setHeading(context(), -90.0f);

        MavlinkCommandLong command = assertSingleCommandLong(commandSet, UxvOperation.SET_HEADING);
        assertEquals(115, command.getCommand());
        assertEquals(270.0f, command.getParam1());
    }

    @Test
    void buildMission_buildsMissionItemIntMessages() {
        MissionPlan missionPlan = new MissionPlan(
            List.of(
                new PlanItem(PlanItemType.WAYPOINT, position(), null, null, null, null, 100.0d, null),
                new PlanItem(PlanItemType.LOITER, position(), Duration.ofSeconds(20), 15.0d, null, null, 100.0d, null),
                new PlanItem(PlanItemType.RETURN_TO_HOME, null, null, null, null, null, null, null)));

        UxvModelCommandSet commandSet = model.buildMission(context(), missionPlan);

        assertEquals(UxvOperation.BUILD_MISSION, commandSet.operation());
        assertEquals(3, commandSet.messages().size());

        MavlinkMissionItemInt waypoint = assertInstanceOf(MavlinkMissionItemInt.class, commandSet.messages().get(0));
        assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_WAYPOINT, waypoint.getCommand());

        MavlinkMissionItemInt loiter = assertInstanceOf(MavlinkMissionItemInt.class, commandSet.messages().get(1));
        assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_LOITER_TIME, loiter.getCommand());

        MavlinkMissionItemInt returnToHome = assertInstanceOf(MavlinkMissionItemInt.class, commandSet.messages().get(2));
        assertEquals(MavlinkMissionItemIntFactory.MAV_CMD_NAV_RETURN_TO_LAUNCH, returnToHome.getCommand());
    }

    @Test
    void buildMission_withDepthItem_throws() {
        MissionPlan missionPlan = new MissionPlan(
            List.of(new PlanItem(PlanItemType.WAYPOINT, position(), null, null, null, null, null, 5.0d)));

        assertThrows(IllegalArgumentException.class, () -> model.buildMission(context(), missionPlan));
    }

    @Test
    void startMission_returnsMissionStartCommandLong() {
        UxvModelCommandSet commandSet = model.startMission(context());

        MavlinkCommandLong command = assertSingleCommandLong(commandSet, UxvOperation.START_MISSION);
        assertEquals(MavlinkCommandLongFactory.MAV_CMD_MISSION_START, command.getCommand());
    }

    private MavlinkCommandLong assertSingleCommandLong(UxvModelCommandSet commandSet, UxvOperation operation) {
        assertEquals(operation, commandSet.operation());
        assertEquals(GenericPx4UavModel.MODEL_NAME, commandSet.modelName());
        assertEquals(1, commandSet.messages().size());
        return assertInstanceOf(MavlinkCommandLong.class, commandSet.messages().get(0));
    }

    private MavlinkCommandInt assertSingleCommandInt(UxvModelCommandSet commandSet, UxvOperation operation) {
        assertEquals(operation, commandSet.operation());
        assertEquals(GenericPx4UavModel.MODEL_NAME, commandSet.modelName());
        assertEquals(1, commandSet.messages().size());
        return assertInstanceOf(MavlinkCommandInt.class, commandSet.messages().get(0));
    }

    private UxvCommandContext context() {
        return new UxvCommandContext(VEHICLE_ID, TARGET_SYSTEM, TARGET_COMPONENT, SOURCE_SYSTEM, SOURCE_COMPONENT, SEQUENCE);
    }

    private GeoPosition position() {
        return new GeoPosition(-33.8688d, 151.2093d, 120.5d, null);
    }

    private int toScaledCoordinate(Double value) {
        return (int) Math.round(value * 10_000_000.0d);
    }
}