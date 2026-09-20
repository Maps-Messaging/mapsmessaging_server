/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.config.CotConfigDTO;
import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.model.DetectionEventType;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TakDetectionEventMapperTest {

  private static final UUID CONTACT_ID =
      UUID.fromString("74f36503-e9e5-47fa-92bf-d7a9915018c8");

  private final TakEventMapper mapper = new TakEventMapper();
  private final TakXmlSerialiser serialiser = new TakXmlSerialiser();

  @Test
  void mapsDetectionToStableCotEventWithMultipleVideoFeeds() {
    DroneTwin drone = new DroneTwin("USV-002");
    DetectionEvent detection =
        new DetectionEvent(CONTACT_ID, "DETECT", DetectionEventType.DETECTED);
    detection.setPosition(new GeoPosition(38.42886d, -9.10898d, 0.0d, null, null));
    detection.setTimestamp(Instant.parse("2026-09-20T10:00:00Z"));
    detection.setTtlMillis(60_000L);
    detection.addAttribute(
        "tak.videoUrls",
        List.of(
            "rtsp://ops-se.stickleback.ai:8554/optical_view",
            "rtsp://ops-se.stickleback.ai:8554/thermal_view",
            "https://ops-se.stickleback.ai/viewer"));

    TakEvent event = mapper.mapDetection(drone, detection, new TwinUpdateContext());

    assertEquals(CONTACT_ID.toString(), event.getUid());
    assertEquals("a-u-G", event.getType());
    assertEquals("2026-09-20T10:01:00Z", event.getStale());
    assertEquals(38.42886d, event.getPoint().getLat());
    assertEquals(-9.10898d, event.getPoint().getLon());
    assertEquals("DETECT", event.getDetail().getContact().getCallsign());
    assertEquals("USV-002", event.getDetail().getLinks().getFirst().getUid());
    assertEquals("p-p", event.getDetail().getLinks().getFirst().getRelation());
    assertEquals(2, event.getDetail().getVideos().size());

    String xml = serialiser.toXml(event);
    assertTrue(xml.contains("rtsp://ops-se.stickleback.ai:8554/optical_view"));
    assertTrue(xml.contains("rtsp://ops-se.stickleback.ai:8554/thermal_view"));
    assertTrue(!xml.contains("https://ops-se.stickleback.ai/viewer"));

    detection.setEventType(DetectionEventType.UPDATED);
    assertEquals(CONTACT_ID.toString(), mapper.mapDetection(drone, detection, null).getUid());
  }

  @Test
  void detectionUsesTheSourceBattleDimensionWithUnknownAffiliation() {
    DroneTwin usv = new DroneTwin("USV-002");
    usv.setVehicleClass(VehicleClass.USV);

    DroneTwin air = new DroneTwin("UAV-001");
    air.getDescription().put("symbol_set", "SymbolSetEnum_AIR");

    assertEquals("a-u-S", mapper.mapDetection(usv, detection(), null).getType());
    assertEquals("a-u-A", mapper.mapDetection(air, detection(), null).getType());
  }

  @Test
  void detectionPolicyPreservesTrackIdentityAndTtl() {
    DroneTwin drone = new DroneTwin("USV-002");
    drone.setVehicleClass(VehicleClass.USV);
    DetectionEvent detection = detection();
    TakEvent event = mapper.mapDetection(drone, detection, null);

    CotConfigDTO config = new CotConfigDTO();
    config.setHow("m-g");
    config.setUidPrefix("ignored-");
    config.setStaleTimeoutMillis(5_000L);
    config.setDefaultCircularErrorMeters(25.0d);
    config.setDefaultLinearErrorMeters(30.0d);
    config.setAltitudeSource("BARO");

    new CotEventPolicy().applyDetection(event, drone, config);

    assertEquals(CONTACT_ID.toString(), event.getUid());
    assertEquals("a-u-S", event.getType());
    assertEquals("2026-09-20T10:01:00Z", event.getStale());
    assertEquals("m-g", event.getHow());
    assertEquals(25.0d, event.getPoint().getCe());
    assertEquals(30.0d, event.getPoint().getLe());
    assertEquals("BARO", event.getDetail().getPrecisionLocation().getAltsrc());
    assertEquals("GPS", event.getDetail().getPrecisionLocation().getGeopointsrc());
  }

  @Test
  void supportsLegacySingleVideoUrlAttribute() {
    DroneTwin drone = new DroneTwin("USV-002");
    DetectionEvent detection =
        new DetectionEvent(CONTACT_ID, "DETECT", DetectionEventType.DETECTED);
    detection.setPosition(new GeoPosition(38.42886d, -9.10898d, 0.0d, null, null));
    detection.setTtlMillis(60_000L);
    detection.addAttribute("tak.videoUrl", "rtsp://ops-se.stickleback.ai:8554/optical_view");

    TakEvent event = mapper.mapDetection(drone, detection, null);

    assertEquals(1, event.getDetail().getVideos().size());
  }

  private static DetectionEvent detection() {
    DetectionEvent detection =
        new DetectionEvent(CONTACT_ID, "DETECT", DetectionEventType.DETECTED);
    detection.setPosition(new GeoPosition(38.42886d, -9.10898d, 0.0d, null, null));
    detection.setTimestamp(Instant.parse("2026-09-20T10:00:00Z"));
    detection.setTtlMillis(60_000L);
    return detection;
  }

  @Test
  void ignoresDetectionWithoutPosition() {
    DroneTwin drone = new DroneTwin("USV-002");
    DetectionEvent detection =
        new DetectionEvent(CONTACT_ID, "DETECT", DetectionEventType.DETECTED);
    detection.setTtlMillis(60_000L);

    assertNull(mapper.mapDetection(drone, detection, null));
  }
}
