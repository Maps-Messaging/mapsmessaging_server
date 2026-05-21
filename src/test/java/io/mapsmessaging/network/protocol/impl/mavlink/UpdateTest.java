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

package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.listener.*;
import io.mapsmessaging.state.mavlink.packet.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateTest {
  @Test
  void globalPosition_updatesTwin() {

    TwinManager manager = new TwinManager();
    DroneTwin drone = new DroneTwin("d1");

    manager.registerTwin(drone, new TwinUpdateContext());

    Map<String, Object> fields = Map.of(
        "lat", 100000000,
        "lon", 200000000,
        "alt", 100000,
        "vx", 100,
        "vy", 200,
        "vz", -50,
        "hdg", 9000
    );

    ProcessedFrame frame = new ProcessedFrame(
        "GLOBAL_POSITION_INT",
        null,
        fields,
        true,
        null
    );

    GlobalPositionPacket packet = new GlobalPositionPacket(frame);

    new GlobalPositionListener(manager)
        .handle("d1", packet, new TwinUpdateContext());

    DroneTwin updated = (DroneTwin) manager.getTwin("d1").get();

    assertEquals(10.0, updated.getGeoPosition().getLatitude());
    assertEquals(90.0, updated.getHeadingDegrees());
  }

  @Test
  void attitudeListener_updatesOrientation() {
    TwinManager manager = new TwinManager();
    DroneTwin drone = new DroneTwin("mavlink:test:1");

    manager.registerTwin(drone, new TwinUpdateContext());

    Map<String, Object> fields = new HashMap<>();
    fields.put("roll", Math.PI / 6.0);
    fields.put("pitch", Math.PI / 12.0);
    fields.put("yaw", Math.PI / 2.0);

    ProcessedFrame frame = new ProcessedFrame(
        "ATTITUDE",
        null,
        fields,
        true,
        List.of()
    );

    AttitudePacket packet = new AttitudePacket(frame);

    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.now());

    AttitudeListener listener = new AttitudeListener(manager);
    listener.handle("mavlink:test:1", packet, context);

    DroneTwin updated = (DroneTwin) manager.getTwin("mavlink:test:1").orElseThrow();

    assertNotNull(updated.getOrientation());
    assertEquals(30.0, updated.getOrientation().getRollDegrees(), 0.0001);
    assertEquals(15.0, updated.getOrientation().getPitchDegrees(), 0.0001);
    assertEquals(90.0, updated.getOrientation().getYawDegrees(), 0.0001);
    assertEquals(context.getReceivedTime(), updated.getMotionUpdatedAt());
  }

  @Test
  void heartbeatListener_updatesOperationalState() {
    TwinManager manager = new TwinManager();
    DroneTwin drone = new DroneTwin("mavlink:test:1");

    manager.registerTwin(drone, new TwinUpdateContext());

    Map<String, Object> fields = new HashMap<>();
    fields.put("type", 2);
    fields.put("autopilot", 3);
    fields.put("base_mode", 128);
    fields.put("custom_mode", 4L);
    fields.put("system_status", 3);
    fields.put("mavlink_version", 3);

    ProcessedFrame frame = new ProcessedFrame(
        "HEARTBEAT",
        null,
        fields,
        true,
        List.of()
    );

    HeartbeatPacket packet = new HeartbeatPacket(frame);

    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.now());

    HeartbeatListener listener = new HeartbeatListener(manager);
    listener.handle("mavlink:test:1", packet, context);

    DroneTwin updated = (DroneTwin) manager.getTwin("mavlink:test:1").orElseThrow();

    assertEquals("2", updated.getVehicleClass());
    assertTrue(updated.getArmed());
    assertEquals("4", updated.getFlightMode());
    assertNotNull(updated.getLinkState());
    assertTrue(Boolean.TRUE.equals(updated.getLinkState().getConnected()));
    assertEquals("CONNECTED", updated.getLinkState().getState());
    assertEquals(context.getReceivedTime(), updated.getConnectivityUpdatedAt());
  }

  @Test
  void sysStatusListener_updatesBatteryState() {
    TwinManager manager = new TwinManager();
    DroneTwin drone = new DroneTwin("mavlink:test:1");

    manager.registerTwin(drone, new TwinUpdateContext());

    Map<String, Object> fields = new HashMap<>();
    fields.put("voltage_battery", 15200);
    fields.put("current_battery", 2350);
    fields.put("battery_remaining", 78);

    ProcessedFrame frame = new ProcessedFrame(
        "SYS_STATUS",
        null,
        fields,
        true,
        List.of()
    );

    SysStatusPacket packet = new SysStatusPacket(frame);

    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.now());

    SysStatusListener listener = new SysStatusListener(manager);
    listener.handle("mavlink:test:1", packet, context);

    DroneTwin updated = (DroneTwin) manager.getTwin("mavlink:test:1").orElseThrow();

    assertNotNull(updated.getBatteryState());
    assertEquals(15.2, updated.getBatteryState().getVoltageVolts(), 0.0001);
    assertEquals(23.5, updated.getBatteryState().getCurrentAmps(), 0.0001);
    assertEquals(78.0, updated.getBatteryState().getPercentage(), 0.0001);
    assertEquals(context.getReceivedTime(), updated.getPowerUpdatedAt());
  }

  @Test
  void gpsRawIntListener_updatesFixInformation() {
    TwinManager manager = new TwinManager();
    DroneTwin drone = new DroneTwin("mavlink:test:1");

    manager.registerTwin(drone, new TwinUpdateContext());

    Map<String, Object> fields = new HashMap<>();
    fields.put("fix_type", 3);
    fields.put("satellites_visible", 14);
    fields.put("eph", 95);
    fields.put("epv", 140);

    ProcessedFrame frame = new ProcessedFrame(
        "GPS_RAW_INT",
        null,
        fields,
        true,
        List.of()
    );

    GpsRawIntPacket packet = new GpsRawIntPacket(frame);

    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.now());

    GpsRawIntListener listener = new GpsRawIntListener(manager);
    listener.handle("mavlink:test:1", packet, context);

    DroneTwin updated = (DroneTwin) manager.getTwin("mavlink:test:1").orElseThrow();

    assertNotNull(updated.getFixInfo());
    assertEquals("3D", updated.getFixInfo().getFixType());
    assertEquals(14, updated.getFixInfo().getSatelliteCount());
    assertEquals(0.95, updated.getFixInfo().getHdop(), 0.0001);
    assertEquals(1.40, updated.getFixInfo().getVdop(), 0.0001);
    assertTrue(Boolean.TRUE.equals(updated.getGpsValid()));
    assertEquals(context.getReceivedTime(), updated.getNavigationUpdatedAt());
  }

  @Test
  void systemTimeListener_updatesTimeState() {
    TwinManager manager = new TwinManager();
    DroneTwin drone = new DroneTwin("mavlink:test:1");

    manager.registerTwin(drone, new TwinUpdateContext());

    Map<String, Object> fields = new HashMap<>();
    fields.put("time_unix_usec", 1713168000000000L);
    fields.put("time_boot_ms", 123456L);

    ProcessedFrame frame = new ProcessedFrame(
        "SYSTEM_TIME",
        null,
        fields,
        true,
        List.of()
    );

    SystemTimePacket packet = new SystemTimePacket(frame);

    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.ofEpochMilli(1713168001000L));

    SystemTimeListener listener = new SystemTimeListener(manager);
    listener.handle("mavlink:test:1", packet, context);

    DroneTwin updated = (DroneTwin) manager.getTwin("mavlink:test:1").orElseThrow();

    assertNotNull(updated.getTimeState());
    assertEquals(1713168000000L, updated.getTimeState().getGpsTimeEpochMillis());
    assertEquals(123456L, updated.getTimeState().getSystemTimeEpochMillis());
    assertTrue(Boolean.TRUE.equals(updated.getTimeState().getGpsTimeValid()));
    assertEquals(1000.0, updated.getTimeState().getTimeOffsetMillis(), 0.0001);
  }

  @Test
  void altitudeListener_updatesAltitudeFields() {
    TwinManager manager = new TwinManager();
    DroneTwin drone = new DroneTwin("mavlink:test:1");

    manager.registerTwin(drone, new TwinUpdateContext());

    Map<String, Object> fields = new HashMap<>();
    fields.put("altitude_amsl", 123.45);
    fields.put("altitude_relative", 18.75);

    ProcessedFrame frame = new ProcessedFrame(
        "ALTITUDE",
        null,
        fields,
        true,
        List.of()
    );

    AltitudePacket packet = new AltitudePacket(frame);

    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.now());

    AltitudeListener listener = new AltitudeListener(manager);
    listener.handle("mavlink:test:1", packet, context);

    DroneTwin updated = (DroneTwin) manager.getTwin("mavlink:test:1").orElseThrow();

    assertNotNull(updated.getGeoPosition());
    assertEquals(123.45, updated.getGeoPosition().getAltitudeMslMeters(), 0.0001);
    assertEquals(18.75, updated.getGeoPosition().getAltitudeAglMeters(), 0.0001);
    assertEquals(context.getReceivedTime(), updated.getNavigationUpdatedAt());
  }

  @Test
  void extendedSysStateListener_updatesOperationalStates() {
    TwinManager manager = new TwinManager();
    DroneTwin drone = new DroneTwin("mavlink:test:1");

    manager.registerTwin(drone, new TwinUpdateContext());

    Map<String, Object> fields = new HashMap<>();
    fields.put("landed_state", 2);
    fields.put("vtol_state", 4);

    ProcessedFrame frame = new ProcessedFrame(
        "EXTENDED_SYS_STATE",
        null,
        fields,
        true,
        List.of()
    );

    ExtendedSysStatePacket packet = new ExtendedSysStatePacket(frame);

    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.now());

    ExtendedSysStateListener listener = new ExtendedSysStateListener(manager);
    listener.handle("mavlink:test:1", packet, context);

    DroneTwin updated = (DroneTwin) manager.getTwin("mavlink:test:1").orElseThrow();

    assertEquals("IN_AIR", updated.getLandedState());
    assertEquals("MC", updated.getVtolState());
    assertEquals(context.getReceivedTime(), updated.getOperationalUpdatedAt());
  }

  @Test
  void missionCurrentListener_updatesMissionSequence() {
    TwinManager manager = new TwinManager();
    DroneTwin drone = new DroneTwin("mavlink:test:1");

    manager.registerTwin(drone, new TwinUpdateContext());

    Map<String, Object> fields = new HashMap<>();
    fields.put("seq", 7);

    ProcessedFrame frame = new ProcessedFrame(
        "MISSION_CURRENT",
        null,
        fields,
        true,
        List.of()
    );

    MissionCurrentPacket packet = new MissionCurrentPacket(frame);

    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.now());

    MissionCurrentListener listener = new MissionCurrentListener(manager);
    listener.handle("mavlink:test:1", packet, context);

    DroneTwin updated = (DroneTwin) manager.getTwin("mavlink:test:1").orElseThrow();

    assertEquals(7, updated.getCurrentMissionSequence());
    assertEquals("MISSION_ACTIVE", updated.getMissionState());
    assertEquals(context.getReceivedTime(), updated.getOperationalUpdatedAt());
  }
}
