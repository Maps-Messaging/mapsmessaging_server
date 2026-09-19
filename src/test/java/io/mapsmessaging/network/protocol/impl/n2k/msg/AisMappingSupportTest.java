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

package io.mapsmessaging.network.protocol.impl.n2k.msg;

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AisMappingSupportTest {

  @Test
  void corePositionRequiresTwinMmsiLatitudeAndLongitude() {
    assertFalse(AisMappingSupport.hasCorePosition(null));

    DroneTwin twin = new DroneTwin("vessel-1");
    assertFalse(AisMappingSupport.hasCorePosition(twin));

    twin.setGeoPosition(new GeoPosition());
    assertFalse(AisMappingSupport.hasCorePosition(twin));

    twin.getGeoPosition().setLatitude(38.4);
    assertFalse(AisMappingSupport.hasCorePosition(twin));

    twin.getGeoPosition().setLongitude(-9.1);
    assertTrue(AisMappingSupport.hasCorePosition(twin));

    twin.setMmsi(null);
    assertFalse(AisMappingSupport.hasCorePosition(twin));
  }

  @Test
  void secondOfMinuteHandlesNullPositiveAndPreEpochTimes() {
    assertNull(AisMappingSupport.toSecondOfMinute(null));
    assertEquals(5L, AisMappingSupport.toSecondOfMinute(Instant.ofEpochSecond(65)));
    assertEquals(59L, AisMappingSupport.toSecondOfMinute(Instant.ofEpochSecond(-1)));
  }

  @Test
  void degreeNormalizationAndRadiansHandleWrapAround() {
    assertEquals(0.0, AisMappingSupport.normalizeDegrees(0.0), 0.0);
    assertEquals(10.0, AisMappingSupport.normalizeDegrees(370.0), 0.0);
    assertEquals(350.0, AisMappingSupport.normalizeDegrees(-10.0), 0.0);
    assertEquals(0.0, AisMappingSupport.normalizeDegrees(720.0), 0.0);

    assertNull(AisMappingSupport.toRadians(null));
    assertEquals(Math.toRadians(350.0), AisMappingSupport.toRadians(-10.0), 0.0000001);
  }

  @Test
  void sequenceIdUsesConfiguredValueOrDeterministicTwinHash() {
    DroneTwin twin = new DroneTwin("vessel-alpha");

    assertEquals(17L, AisMappingSupport.deriveSequenceId(twin, 17L));
    assertEquals(0L, AisMappingSupport.deriveSequenceId(null, null));
    assertEquals(
        (long) Math.floorMod("vessel-alpha".hashCode(), 253),
        AisMappingSupport.deriveSequenceId(twin, null)
    );
  }

  @Test
  void resolveNameUsesConfiguredThenDisplayRegistrationAndTwinId() {
    DroneTwin twin = new DroneTwin("fallback-id");
    twin.setDisplayName("Display Name");
    twin.setRegistrationId("REG-123");

    assertEquals("Configured Name", AisMappingSupport.resolveName(twin, "Configured Name"));
    assertEquals("Display Name", AisMappingSupport.resolveName(twin, null));

    twin.setDisplayName(" ");
    assertEquals("REG 123", AisMappingSupport.resolveName(twin, null));

    twin.setRegistrationId("");
    assertEquals("fallback id", AisMappingSupport.resolveName(twin, null));

    assertNull(AisMappingSupport.resolveName(null, null));
  }

  @Test
  void vendorAndCallsignAreSanitisedAndTruncated() {
    assertNull(AisMappingSupport.resolveVendorId(null));
    assertNull(AisMappingSupport.resolveVendorId(" "));
    assertEquals("VENDOR1", AisMappingSupport.resolveVendorId("vendor1"));
    assertEquals("ABCDEFG", AisMappingSupport.resolveVendorId("abcdefghij"));

    assertEquals("CALL 12", AisMappingSupport.resolveCallsign(null, "call-12"));
    assertNull(AisMappingSupport.resolveCallsign(null, null));

    DroneTwin twin = new DroneTwin("vessel");
    twin.setCallSign("boat-01");
    assertEquals("boat 01", AisMappingSupport.resolveCallsign(twin, "ignored"));

    twin.setCallSign(" ");
    assertNull(AisMappingSupport.resolveCallsign(twin, "configured"));
  }

  @Test
  void truncateNormalisesUnsupportedCharactersAndLength() {
    assertNull(AisMappingSupport.truncate(null, 10));
    assertEquals("ABC 123", AisMappingSupport.truncate("  ABC---123  ", 20));
    assertEquals("12345", AisMappingSupport.truncate("123456789", 5));
  }
}
