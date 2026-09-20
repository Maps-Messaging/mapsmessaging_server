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

package io.mapsmessaging.state.n2k.msg.source;

import io.mapsmessaging.state.n2k.msg.AisClassBStaticDataPartBReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AisClassBStaticDataPartBFieldValueSourceTest {

  @Test
  void reportFieldsAreExposedWithN2kNames() {
    AisClassBStaticDataPartBReport report = new AisClassBStaticDataPartBReport();
    report.setMessageId(24L);
    report.setRepeatIndicator(0L);
    report.setUserId(123456789L);
    report.setTypeOfShip(55L);
    report.setVendorId("MAPS");
    report.setCallsign("BOAT01");
    report.setLength(6.0);
    report.setBeam(2.0);
    report.setPositionReferenceFromStarboard(1.0);
    report.setPositionReferenceFromBow(2.0);
    report.setMothershipUserId(0L);
    report.setGnssType(1L);
    report.setAisTransceiverInformation(0L);
    report.setSequenceId(3L);

    AisClassBStaticDataPartBFieldValueSource source =
        new AisClassBStaticDataPartBFieldValueSource(report);

    assertEquals(24L, source.getLong("messageId"));
    assertEquals(123456789L, source.getLong("userId"));
    assertEquals(55L, source.getLong("typeOfShipAndCargo"));
    assertEquals("MAPS", source.getString("vendorId"));
    assertEquals("BOAT01", source.getString("callSign"));
    assertEquals(6.0, source.getDouble("shipLength"), 0.0);
    assertEquals(2.0, source.getDouble("shipBeam"), 0.0);
    assertEquals(3L, source.getLong("sequenceId"));
  }

  @Test
  void nullAndEmptyValuesAreNotExposed() {
    AisClassBStaticDataPartBReport report = new AisClassBStaticDataPartBReport();
    report.setMessageId(24L);
    report.setVendorId("");
    report.setCallsign(null);

    AisClassBStaticDataPartBFieldValueSource source =
        new AisClassBStaticDataPartBFieldValueSource(report);

    assertTrue(source.has("messageId"));
    assertFalse(source.has("vendorId"));
    assertFalse(source.has("callSign"));
    assertFalse(source.has("shipLength"));
  }
}
