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

import io.mapsmessaging.state.n2k.msg.AisClassBExtendedPositionReport;
import io.mapsmessaging.state.n2k.msg.AisClassBPositionReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AisFieldValueSourceTest {

  @Test
  void positionSourceSeparatesLongAndDoubleFieldsAndOmitsNulls() {
    AisClassBPositionReport report = new AisClassBPositionReport();
    report.setMessageId(18L);
    report.setUserId(123456789L);
    report.setLatitude(38.4);
    report.setLongitude(-9.1);
    report.setPositionAccuracy(1L);
    report.setCog(Math.toRadians(90.0));
    report.setSog(4.5);
    report.setHeading(null);

    AisClassBPositionFieldValueSource source = new AisClassBPositionFieldValueSource(report);

    assertTrue(source.has("messageId"));
    assertEquals(18L, source.getLong("messageId"));
    assertEquals(123456789L, source.getLong("userId"));
    assertEquals(38.4, source.getDouble("latitude"), 0.0);
    assertEquals(-9.1, source.getDouble("longitude"), 0.0);
    assertEquals(1L, source.getLong("positionAccuracy"));
    assertEquals(4.5, source.getDouble("sog"), 0.0);

    assertFalse(source.has("trueHeading"));
    assertNull(source.getDouble("trueHeading"));
    assertNull(source.getString("messageId"));
    assertFalse(source.has("missing"));
  }

  @Test
  void extendedPositionSourceExposesNumericAndStringFields() {
    AisClassBExtendedPositionReport report = new AisClassBExtendedPositionReport();
    report.setMessageId(19L);
    report.setUserId(987654321L);
    report.setLatitude(1.5);
    report.setLongitude(2.5);
    report.setTypeOfShip(55L);
    report.setTrueHeading(Math.toRadians(180.0));
    report.setLength(6.0);
    report.setBeam(2.0);
    report.setName("SURVEY-1");
    report.setDte(0L);

    AisClassBExtendedPositionFieldValueSource source =
        new AisClassBExtendedPositionFieldValueSource(report);

    assertEquals(19L, source.getLong("messageId"));
    assertEquals(987654321L, source.getLong("userId"));
    assertEquals(1.5, source.getDouble("latitude"), 0.0);
    assertEquals(55L, source.getLong("shipcargoType"));
    assertEquals(Math.toRadians(180.0), source.getDouble("trueHeading"), 0.0);
    assertEquals(6.0, source.getDouble("shipLength"), 0.0);
    assertEquals(2.0, source.getDouble("shipBeam"), 0.0);
    assertEquals("SURVEY-1", source.getString("name"));
    assertEquals(0L, source.getLong("dataTerminalEquipmentDte"));
    assertFalse(source.has("missing"));
  }
}
