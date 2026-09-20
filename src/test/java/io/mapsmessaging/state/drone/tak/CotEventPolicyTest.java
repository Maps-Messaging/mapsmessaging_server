package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakDetail;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import io.mapsmessaging.state.drone.tak.model.TakPoint;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CotEventPolicyTest {

  @Test
  void detectionPolicyAppliesDefaultHowErrorsAndPrecisionSource() {
    TakEvent event = new TakEvent();
    event.setPoint(new TakPoint());
    event.setDetail(new TakDetail());

    new CotEventPolicy().apply(event, new DroneTwin("source"), null, null);

    assertEquals("h-g-i-g-o", event.getHow());
    assertEquals(10.0, event.getPoint().getCe(), 0.0);
    assertEquals(15.0, event.getPoint().getLe(), 0.0);
    assertEquals("GPS", event.getDetail().getPrecisionLocation().getAltsrc());
    assertEquals("GPS", event.getDetail().getPrecisionLocation().getGeopointsrc());
  }

  @Test
  void standardPolicySetsDefaultsAndAdvancesStaleTime() {
    DroneTwin twin = new DroneTwin("drone-2");
    twin.setGeoPosition(new GeoPosition(1.0, 2.0, 3.0, null, null));

    TakEvent event = new TakEvent();
    event.setUid("drone-2");
    event.setType("a-f-A");
    event.setHow("old");
    event.setTime(Instant.parse("2026-09-20T12:00:00Z").toString());
    event.setPoint(new TakPoint());
    event.setDetail(new TakDetail());

    new CotEventPolicy().apply(event, twin, null, null);

    assertEquals("h-g-i-g-o", event.getHow());
    assertEquals(
        Instant.parse("2026-09-20T12:00:30Z").toString(),
        event.getStale());
    assertEquals("GPS", event.getDetail().getPrecisionLocation().getAltsrc());
  }

  @Test
  void nullInputsAreIgnored() {
    CotEventPolicy policy = new CotEventPolicy();

    assertDoesNotThrow(() -> policy.apply(null, new DroneTwin("d"), null, null));
    assertDoesNotThrow(() -> policy.apply(new TakEvent(), null, null, null));
  }
}
