/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CotToTwinMapperTest {

  private final CotToTwinMapper mapper = new CotToTwinMapper();

  @Test
  void mapsIdentityPositionContactTrackAndOriginalType() {
    DroneTwin twin = mapper.map(cot(
        "UAS-1",
        "a-f-A-M-F-Q",
        "EAGLE",
        "38.123",
        "-9.456",
        "120.5",
        "12.5",
        "270.0"));

    assertEquals("UAS-1", twin.getTwinId());
    assertEquals("a-f-A-M-F-Q", twin.getAttributes().get(CotToTwinMapper.ORIGINAL_COT_TYPE_ATTRIBUTE));
    assertEquals("EAGLE", twin.getCallSign());
    assertEquals("EAGLE", twin.getDisplayName());
    assertEquals(38.123, twin.getGeoPosition().getLatitude());
    assertEquals(-9.456, twin.getGeoPosition().getLongitude());
    assertEquals(120.5, twin.getGeoPosition().getAltitudeMslMeters());
    assertEquals(12.5, twin.getGroundSpeedMetersPerSecond());
    assertEquals(270.0, twin.getCourseOverGroundDegrees());
  }

  @Test
  void routeUpdatesExistingTwinRatherThanReplacingIt() {
    TwinManager twinManager = new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
    DroneTwin existing = new DroneTwin("UAS-1");
    existing.setGeoPosition(new GeoPosition(0.0, 0.0, 0.0, null));
    twinManager.registerTwin(existing, null);

    assertTrue(mapper.routeToTwinManager(
        twinManager,
        cot("UAS-1", "a-f-A-M-F-Q", "UPDATED", "38.0", "-9.0", "50", "5", "90"),
        "cot-test"));

    DroneTwin updated = (DroneTwin) twinManager.getTwin("UAS-1").orElseThrow();
    assertSame(existing, updated);
    assertEquals("UPDATED", updated.getCallSign());
    assertEquals(38.0, updated.getGeoPosition().getLatitude());
    assertEquals(5.0, updated.getGroundSpeedMetersPerSecond());
    assertEquals(90.0, updated.getCourseOverGroundDegrees());
  }

  @Test
  void missingUidAndMalformedXmlAreRejected() {
    assertNull(mapper.map("<event type=\"a-f-A\"><point lat=\"1\" lon=\"2\"/></event>"
        .getBytes(StandardCharsets.UTF_8)));
    assertFalse(mapper.routeToTwinManager(
        new TwinManager(),
        "<event".getBytes(StandardCharsets.UTF_8),
        "cot-test"));
  }

  @Test
  void doctypeAndExternalEntityInputIsRejected() {
    String xml = """
        <!DOCTYPE event [
          <!ENTITY xxe SYSTEM "file:///etc/passwd">
        ]>
        <event uid="UAS-1" type="a-f-A">
          <point lat="1" lon="2" hae="3"/>
          <detail><contact callsign="&xxe;"/></detail>
        </event>
        """;

    assertNull(mapper.map(xml.getBytes(StandardCharsets.UTF_8)));
  }

  private byte[] cot(
      String uid,
      String type,
      String callsign,
      String lat,
      String lon,
      String hae,
      String speed,
      String course) {
    String xml = """
        <event uid="%s" type="%s">
          <point lat="%s" lon="%s" hae="%s"/>
          <detail>
            <contact callsign="%s"/>
            <track speed="%s" course="%s"/>
          </detail>
        </event>
        """.formatted(uid, type, lat, lon, hae, callsign, speed, course);
    return xml.getBytes(StandardCharsets.UTF_8);
  }
}
