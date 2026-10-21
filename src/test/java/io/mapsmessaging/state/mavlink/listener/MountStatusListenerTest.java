/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.state.mavlink.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.packet.MountStatusPacket;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MountStatusListenerTest {

  private static final Instant NOW = Instant.parse("2026-09-18T16:30:00Z");

  @Test
  void mountStatusUpdatesCameraOrientationFromCentidegrees() {
    TwinManager twinManager =
        new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
    TwinUpdateContext context = context();
    DroneTwin twin = new DroneTwin("drone-1");
    twinManager.registerTwin(twin, context);

    ProcessedFrame frame = mock(ProcessedFrame.class);
    when(frame.getFields())
        .thenReturn(
            Map.of(
                "pointing_a", -1234,
                "pointing_b", 250,
                "pointing_c", 35950,
                "mount_mode", 2));
    when(frame.isValid()).thenReturn(true);

    MountStatusPacket packet = new MountStatusPacket(frame);
    new MountStatusListener(twinManager).handle("drone-1", packet, context);

    assertEquals(-12.34d, twin.getCameraPitchDegrees(), 0.000001d);
    assertEquals(2.50d, twin.getCameraRollDegrees(), 0.000001d);
    assertEquals(359.50d, twin.getCameraBearingDegrees(), 0.000001d);
    assertEquals(2, twin.getCameraMountMode());
    assertEquals(NOW, twin.getCameraOrientationUpdatedAt());
  }

  @Test
  void cameraBearingIsNormalisedClockwiseFromNorth() {
    TwinManager twinManager =
        new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
    TwinUpdateContext context = context();
    DroneTwin twin = new DroneTwin("drone-1");
    twinManager.registerTwin(twin, context);

    ProcessedFrame frame = mock(ProcessedFrame.class);
    when(frame.getFields())
        .thenReturn(
            Map.of(
                "pointing_a", 0,
                "pointing_b", 0,
                "pointing_c", -100));
    when(frame.isValid()).thenReturn(true);

    MountStatusPacket packet = new MountStatusPacket(frame);
    new MountStatusListener(twinManager).handle("drone-1", packet, context);

    assertEquals(359.0d, twin.getCameraBearingDegrees(), 0.000001d);
    assertNull(twin.getCameraMountMode());
  }

  private TwinUpdateContext context() {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(NOW);
    return context;
  }
}
