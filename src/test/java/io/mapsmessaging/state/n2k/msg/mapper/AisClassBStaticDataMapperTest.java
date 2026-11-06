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

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.n2k.msg.AisClassBEmitterConfig;
import io.mapsmessaging.state.n2k.msg.AisClassBStaticDataPartAReport;
import io.mapsmessaging.state.n2k.msg.AisClassBStaticDataPartBReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AisClassBStaticDataMapperTest {

  @Test
  void partAMapsIdentityNameAndSequence() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    config.setName("Configured Vessel");
    config.setSequenceId(12L);

    DroneTwin twin = new DroneTwin("vessel-a");
    twin.setMmsi(123456789L);

    AisClassBStaticDataPartAReport report =
        new AisClassBStaticDataPartAMapper(config).map(twin).orElseThrow();

    assertEquals(24L, report.getMessageId());
    assertEquals(123456789L, report.getUserId());
    assertEquals("Configured Vessel", report.getName());
    assertEquals(12L, report.getSequenceId());
    assertEquals(config.getAisTransceiverInformation(), report.getAisTransceiverInformation());
  }

  @Test
  void partBMapsVesselDetailsAndNormalisesIdentityStrings() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    config.setVendorId("maps-bv");
    config.setCallsign("fallback");
    config.setSequenceId(7L);

    DroneTwin twin = new DroneTwin("vessel-b");
    twin.setMmsi(987654321L);
    twin.setCallSign("boat-01");

    AisClassBStaticDataPartBReport report =
        new AisClassBStaticDataPartBMapper(config).map(twin).orElseThrow();

    assertEquals(24L, report.getMessageId());
    assertEquals(987654321L, report.getUserId());
    assertEquals(55L, report.getTypeOfShip());
    assertEquals("MAPS BV", report.getVendorId());
    assertEquals("boat 01", report.getCallsign());
    assertEquals(1.0, report.getLength(), 0.0);
    assertEquals(1.0, report.getBeam(), 0.0);
    assertEquals(7L, report.getSequenceId());
  }

  @Test
  void nullTwinOrMissingMmsiIsRejectedByBothMappers() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    AisClassBStaticDataPartAMapper partA = new AisClassBStaticDataPartAMapper(config);
    AisClassBStaticDataPartBMapper partB = new AisClassBStaticDataPartBMapper(config);

    assertTrue(partA.map(null).isEmpty());
    assertTrue(partB.map(null).isEmpty());

    DroneTwin twin = new DroneTwin("vessel");
    twin.setMmsi(null);

    assertTrue(partA.map(twin).isEmpty());
    assertTrue(partB.map(twin).isEmpty());
  }
}
