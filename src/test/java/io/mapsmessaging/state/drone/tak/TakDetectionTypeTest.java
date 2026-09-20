/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.model.DetectionEventType;
import io.mapsmessaging.state.drone.model.GeoPosition;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * What a detection renders as. A contact is unknown by definition -- nobody has classified it --
 * but the medium it was found in is not unknown: a boat detects things on the water. Typing every
 * detection as unknown GROUND puts sea contacts in the wrong dimension on the picture.
 */
class TakDetectionTypeTest {

  private final TakEventMapper mapper = new TakEventMapper();

  private static DetectionEvent detection() {
    DetectionEvent event = new DetectionEvent(UUID.randomUUID(), "DRONE_STAT", DetectionEventType.DETECTED);
    event.setPosition(new GeoPosition(38.41d, -9.07d, 4.3d, null));
    event.setTtlMillis(60_000L);
    event.setTimestamp(Instant.parse("2026-09-20T12:00:00Z"));
    return event;
  }

  private static DroneTwin source(String symbolSet) {
    DroneTwin twin = new DroneTwin("USV-002");
    twin.setGeoPosition(new GeoPosition(38.42d, -9.08d, 4.2d, null));
    twin.setLastSeenAt(Instant.parse("2026-09-20T12:00:00Z"));
    if (symbolSet != null) {
      twin.getDescription().putAll(Map.of("symbol_set", symbolSet));
    }
    return twin;
  }

  @Test
  void aDetectionTakesTheDimensionOfTheThingThatSawIt() {
    assertEquals("a-u-S",
        mapper.mapDetection(source("SymbolSetEnum_SEA_SURFACE"), detection(), new TwinUpdateContext()).getType());
    assertEquals("a-u-A",
        mapper.mapDetection(source("SymbolSetEnum_AIR"), detection(), new TwinUpdateContext()).getType());
    assertEquals("a-u-U",
        mapper.mapDetection(source("SEA_SUBSURFACE"), detection(), new TwinUpdateContext()).getType());
  }

  @Test
  void withoutADescriptionItStaysTheConfiguredDefault() {
    assertEquals("a-u-G", mapper.mapDetection(source(null), detection(), new TwinUpdateContext()).getType());
    assertEquals("a-u-G",
        mapper.mapDetection(source("not-a-symbol-set"), detection(), new TwinUpdateContext()).getType());
  }

  @Test
  void theRestOfTheEventIsUnchanged() {
    DetectionEvent event = detection();
    var mapped = mapper.mapDetection(source("SymbolSetEnum_SEA_SURFACE"), event, new TwinUpdateContext());
    assertEquals(event.getContactId().toString(), mapped.getUid());
    assertEquals("DRONE_STAT", mapped.getDetail().getContact().getCallsign());
    assertEquals("2026-09-20T12:01:00Z", mapped.getStale());
  }
}
