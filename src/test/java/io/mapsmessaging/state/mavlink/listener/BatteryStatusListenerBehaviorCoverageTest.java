/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.state.mavlink.listener;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.BatteryState;
import io.mapsmessaging.state.mavlink.packet.BatteryStatusPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BatteryStatusListenerBehaviorCoverageTest {

  private static final Instant RECEIVED = Instant.parse("2026-10-03T12:34:56Z");

  @Test
  void validPacketPopulatesAllKnownBatteryFields() {
    Fixture fixture = fixture(2.0, null);
    BatteryStatusPacket packet = packet();
    when(packet.isBatteryRemainingKnown()).thenReturn(true);
    when(packet.getBatteryRemaining()).thenReturn(50);
    when(packet.isCurrentBatteryKnown()).thenReturn(true);
    when(packet.getCurrentBatteryAmps()).thenReturn(2.5);
    when(packet.isTemperatureKnown()).thenReturn(true);
    when(packet.getTemperatureDegreesCelsius()).thenReturn(35.5);
    when(packet.getKnownVoltages()).thenReturn(new int[]{12_000, 12_100});
    when(packet.getCurrentConsumed()).thenReturn(500);
    when(packet.isChargeStatePresent()).thenReturn(true);
    when(packet.getChargeState()).thenReturn(2);

    fixture.listener.handle("drone-1", packet, context(RECEIVED));

    BatteryState state = fixture.twin.getBatteryState();
    assertNotNull(state);
    assertEquals(50.0, state.getPercentage());
    assertEquals(2.5, state.getCurrentAmps());
    assertEquals(35.5, state.getTemperatureCelsius());
    assertEquals(24.1, state.getVoltageVolts(), 0.0001);
    assertEquals(500.0, state.getRemainingMilliampHours(), 0.0001);
    assertEquals("PT1H", state.getDuration());
    assertTrue(state.getCharging());
    assertEquals(RECEIVED, fixture.twin.getOperationalUpdatedAt());
  }

  @Test
  void unknownPacketFieldsPreserveExistingMeasurements() {
    BatteryState existing = new BatteryState(
        77.0, 22.0, 3.0, 1_000.0, 25.0, true, "PT20M");
    Fixture fixture = fixture(0.0, existing);
    BatteryStatusPacket packet = packet();

    fixture.listener.handle("drone-1", packet, context(RECEIVED));

    BatteryState state = fixture.twin.getBatteryState();
    assertSame(existing, state);
    assertEquals(77.0, state.getPercentage());
    assertEquals(22.0, state.getVoltageVolts());
    assertEquals(3.0, state.getCurrentAmps());
    assertEquals(1_000.0, state.getRemainingMilliampHours());
    assertEquals(25.0, state.getTemperatureCelsius());
    assertEquals("PT20M", state.getDuration());
    assertFalse(state.getCharging());
    assertEquals(RECEIVED, fixture.twin.getOperationalUpdatedAt());
  }

  @ParameterizedTest
  @MethodSource("remainingCapacityCases")
  void remainingCapacityEstimateHonoursConsumedAndPercentageBoundaries(
      int consumed,
      int remainingPercent,
      Double expectedRemainingMilliampHours) {
    Fixture fixture = fixture(0.0, null);
    BatteryStatusPacket packet = packet();
    when(packet.isBatteryRemainingKnown()).thenReturn(true);
    when(packet.getBatteryRemaining()).thenReturn(remainingPercent);
    when(packet.getCurrentConsumed()).thenReturn(consumed);

    fixture.listener.handle("drone-1", packet, context(RECEIVED));

    if (expectedRemainingMilliampHours == null) {
      assertNull(fixture.twin.getBatteryState().getRemainingMilliampHours());
    } else {
      assertEquals(
          expectedRemainingMilliampHours,
          fixture.twin.getBatteryState().getRemainingMilliampHours(),
          0.0001);
    }
  }

  @ParameterizedTest
  @CsvSource({
      "0.0,4.0,PT0S",
      "25.0,4.0,PT1H",
      "50.0,2.0,PT1H",
      "75.0,2.0,PT1H30M",
      "100.0,1.5,PT1H30M"
  })
  void configuredCapacityProducesDeterministicDuration(
      double percentage,
      double capacityHours,
      String expectedDuration) {
    Fixture fixture = fixture(capacityHours, null);
    BatteryStatusPacket packet = packet();
    when(packet.isBatteryRemainingKnown()).thenReturn(true);
    when(packet.getBatteryRemaining()).thenReturn((int) percentage);

    fixture.listener.handle("drone-1", packet, context(RECEIVED));

    assertEquals(expectedDuration, fixture.twin.getBatteryState().getDuration());
  }

  @Test
  void calculatedDurationUsesEstimatedCapacityAndCurrentWhenNoConfiguredCapacity() {
    Fixture fixture = fixture(0.0, null);
    BatteryStatusPacket packet = packet();
    when(packet.isBatteryRemainingKnown()).thenReturn(true);
    when(packet.getBatteryRemaining()).thenReturn(50);
    when(packet.getCurrentConsumed()).thenReturn(500);
    when(packet.isCurrentBatteryKnown()).thenReturn(true);
    when(packet.getCurrentBatteryAmps()).thenReturn(1.0);

    fixture.listener.handle("drone-1", packet, context(RECEIVED));

    BatteryState state = fixture.twin.getBatteryState();
    assertEquals(500.0, state.getRemainingMilliampHours(), 0.0001);
    assertEquals("PT30M", state.getDuration());
  }

  @ParameterizedTest
  @CsvSource({
      "false,0,false",
      "false,2,false",
      "true,1,false",
      "true,2,true",
      "true,3,true",
      "true,4,false"
  })
  void chargingStateIsStoredFromMavlinkChargeState(
      boolean present,
      int chargeState,
      boolean expectedCharging) {
    Fixture fixture = fixture(0.0, null);
    BatteryStatusPacket packet = packet();
    when(packet.isChargeStatePresent()).thenReturn(present);
    when(packet.getChargeState()).thenReturn(chargeState);

    fixture.listener.handle("drone-1", packet, context(RECEIVED));

    assertEquals(expectedCharging, fixture.twin.getBatteryState().getCharging());
  }

  @Test
  void nullContextUsesCurrentTimeForOperationalUpdate() {
    Fixture fixture = fixture(0.0, null);
    BatteryStatusPacket packet = packet();
    Instant before = Instant.now();

    fixture.listener.handle("drone-1", packet, null);

    Instant after = Instant.now();
    Instant updated = fixture.twin.getOperationalUpdatedAt();
    assertNotNull(updated);
    assertFalse(updated.isBefore(before));
    assertFalse(updated.isAfter(after));
  }

  @Test
  void contextWithoutReceivedTimeUsesCurrentTimeForOperationalUpdate() {
    Fixture fixture = fixture(0.0, null);
    BatteryStatusPacket packet = packet();
    TwinUpdateContext context = new TwinUpdateContext();
    Instant before = Instant.now();

    fixture.listener.handle("drone-1", packet, context);

    Instant after = Instant.now();
    Instant updated = fixture.twin.getOperationalUpdatedAt();
    assertNotNull(updated);
    assertFalse(updated.isBefore(before));
    assertFalse(updated.isAfter(after));
  }

  @Test
  void validPacketForUnknownTwinIsIgnoredWithoutFailure() {
    TwinManager manager = new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
    BatteryStatusPacket packet = packet();

    assertDoesNotThrow(
        () -> new BatteryStatusListener(manager).handle("missing", packet, context(RECEIVED)));
    assertEquals(0, manager.getTwinCount());
  }

  private static Stream<Arguments> remainingCapacityCases() {
    return Stream.of(
        Arguments.of(500, 50, 500.0),
        Arguments.of(750, 25, 250.0),
        Arguments.of(0, 50, 0.0),
        Arguments.of(500, 1, 5.0505050505),
        Arguments.of(500, 0, null),
        Arguments.of(500, 100, null),
        Arguments.of(-1, 50, null)
    );
  }

  private static BatteryStatusPacket packet() {
    BatteryStatusPacket packet = mock(BatteryStatusPacket.class);
    when(packet.isValid()).thenReturn(true);
    when(packet.getKnownVoltages()).thenReturn(new int[0]);
    when(packet.getCurrentConsumed()).thenReturn(-1);
    return packet;
  }

  private static TwinUpdateContext context(Instant receivedTime) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(receivedTime);
    return context;
  }

  private static Fixture fixture(double batteryCapacityHours, BatteryState batteryState) {
    TwinManager manager = new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
    DroneTwin twin = new DroneTwin("drone-1");
    twin.setBatteryCapacityHours(batteryCapacityHours);
    twin.setBatteryState(batteryState);
    manager.registerTwin(twin, context(RECEIVED));
    return new Fixture(twin, new BatteryStatusListener(manager));
  }

  private record Fixture(DroneTwin twin, BatteryStatusListener listener) {
  }
}
