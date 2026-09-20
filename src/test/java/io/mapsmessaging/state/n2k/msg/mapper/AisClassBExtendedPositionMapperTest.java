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
import io.mapsmessaging.state.n2k.msg.AisClassBEmitterConfig;
import io.mapsmessaging.state.n2k.msg.AisClassBExtendedPositionReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AisClassBExtendedPositionMapperTest {

  @Test
  void eligibleTwinMapsToExtendedPositionReport() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    config.setName("Configured Name");
    DroneTwin twin = AisClassBPositionMapperTest.eligibleTwin();

    AisClassBExtendedPositionReport report =
        new AisClassBExtendedPositionMapper(config).map(twin).orElseThrow();

    assertEquals(19L, report.getMessageId());
    assertEquals(123456789L, report.getUserId());
    assertEquals(-9.1, report.getLongitude(), 0.000001);
    assertEquals(38.4, report.getLatitude(), 0.000001);
    assertEquals(5L, report.getTimeStamp());
    assertEquals(Math.toRadians(10.0), report.getCog(), 0.000001);
    assertEquals(Math.toRadians(350.0), report.getTrueHeading(), 0.000001);
    assertEquals(55L, report.getTypeOfShip());
    assertEquals(1L, report.getGnssType());
    assertEquals("Configured Name", report.getName());
    assertEquals(1.0, report.getLength(), 0.0);
    assertEquals(1.0, report.getBeam(), 0.0);
  }

  @Test
  void invalidTwinStateIsRejected() {
    AisClassBExtendedPositionMapper mapper =
        new AisClassBExtendedPositionMapper(AisClassBEmitterConfig.getDefaults());

    assertTrue(mapper.map(null).isEmpty());

    DroneTwin twin = AisClassBPositionMapperTest.eligibleTwin();
    twin.setLifecycleStatus(TwinLifecycleStatus.DISCONNECTED);
    assertTrue(mapper.map(twin).isEmpty());

    twin = AisClassBPositionMapperTest.eligibleTwin();
    twin.setGpsValid(false);
    assertTrue(mapper.map(twin).isEmpty());

    twin = AisClassBPositionMapperTest.eligibleTwin();
    twin.setNavigationUpdatedAt(null);
    assertTrue(mapper.map(twin).isEmpty());

    twin = AisClassBPositionMapperTest.eligibleTwin();
    twin.getGeoPosition().setLatitude(null);
    assertTrue(mapper.map(twin).isEmpty());
  }
}
