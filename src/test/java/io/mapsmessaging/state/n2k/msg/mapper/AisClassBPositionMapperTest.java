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

import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.n2k.msg.AisClassBEmitterConfig;
import io.mapsmessaging.state.n2k.msg.AisClassBPositionReport;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AisClassBPositionMapperTest {

  @Test
  void activeGpsTwinMapsToClassBPositionReport() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    config.setPositionAccuracy(null);
    DroneTwin twin = eligibleTwin();

    AisClassBPositionReport report =
        new AisClassBPositionMapper(config).map(twin).orElseThrow();

    assertEquals(18L, report.getMessageId());
    assertEquals(twin.getMmsi(), report.getUserId());
    assertEquals(-9.1, report.getLongitude(), 0.000001);
    assertEquals(38.4, report.getLatitude(), 0.000001);
    assertEquals(1L, report.getPositionAccuracy());
    assertEquals(5L, report.getTimeStamp());
    assertEquals(Math.toRadians(10.0), report.getCog(), 0.000001);
    assertEquals(Math.toRadians(350.0), report.getHeading(), 0.000001);
    assertEquals(4.2, report.getSog(), 0.000001);
    assertEquals(0L, report.getRegionalApplication());
    assertEquals(0L, report.getRegionalApplicationB());
  }

  @Test
  void configuredPositionAccuracyOverridesGpsDerivedValue() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    config.setPositionAccuracy(0L);

    assertEquals(
        0L,
        new AisClassBPositionMapper(config).map(eligibleTwin()).orElseThrow().getPositionAccuracy()
    );
  }

  @Test
  void ineligibleTwinsAreRejected() {
    AisClassBPositionMapper mapper = new AisClassBPositionMapper(AisClassBEmitterConfig.getDefaults());

    assertTrue(mapper.map(null).isEmpty());

    DroneTwin twin = eligibleTwin();
    twin.setLifecycleStatus(TwinLifecycleStatus.STALE);
    assertTrue(mapper.map(twin).isEmpty());

    twin = eligibleTwin();
    twin.setGpsValid(false);
    assertTrue(mapper.map(twin).isEmpty());

    twin = eligibleTwin();
    twin.setNavigationUpdatedAt(null);
    assertTrue(mapper.map(twin).isEmpty());

    twin = eligibleTwin();
    twin.setGeoPosition(null);
    assertTrue(mapper.map(twin).isEmpty());

    twin = eligibleTwin();
    twin.setMmsi(null);
    assertTrue(mapper.map(twin).isEmpty());
  }

  static DroneTwin eligibleTwin() {
    DroneTwin twin = new DroneTwin("vessel-1");
    twin.setMmsi(123456789L);
    twin.setLifecycleStatus(TwinLifecycleStatus.ACTIVE);
    twin.setGeoPosition(new GeoPosition(38.4, -9.1, 0.0, null, null));
    twin.setGpsValid(true);
    twin.setNavigationUpdatedAt(Instant.ofEpochSecond(65));
    twin.setCourseOverGroundDegrees(370.0);
    twin.setHeadingDegrees(-10.0);
    twin.setGroundSpeedMetersPerSecond(4.2);
    twin.setDisplayName("Survey Vessel");
    twin.setCallSign("SV001");
    return twin;
  }
}
