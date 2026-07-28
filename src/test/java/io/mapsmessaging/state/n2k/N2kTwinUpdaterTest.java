package io.mapsmessaging.state.n2k;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.n2k.listener.N2kPgns;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class N2kTwinUpdaterTest {

  private TwinManager twinManager;
  private N2kTwinUpdater updater;
  private N2KTwinConfig config;
  private DroneInfoDTO droneInfo;

  @BeforeEach
  void setUp() {
    twinManager = new TwinManager();
    updater = new N2kTwinUpdater(twinManager);
    config = new N2KTwinConfig();
    config.setName("  vessel/feed  ");
    config.setTopic("/canbus0/n2k/json/#");
    config.setVehicleClass("usv");
    droneInfo = new DroneInfoDTO();
    droneInfo.setUuid(UUID.fromString("f94b778c-a504-4f51-a325-829ff7fa1653"));
    droneInfo.setBatteryCapacityHours(12.5d);
  }

  @Test
  void validPositionCreatesTwinAndAppliesConfiguredIdentity() {
    Instant receivedAt = Instant.parse("2026-07-29T01:00:00Z");

    updater.updateTwinState(
        N2kPgns.POSITION_RAPID_UPDATE,
        position(-33.8688d, 151.2093d),
        context(receivedAt),
        config,
        droneInfo);

    EntityTwin entityTwin = twinManager.getTwin("vessel-feed").orElseThrow();
    DroneTwin droneTwin = assertInstanceOf(DroneTwin.class, entityTwin);
    assertEquals(droneInfo.getUuid(), droneTwin.getUuid());
    assertEquals("  vessel/feed  ", droneTwin.getDisplayName());
    assertEquals("  vessel/feed  ", droneTwin.getCallSign());
    assertEquals("N2K STANAG feed   vessel/feed  ", droneTwin.getDescriptionString());
    assertEquals(VehicleClass.USV, droneTwin.getVehicleClass());
    assertEquals(-33.8688d, droneTwin.getGeoPosition().getLatitude());
    assertEquals(151.2093d, droneTwin.getGeoPosition().getLongitude());
    assertTrue(droneTwin.getGpsValid());
    assertEquals(receivedAt, droneTwin.getNavigationUpdatedAt());
    assertEquals(receivedAt, droneTwin.getIdentityUpdatedAt());
    assertEquals(receivedAt, droneTwin.getLastSeenAt());
    assertEquals("/canbus0/n2k/json/#", droneTwin.getAttributes().get("n2k.subscription.topic"));
    assertEquals(12.5d, droneTwin.getBatteryCapacityHours());
  }

  @Test
  void unchangedPositionRetainsValuesAndAdvancesFreshness() {
    Instant firstAt = Instant.parse("2026-07-29T01:00:00Z");
    Instant secondAt = firstAt.plusSeconds(1);
    updater.updateTwinState(N2kPgns.POSITION_RAPID_UPDATE, position(10.0d, 20.0d), context(firstAt), config, droneInfo);
    DroneTwin droneTwin = twin();
    Object firstPosition = droneTwin.getGeoPosition();

    updater.updateTwinState(N2kPgns.POSITION_RAPID_UPDATE, position(10.0d, 20.0d), context(secondAt), config, droneInfo);

    assertEquals(10.0d, droneTwin.getGeoPosition().getLatitude());
    assertEquals(20.0d, droneTwin.getGeoPosition().getLongitude());
    assertEquals(secondAt, droneTwin.getNavigationUpdatedAt());
    assertEquals(secondAt, droneTwin.getLastSeenAt());
    assertNotSame(firstPosition, droneTwin.getGeoPosition());
  }

  @Test
  void missingAndInvalidPositionDoNotReplaceLastValidNavigationState() {
    Instant validAt = Instant.parse("2026-07-29T01:00:00Z");
    updater.updateTwinState(N2kPgns.POSITION_RAPID_UPDATE, position(10.0d, 20.0d), context(validAt), config, droneInfo);
    DroneTwin droneTwin = twin();
    Object validPosition = droneTwin.getGeoPosition();

    updater.updateTwinState(N2kPgns.POSITION_RAPID_UPDATE, new JsonObject(), context(validAt.plusSeconds(1)), config, droneInfo);
    updater.updateTwinState(N2kPgns.POSITION_RAPID_UPDATE, position(91.0d, 181.0d), context(validAt.plusSeconds(2)), config, droneInfo);

    assertSame(validPosition, droneTwin.getGeoPosition());
    assertEquals(validAt, droneTwin.getNavigationUpdatedAt());
    assertEquals(validAt.plusSeconds(2), droneTwin.getLastSeenAt());
  }

  @Test
  void stalePositionCannotOverwriteNewerPositionOrNotifyObservers() {
    Instant newerAt = Instant.parse("2026-07-29T01:00:10Z");
    updater.updateTwinState(N2kPgns.POSITION_RAPID_UPDATE, position(10.0d, 20.0d), context(newerAt), config, droneInfo);
    DroneTwin droneTwin = twin();
    AtomicInteger updates = new AtomicInteger();
    twinManager.addObserver(new TwinObserver() {
      @Override
      public void onTwinUpdated(String twinId, EntityTwin current, TwinUpdateContext context) {
        updates.incrementAndGet();
      }
    });

    updater.updateTwinState(
        N2kPgns.POSITION_RAPID_UPDATE,
        position(30.0d, 40.0d),
        context(newerAt.minusSeconds(5)),
        config,
        droneInfo);

    assertEquals(10.0d, droneTwin.getGeoPosition().getLatitude());
    assertEquals(20.0d, droneTwin.getGeoPosition().getLongitude());
    assertEquals(newerAt, droneTwin.getNavigationUpdatedAt());
    assertEquals(newerAt, droneTwin.getLastSeenAt());
    assertEquals(0, updates.get());
  }

  @Test
  void partialMotionUpdatesOnlyPresentValues() {
    Instant firstAt = Instant.parse("2026-07-29T01:00:00Z");
    JsonObject initial = new JsonObject();
    initial.addProperty("courseOverGround", Math.toRadians(350.0d));
    initial.addProperty("speedOverGround", 2.0d);
    updater.updateTwinState(N2kPgns.COG_SOG_RAPID_UPDATE, initial, context(firstAt), config, droneInfo);
    DroneTwin droneTwin = twin();

    JsonObject speedOnly = new JsonObject();
    speedOnly.addProperty("speedOverGround", 3.5d);
    updater.updateTwinState(N2kPgns.COG_SOG_RAPID_UPDATE, speedOnly, context(firstAt.plusSeconds(1)), config, droneInfo);

    assertEquals(350.0d, droneTwin.getCourseOverGroundDegrees(), 0.000001d);
    assertEquals(3.5d, droneTwin.getGroundSpeedMetersPerSecond());
    assertEquals(firstAt.plusSeconds(1), droneTwin.getMotionUpdatedAt());
  }

  @Test
  void missingMotionLeavesMotionFieldsAndTimestampUnset() {
    Instant receivedAt = Instant.parse("2026-07-29T01:00:00Z");

    updater.updateTwinState(N2kPgns.COG_SOG_RAPID_UPDATE, new JsonObject(), context(receivedAt), config, droneInfo);

    DroneTwin droneTwin = twin();
    assertNull(droneTwin.getCourseOverGroundDegrees());
    assertNull(droneTwin.getGroundSpeedMetersPerSecond());
    assertNull(droneTwin.getMotionUpdatedAt());
    assertEquals(receivedAt, droneTwin.getLastSeenAt());
  }

  @Test
  void staleMotionCannotOverwriteNewerMotion() {
    Instant newerAt = Instant.parse("2026-07-29T01:00:10Z");
    JsonObject newer = new JsonObject();
    newer.addProperty("courseOverGround", Math.toRadians(45.0d));
    newer.addProperty("speedOverGround", 4.0d);
    updater.updateTwinState(N2kPgns.COG_SOG_RAPID_UPDATE, newer, context(newerAt), config, droneInfo);
    DroneTwin droneTwin = twin();

    JsonObject stale = new JsonObject();
    stale.addProperty("courseOverGround", Math.toRadians(180.0d));
    stale.addProperty("speedOverGround", 9.0d);
    updater.updateTwinState(N2kPgns.COG_SOG_RAPID_UPDATE, stale, context(newerAt.minusSeconds(1)), config, droneInfo);

    assertEquals(45.0d, droneTwin.getCourseOverGroundDegrees(), 0.000001d);
    assertEquals(4.0d, droneTwin.getGroundSpeedMetersPerSecond());
    assertEquals(newerAt, droneTwin.getMotionUpdatedAt());
  }

  private DroneTwin twin() {
    return assertInstanceOf(DroneTwin.class, twinManager.getTwin("vessel-feed").orElseThrow());
  }

  private static JsonObject position(double latitude, double longitude) {
    JsonObject packet = new JsonObject();
    packet.addProperty("latitude", latitude);
    packet.addProperty("longitude", longitude);
    return packet;
  }

  private static TwinUpdateContext context(Instant receivedAt) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(receivedAt);
    context.setUpdateSource("test");
    return context;
  }
}
