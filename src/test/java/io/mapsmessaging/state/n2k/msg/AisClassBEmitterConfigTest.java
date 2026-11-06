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

package io.mapsmessaging.state.n2k.msg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AisClassBEmitterConfigTest {

  @Test
  void defaultsDefineCompleteClassBEmissionProfile() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();

    assertEquals(0L, config.getSequenceId());
    assertEquals(0L, config.getMothershipUserId());
    assertEquals(0L, config.getRepeatIndicator());
    assertEquals(1L, config.getPositionAccuracy());
    assertEquals(0L, config.getRaim());
    assertEquals(0L, config.getCommunicationStateInformation());
    assertEquals(0L, config.getAisTransceiverInformation());
    assertEquals(0L, config.getUnitType());
    assertEquals(0L, config.getIntegratedDisplay());
    assertEquals(0L, config.getDsc());
    assertEquals(1L, config.getBand());
    assertEquals(0L, config.getCanHandleMsg22());
    assertEquals(0L, config.getAisMode());
    assertEquals(0L, config.getAisCommunicationState());
    assertEquals(55L, config.getShipType());
    assertEquals(1L, config.getGnssType());
    assertEquals(0L, config.getDte());
    assertEquals(1.0, config.getLengthMeters(), 0.0);
    assertEquals(1.0, config.getBeamMeters(), 0.0);
    assertEquals(0.5, config.getPositionReferenceFromStarboardMeters(), 0.0);
    assertEquals(0.5, config.getPositionReferenceFromBowMeters(), 0.0);
    assertEquals("MAPS", config.getVendorId());
    assertEquals("DRONE", config.getCallsign());
  }

  @Test
  void freshConfigHasNoImplicitOverrides() {
    AisClassBEmitterConfig config = new AisClassBEmitterConfig();

    assertNull(config.getRepeatIndicator());
    assertNull(config.getPositionAccuracy());
    assertNull(config.getName());
    assertNull(config.getVendorId());
    assertNull(config.getLengthMeters());
  }
}
