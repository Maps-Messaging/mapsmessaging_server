/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ContactTakPolicyTest {

  private final TakEventMapper mapper = new TakEventMapper();
  private final CotEventPolicy policy = new CotEventPolicy();
  private final TakXmlSerialiser serialiser = new TakXmlSerialiser();

  @Test
  void appliesAuthoritativeContactTakContract() {
    ContactTwin twin = contact();

    TakEvent event = mapper.map(twin, new TwinUpdateContext());
    policy.apply(event, twin, null, null);

    assertEquals("a-u-U", event.getType());
    assertEquals("m-g", event.getHow());
    assertEquals("2026-09-12T11:00:00Z", event.getStale());
    assertEquals(9_999_999.0d, event.getPoint().getHae());
    assertEquals("MILCO-690", event.getDetail().getContact().getCallsign());
    assertEquals(Boolean.TRUE, event.getDetail().getArchive());
    assertEquals(-65536, event.getDetail().getColorArgb());
    assertEquals("GPS", event.getDetail().getPrecisionLocation().getGeopointsrc());
    assertTrue(event.getDetail().getRemarks().contains("Tasking: INSPECT (IDENTIFY_MCM)"));
    assertTrue(event.getDetail().getRemarks().contains("probability=99.9%"));
    assertTrue(event.getDetail().getRemarks().contains("depth=12.50 m"));
    assertTrue(event.getDetail().getRemarks().contains("size=1.80 x 0.70 m"));
    assertTrue(event.getDetail().getRemarks().contains("source=Kraken-V2"));

    String xml = serialiser.toXml(event);
    assertTrue(xml.contains("<archive/>"));
    assertTrue(xml.contains("<color argb=\"-65536\"/>"));
    assertTrue(xml.contains("geopointsrc=\"GPS\""));
  }

  @Test
  void removalIncludesLastKnownPoint() {
    ContactTwin twin = contact();

    TakEvent event = mapper.mapRemoval(twin, new TwinUpdateContext());
    policy.applyRemoval(event, twin, null, null);

    assertNotNull(event.getPoint());
    assertEquals(38.42886d, event.getPoint().getLat());
    assertEquals(-9.10898d, event.getPoint().getLon());
    assertEquals(9_999_999.0d, event.getPoint().getHae());
    assertTrue(serialiser.toXml(event).contains("<point"));
  }

  private ContactTwin contact() {
    ContactTwin twin = new ContactTwin("milco:kraken-v2:milco:690");
    twin.setGeoPosition(new GeoPosition(38.42886d, -9.10898d, null, null, null));
    twin.setLastSeenAt(Instant.parse("2026-09-12T10:00:00Z"));
    twin.getAttributes().put("contactCategory", "MILCO");
    twin.getAttributes().put("sourceDetectionId", "690");
    twin.getAttributes().put("sourceSensor", "Kraken-V2");
    twin.getAttributes().put("probabilityDisplay", "99.9%");
    twin.getAttributes().put("depthDisplay", "12.50 m");
    twin.getAttributes().put("sizeDisplay", "1.80 x 0.70 m");
    twin.getAttributes().put("requestedTaskType", "INSPECT");
    twin.getAttributes().put("requestedTaskSpecialization", "IDENTIFY_MCM");
    return twin;
  }

  private static final class ContactTwin extends EntityTwin {
    private ContactTwin(String twinId) {
      super(twinId, null);
      setTwinType(TwinType.CONTACT);
    }
  }
}
