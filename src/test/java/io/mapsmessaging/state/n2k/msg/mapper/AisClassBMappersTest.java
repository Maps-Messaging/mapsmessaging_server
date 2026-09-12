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

package io.mapsmessaging.state.n2k.msg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.n2k.msg.AisClassBEmitterConfig;
import io.mapsmessaging.state.n2k.msg.AisClassBExtendedPositionReport;
import io.mapsmessaging.state.n2k.msg.AisClassBPositionReport;
import io.mapsmessaging.state.n2k.msg.AisClassBStaticDataPartAReport;
import io.mapsmessaging.state.n2k.msg.AisClassBStaticDataPartBReport;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AisClassBMappersTest {

  private static final double DELTA = 0.0000001d;

  @Test
  void positionMapper_completeTwin_mapsProtocolFieldsAndUnits() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    DroneTwin droneTwin = eligibleTwin();
    droneTwin.setCourseOverGroundDegrees(-90.0d);
    droneTwin.setHeadingDegrees(450.0d);
    droneTwin.setGroundSpeedMetersPerSecond(3.5d);

    AisClassBPositionReport report = new AisClassBPositionMapper(config).map(droneTwin).orElseThrow();

    assertEquals(18L, report.getMessageId());
    assertEquals(123_456_789L, report.getUserId());
    assertEquals(151.2093d, report.getLongitude());
    assertEquals(-33.8688d, report.getLatitude());
    assertEquals(1L, report.getPositionAccuracy());
    assertEquals(5L, report.getTimeStamp());
    assertEquals(Math.toRadians(270.0d), report.getCog(), DELTA);
    assertEquals(3.5d, report.getSog());
    assertEquals(Math.toRadians(90.0d), report.getHeading(), DELTA);
    assertEquals(0L, report.getRegionalApplication());
    assertEquals(0L, report.getRegionalApplicationB());
    assertEquals(config.getAisCommunicationState(), report.getAisCommunicationState());
  }

  @Test
  void positionMapper_nullOptionalMotionFields_remainAbsent() {
    DroneTwin droneTwin = eligibleTwin();
    droneTwin.setCourseOverGroundDegrees(null);
    droneTwin.setHeadingDegrees(null);
    droneTwin.setGroundSpeedMetersPerSecond(null);

    AisClassBPositionReport report = new AisClassBPositionMapper(new AisClassBEmitterConfig())
        .map(droneTwin)
        .orElseThrow();

    assertNull(report.getCog());
    assertNull(report.getHeading());
    assertNull(report.getSog());
    assertNull(report.getRepeatIndicator());
    assertEquals(1L, report.getPositionAccuracy());
  }

  @Test
  void positionMapper_invalidOptionalMotionFields_remainAbsent() {
    DroneTwin droneTwin = eligibleTwin();
    droneTwin.setCourseOverGroundDegrees(Double.POSITIVE_INFINITY);
    droneTwin.setHeadingDegrees(Double.NaN);
    droneTwin.setGroundSpeedMetersPerSecond(-0.1d);

    AisClassBPositionReport report = new AisClassBPositionMapper(AisClassBEmitterConfig.getDefaults())
        .map(droneTwin)
        .orElseThrow();

    assertNull(report.getCog());
    assertNull(report.getHeading());
    assertNull(report.getSog());
  }

  @Test
  void positionMapper_invalidCoreState_returnsEmpty() {
    AisClassBPositionMapper mapper = new AisClassBPositionMapper(AisClassBEmitterConfig.getDefaults());

    assertTrue(mapper.map(null).isEmpty());

    DroneTwin missingMmsi = eligibleTwin();
    missingMmsi.setMmsi(null);
    assertTrue(mapper.map(missingMmsi).isEmpty());

    DroneTwin invalidLatitude = eligibleTwin();
    invalidLatitude.getGeoPosition().setLatitude(91.0d);
    assertTrue(mapper.map(invalidLatitude).isEmpty());

    DroneTwin invalidLongitude = eligibleTwin();
    invalidLongitude.getGeoPosition().setLongitude(Double.NaN);
    assertTrue(mapper.map(invalidLongitude).isEmpty());

    DroneTwin gpsInvalid = eligibleTwin();
    gpsInvalid.setGpsValid(false);
    assertTrue(mapper.map(gpsInvalid).isEmpty());

    DroneTwin stale = eligibleTwin();
    stale.setLifecycleStatus(TwinLifecycleStatus.STALE);
    assertTrue(mapper.map(stale).isEmpty());

    DroneTwin noTimestamp = eligibleTwin();
    noTimestamp.setNavigationUpdatedAt(null);
    assertTrue(mapper.map(noTimestamp).isEmpty());
  }

  @Test
  void extendedPositionMapper_completeTwin_mapsStaticAndDynamicFields() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    config.setName(null);
    DroneTwin droneTwin = eligibleTwin();
    droneTwin.setDisplayName("Survey Vessel Alpha");
    droneTwin.setHeadingDegrees(180.0d);
    droneTwin.setCourseOverGroundDegrees(45.0d);
    droneTwin.setGroundSpeedMetersPerSecond(2.25d);

    AisClassBExtendedPositionReport report = new AisClassBExtendedPositionMapper(config)
        .map(droneTwin)
        .orElseThrow();

    assertEquals(19L, report.getMessageId());
    assertEquals(123_456_789L, report.getUserId());
    assertEquals(Math.toRadians(45.0d), report.getCog(), DELTA);
    assertEquals(2.25d, report.getSog());
    assertEquals(Math.PI, report.getTrueHeading(), DELTA);
    assertEquals("Survey Vessel Alpha", report.getName());
    assertEquals(config.getShipType(), report.getTypeOfShip());
    assertEquals(config.getLengthMeters(), report.getLength());
    assertEquals(config.getBeamMeters(), report.getBeam());
  }

  @Test
  void extendedPositionMapper_invalidCoreOrMotionFields_areRejectedOrOmitted() {
    AisClassBExtendedPositionMapper mapper = new AisClassBExtendedPositionMapper(AisClassBEmitterConfig.getDefaults());
    DroneTwin invalidCore = eligibleTwin();
    invalidCore.getGeoPosition().setLongitude(181.0d);
    assertTrue(mapper.map(invalidCore).isEmpty());

    DroneTwin validCore = eligibleTwin();
    validCore.setCourseOverGroundDegrees(Double.NaN);
    validCore.setHeadingDegrees(Double.NEGATIVE_INFINITY);
    validCore.setGroundSpeedMetersPerSecond(-1.0d);

    AisClassBExtendedPositionReport report = mapper.map(validCore).orElseThrow();
    assertNull(report.getCog());
    assertNull(report.getTrueHeading());
    assertNull(report.getSog());
  }

  @Test
  void staticDataPartA_usesNameFallbackAndDerivedSequence() {
    AisClassBEmitterConfig config = new AisClassBEmitterConfig();
    DroneTwin droneTwin = eligibleTwin();
    droneTwin.setDisplayName("Drone @ Alpha");
    droneTwin.setTwinId("drone-alpha");

    AisClassBStaticDataPartAReport report = new AisClassBStaticDataPartAMapper(config)
        .map(droneTwin)
        .orElseThrow();

    assertEquals(24L, report.getMessageId());
    assertEquals("Drone Alpha", report.getName());
    assertTrue(report.getSequenceId() >= 0L && report.getSequenceId() <= 252L);
    assertNull(report.getRepeatIndicator());
    assertTrue(new AisClassBStaticDataPartAMapper(config).map(null).isEmpty());
  }

  @Test
  void staticDataPartB_usesTwinCallsignThenConfiguredFallback() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    DroneTwin droneTwin = eligibleTwin();
    droneTwin.setCallSign("uxv-01");

    AisClassBStaticDataPartBReport report = new AisClassBStaticDataPartBMapper(config)
        .map(droneTwin)
        .orElseThrow();
    assertEquals("UXV 01", report.getCallsign());

    droneTwin.setCallSign(null);
    report = new AisClassBStaticDataPartBMapper(config).map(droneTwin).orElseThrow();
    assertEquals("DRONE", report.getCallsign());
    assertEquals("MAPS", report.getVendorId());
  }

  @Test
  void staticDataPartB_nullOptionalConfigFields_remainNull() {
    AisClassBEmitterConfig config = new AisClassBEmitterConfig();
    DroneTwin droneTwin = eligibleTwin();

    AisClassBStaticDataPartBReport report = new AisClassBStaticDataPartBMapper(config)
        .map(droneTwin)
        .orElseThrow();

    assertNull(report.getTypeOfShip());
    assertNull(report.getVendorId());
    assertNull(report.getCallsign());
    assertNull(report.getLength());
    assertNull(report.getBeam());
    assertNull(report.getMothershipUserId());
    assertFalse(new AisClassBStaticDataPartBMapper(config).map(new DroneTwin()).isPresent());
  }

  @Test
  void emitterDefaults_areInternallyConsistentForSmallClassBVessel() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();

    assertEquals(0L, config.getRepeatIndicator());
    assertEquals(1L, config.getPositionAccuracy());
    assertEquals(55L, config.getShipType());
    assertEquals(1.0d, config.getLengthMeters());
    assertEquals(1.0d, config.getBeamMeters());
    assertEquals(0.5d, config.getPositionReferenceFromStarboardMeters());
    assertEquals(0.5d, config.getPositionReferenceFromBowMeters());
    assertEquals("MAPS", config.getVendorId());
    assertEquals("DRONE", config.getCallsign());
  }

  private static DroneTwin eligibleTwin() {
    DroneTwin droneTwin = new DroneTwin();
    droneTwin.setTwinId("drone-alpha");
    droneTwin.setMmsi(123_456_789L);
    droneTwin.setLifecycleStatus(TwinLifecycleStatus.ACTIVE);
    droneTwin.setGpsValid(true);
    droneTwin.setGeoPosition(new GeoPosition(-33.8688d, 151.2093d, 12.0d, null));
    droneTwin.setNavigationUpdatedAt(Instant.ofEpochSecond(65L));
    return droneTwin;
  }
}
