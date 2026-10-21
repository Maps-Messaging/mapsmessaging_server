package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TakEventMapperTest {

  @Test
  void nullTwinOrMissingPositionCannotProduceTakTrack() {
    TakEventMapper mapper = new TakEventMapper();

    assertNull(mapper.map(null, null));
    assertNull(mapper.map(new DroneTwin("missing-position"), null));
  }

  @Test
  void positionedDroneMapsCoreCotEnvelopeAndPoint() {
    DroneTwin twin = new DroneTwin("drone-1");
    twin.setGeoPosition(new GeoPosition(38.4, -9.1, 125.0, null, null));
    twin.setLifecycleStatus(TwinLifecycleStatus.ACTIVE);
    twin.setCallSign("UAV-001");
    twin.setLastSeenAt(Instant.parse("2026-09-20T12:00:00Z"));

    TakEvent event = new TakEventMapper().map(twin, null);

    assertNotNull(event);
    assertNotNull(event.getUid());
    assertNotNull(event.getType());
    assertEquals("h-g-i-g-o", event.getHow());
    assertEquals(38.4, event.getPoint().getLat(), 0.0);
    assertEquals(-9.1, event.getPoint().getLon(), 0.0);
    assertEquals(125.0, event.getPoint().getHae(), 0.0);
    assertEquals("UAV-001", event.getDetail().getContact().getCallsign());
    assertEquals("ACTIVE", event.getDetail().getStatus().getLifecycle());
    assertNotNull(event.getDetail().getPrecisionLocation());
  }
}
