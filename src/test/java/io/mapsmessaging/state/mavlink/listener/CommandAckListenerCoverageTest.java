/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.state.mavlink.listener;

import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.packet.CommandAckPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandAckListenerCoverageTest {

  private static final Instant RECEIVED = Instant.parse("2026-10-03T14:15:16Z");

  @ParameterizedTest
  @CsvSource({
      "0,ACCEPTED",
      "1,TEMPORARILY_REJECTED",
      "2,DENIED",
      "3,UNSUPPORTED",
      "4,FAILED",
      "5,IN_PROGRESS",
      "6,CANCELLED",
      "7,COMMAND_LONG_ONLY",
      "8,COMMAND_INT_ONLY",
      "9,COMMAND_UNSUPPORTED_MAV_FRAME",
      "99,UNKNOWN"
  })
  void validAcknowledgementUpdatesTwinForEveryResult(int result, String expectedName) {
    Fixture fixture = fixture();
    CommandAckPacket packet = packet(400, result, 255, 190, true);

    fixture.listener.handle("drone-1", packet, context(RECEIVED));

    assertEquals(400, fixture.twin.getLastAcknowledgedCommand());
    assertEquals(expectedName, fixture.twin.getLastCommandAcknowledgement());
    assertEquals(result, fixture.twin.getLastCommandAcknowledgementResult());
    assertEquals(255, fixture.twin.getLastCommandAcknowledgementTargetSystemId());
    assertEquals(190, fixture.twin.getLastCommandAcknowledgementTargetComponentId());
    assertEquals(RECEIVED, fixture.twin.getLastCommandAcknowledgementAt());
    assertEquals(RECEIVED, fixture.twin.getOperationalUpdatedAt());
  }

  @Test
  void nonCommandAckPacketIsIgnored() {
    Fixture fixture = fixture();

    fixture.listener.handle("drone-1", mock(MavlinkPacket.class), context(RECEIVED));

    assertNull(fixture.twin.getLastAcknowledgedCommand());
    assertNull(fixture.twin.getLastCommandAcknowledgement());
  }

  @Test
  void invalidCommandAckPacketIsIgnored() {
    Fixture fixture = fixture();
    CommandAckPacket packet = packet(400, 0, 1, 1, false);

    fixture.listener.handle("drone-1", packet, context(RECEIVED));

    assertNull(fixture.twin.getLastAcknowledgedCommand());
    assertNull(fixture.twin.getLastCommandAcknowledgement());
  }

  @Test
  void validAcknowledgementForUnknownTwinIsIgnoredWithoutFailure() {
    TwinManager manager = new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
    CommandAckListener listener = new CommandAckListener(manager);
    CommandAckPacket packet = packet(400, 0, 1, 1, true);

    assertDoesNotThrow(() -> listener.handle("missing", packet, context(RECEIVED)));
    assertEquals(0, manager.getTwinCount());
  }

  @Test
  void nullContextUsesCurrentTime() {
    Fixture fixture = fixture();
    CommandAckPacket packet = packet(176, 0, 1, 2, true);
    Instant before = Instant.now();

    fixture.listener.handle("drone-1", packet, null);

    Instant after = Instant.now();
    Instant acknowledgementAt = fixture.twin.getLastCommandAcknowledgementAt();
    assertNotNull(acknowledgementAt);
    assertFalse(acknowledgementAt.isBefore(before));
    assertFalse(acknowledgementAt.isAfter(after));
    assertEquals(acknowledgementAt, fixture.twin.getOperationalUpdatedAt());
  }

  @Test
  void contextWithoutReceivedTimeUsesCurrentTime() {
    Fixture fixture = fixture();
    CommandAckPacket packet = packet(176, 4, 1, 2, true);
    TwinUpdateContext context = new TwinUpdateContext();
    Instant before = Instant.now();

    fixture.listener.handle("drone-1", packet, context);

    Instant after = Instant.now();
    Instant acknowledgementAt = fixture.twin.getLastCommandAcknowledgementAt();
    assertNotNull(acknowledgementAt);
    assertFalse(acknowledgementAt.isBefore(before));
    assertFalse(acknowledgementAt.isAfter(after));
    assertEquals(acknowledgementAt, fixture.twin.getOperationalUpdatedAt());
  }

  private static CommandAckPacket packet(
      int command,
      int result,
      int targetSystem,
      int targetComponent,
      boolean valid) {
    ProcessedFrame frame = mock(ProcessedFrame.class);
    when(frame.getFields()).thenReturn(Map.of(
        "command", command,
        "result", result,
        "target_system", targetSystem,
        "target_component", targetComponent));
    when(frame.isValid()).thenReturn(valid);
    return new CommandAckPacket(frame);
  }

  private static TwinUpdateContext context(Instant receivedTime) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(receivedTime);
    return context;
  }

  private static Fixture fixture() {
    TwinManager manager = new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
    DroneTwin twin = new DroneTwin("drone-1");
    manager.registerTwin(twin, context(RECEIVED));
    return new Fixture(twin, new CommandAckListener(manager));
  }

  private record Fixture(DroneTwin twin, CommandAckListener listener) {
  }
}
