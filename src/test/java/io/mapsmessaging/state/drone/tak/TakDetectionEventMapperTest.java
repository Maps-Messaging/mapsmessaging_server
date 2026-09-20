/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.model.DetectionEventType;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TakDetectionEventMapperTest {

  private static final UUID CONTACT_ID =
      UUID.fromString("74f36503-e9e5-47fa-92bf-d7a9915018c8");

  private final TakEventMapper mapper = new TakEventMapper();
  private final TakXmlSerialiser serialiser = new TakXmlSerialiser();

  @Test
  void mapsDetectionToStableCotEvent() {
    DroneTwin drone = new DroneTwin("USV-002");
    DetectionEvent detection =
        new DetectionEvent(CONTACT_ID, "target-1", DetectionEventType.DETECTED);
    detection.setPosition(new GeoPosition(38.42886d, -9.10898d, 0.0d, null, null));
    detection.setTimestamp(Instant.parse("2026-09-20T10:00:00Z"));
    detection.setTtlMillis(60_000L);
    detection.addAttribute("tak.videoUrl", "rtsp://ops-se.stickleback.ai/optical");

    TakEvent event = mapper.mapDetection(drone, detection, new TwinUpdateContext());

    assertEquals(CONTACT_ID.toString(), event.getUid());
    assertEquals("a-u-G", event.getType());
    assertEquals("2026-09-20T10:01:00Z", event.getStale());
    assertEquals(38.42886d, event.getPoint().getLat());
    assertEquals(-9.10898d, event.getPoint().getLon());
    assertEquals("target-1", event.getDetail().getContact().getCallsign());
    assertEquals("USV-002", event.getDetail().getLinks().getFirst().getUid());
    assertEquals("p-p", event.getDetail().getLinks().getFirst().getRelation());
    assertEquals(
        "rtsp://ops-se.stickleback.ai/optical",
        event.getDetail().getVideoUrl());
    assertEquals(
        true,
        serialiser
            .toXml(event)
            .contains("<__video url=\"rtsp://ops-se.stickleback.ai/optical\"/>"));

    detection.setEventType(DetectionEventType.UPDATED);
    assertEquals(CONTACT_ID.toString(), mapper.mapDetection(drone, detection, null).getUid());
  }

  @Test
  void ignoresNonRtspVideoUrl() {
    DroneTwin drone = new DroneTwin("USV-002");
    DetectionEvent detection =
        new DetectionEvent(CONTACT_ID, "target-1", DetectionEventType.DETECTED);
    detection.setPosition(new GeoPosition(38.42886d, -9.10898d, 0.0d, null, null));
    detection.setTtlMillis(60_000L);
    detection.addAttribute("tak.videoUrl", "https://ops-se.stickleback.ai/optical_view/");

    TakEvent event = mapper.mapDetection(drone, detection, null);

    assertNull(event.getDetail().getVideoUrl());
  }

  @Test
  void ignoresDetectionWithoutPosition() {
    DroneTwin drone = new DroneTwin("USV-002");
    DetectionEvent detection =
        new DetectionEvent(CONTACT_ID, "target-1", DetectionEventType.DETECTED);
    detection.setTtlMillis(60_000L);

    assertNull(mapper.mapDetection(drone, detection, null));
  }
}
