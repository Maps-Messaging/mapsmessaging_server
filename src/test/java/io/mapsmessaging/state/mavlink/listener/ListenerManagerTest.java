package io.mapsmessaging.state.mavlink.listener;

import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.packet.HomePositionPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ListenerManagerTest {

  @Test
  void registeredHomePositionListenerMutatesTwinState() {
    TwinManager twinManager = new TwinManager();
    DroneTwin twin = new DroneTwin("drone-1");
    TwinUpdateContext registration = new TwinUpdateContext();
    registration.setReceivedTime(Instant.parse("2026-09-19T20:00:00Z"));
    twinManager.registerTwin(twin, registration);

    ListenerManager manager = new ListenerManager(twinManager);
    TwinUpdateContext update = new TwinUpdateContext();
    update.setReceivedTime(Instant.parse("2026-09-19T20:01:00Z"));

    HomePositionPacket packet = new HomePositionPacket(new ProcessedFrame(
        "HOME_POSITION",
        null,
        Map.of(
            "latitude", 384261947,
            "longitude", -90737520,
            "altitude", 12345
        ),
        true,
        List.of(),
        null
    ));

    assertTrue(manager.handle(MavlinkMessageIds.HOME_POSITION, "drone-1", packet, update));

    DroneTwin updated = (DroneTwin) twinManager.getTwin("drone-1").orElseThrow();
    assertNotNull(updated.getHomePosition());
    assertEquals(38.4261947, updated.getHomePosition().getLatitude(), 0.0000001);
    assertEquals(-9.073752, updated.getHomePosition().getLongitude(), 0.0000001);
    assertEquals(12.345, updated.getHomePosition().getAltitudeMslMeters(), 0.000001);
    assertEquals(update.getReceivedTime(), updated.getNavigationUpdatedAt());
  }

  @Test
  void unknownMessageIdIsNotClaimed() {
    ListenerManager manager = new ListenerManager(new TwinManager());

    assertFalse(manager.handle(Integer.MAX_VALUE, "missing", null, null));
  }
}
