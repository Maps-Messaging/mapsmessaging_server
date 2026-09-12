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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.canbus.j1939.n2k.codec.FieldValueSource;
import io.mapsmessaging.state.n2k.msg.AisClassBExtendedPositionReport;
import io.mapsmessaging.state.n2k.msg.AisClassBPositionReport;
import io.mapsmessaging.state.n2k.msg.AisClassBStaticDataPartAReport;
import io.mapsmessaging.state.n2k.msg.AisClassBStaticDataPartBReport;
import org.junit.jupiter.api.Test;

class AisFieldValueSourceTest {

  @Test
  void positionSource_completeReport_exposesExpectedProtocolFieldIds() {
    AisClassBPositionReport report = new AisClassBPositionReport(
        18L,
        0L,
        123_456_789L,
        151.2d,
        -33.8d,
        1L,
        0L,
        5L,
        1.5d,
        2.5d,
        3L,
        4L,
        0.75d,
        0L,
        0L,
        1L,
        0L,
        1L,
        0L,
        1L,
        0L,
        1L);

    FieldValueSource source = new AisClassBPositionFieldValueSource(report);

    assertEquals(18L, source.getLong("messageId"));
    assertEquals(123_456_789L, source.getLong("userId"));
    assertEquals(151.2d, source.getDouble("longitude"));
    assertEquals(-33.8d, source.getDouble("latitude"));
    assertEquals(1.5d, source.getDouble("cog"));
    assertEquals(2.5d, source.getDouble("sog"));
    assertEquals(0.75d, source.getDouble("trueHeading"));
    assertEquals(1L, source.getLong("communicationStateSelectorFlag"));
    assertTrue(source.has("classBUnitFlag"));
    assertNull(source.getString("messageId"));
  }

  @Test
  void positionSource_nullOptionalFields_areAbsentWhileZeroIsPresent() {
    AisClassBPositionReport report = new AisClassBPositionReport();
    report.setMessageId(18L);
    report.setRepeatIndicator(0L);
    report.setUserId(123_456_789L);
    report.setLongitude(0.0d);
    report.setLatitude(null);
    report.setSog(null);
    report.setHeading(null);

    FieldValueSource source = new AisClassBPositionFieldValueSource(report);

    assertTrue(source.has("repeatIndicator"));
    assertEquals(0L, source.getLong("repeatIndicator"));
    assertTrue(source.has("longitude"));
    assertEquals(0.0d, source.getDouble("longitude"));
    assertFalse(source.has("latitude"));
    assertFalse(source.has("sog"));
    assertFalse(source.has("trueHeading"));
    assertNull(source.getLong("latitude"));
  }

  @Test
  void extendedPositionSource_partialReport_omitsNullAndEmptyOptionalFields() {
    AisClassBExtendedPositionReport report = new AisClassBExtendedPositionReport();
    report.setMessageId(19L);
    report.setUserId(123_456_789L);
    report.setLongitude(0.0d);
    report.setLatitude(-33.8d);
    report.setName("");
    report.setLength(null);
    report.setDte(0L);

    FieldValueSource source = new AisClassBExtendedPositionFieldValueSource(report);

    assertEquals(19L, source.getLong("messageId"));
    assertEquals(0.0d, source.getDouble("longitude"));
    assertEquals(-33.8d, source.getDouble("latitude"));
    assertFalse(source.has("name"));
    assertFalse(source.has("shipLength"));
    assertTrue(source.has("dataTerminalEquipmentDte"));
    assertEquals(0L, source.getLong("dataTerminalEquipmentDte"));
  }

  @Test
  void staticPartASource_exposesStringAndOmitsNullSequence() {
    AisClassBStaticDataPartAReport report = new AisClassBStaticDataPartAReport(
        24L,
        0L,
        123_456_789L,
        "SURVEY ONE",
        2L,
        null);

    FieldValueSource source = new AisClassBStaticDataPartAFieldValueSource(report);

    assertEquals("SURVEY ONE", source.getString("name"));
    assertEquals(2L, source.getLong("aisTransceiverInformation"));
    assertFalse(source.has("sequenceId"));
  }

  @Test
  void staticPartBSource_partialReport_preservesFieldNamesAndOptionalAbsence() {
    AisClassBStaticDataPartBReport report = new AisClassBStaticDataPartBReport();
    report.setMessageId(24L);
    report.setRepeatIndicator(0L);
    report.setUserId(123_456_789L);
    report.setTypeOfShip(55L);
    report.setVendorId("MAPS");
    report.setCallsign(null);
    report.setLength(1.0d);
    report.setBeam(0.0d);
    report.setMothershipUserId(null);
    report.setSequenceId(0L);

    FieldValueSource source = new AisClassBStaticDataPartBFieldValueSource(report);

    assertEquals(55L, source.getLong("typeOfShipAndCargo"));
    assertEquals("MAPS", source.getString("vendorId"));
    assertFalse(source.has("callSign"));
    assertEquals(1.0d, source.getDouble("shipLength"));
    assertEquals(0.0d, source.getDouble("shipBeam"));
    assertFalse(source.has("motherShipMmsi"));
    assertTrue(source.has("sequenceId"));
    assertEquals(0L, source.getLong("sequenceId"));
  }
}
