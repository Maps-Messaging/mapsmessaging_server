/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.state.mavlink.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.Orientation;
import io.mapsmessaging.state.mavlink.packet.AttitudePacket;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AttitudeListenerTest {

  private static final Instant NOW = Instant.parse("2026-09-06T12:00:00Z");

  @Test
  void incompleteAttitudeUsesNullForUnavailableAngles() {
    TwinManager twinManager = new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
    TwinUpdateContext context = context();
    DroneTwin twin = new DroneTwin("drone-1");
    twin.setOrientation(new Orientation(10.0d, 20.0d, 30.0d));
    twinManager.registerTwin(twin, context);

    ProcessedFrame frame = mock(ProcessedFrame.class);
    when(frame.getFields()).thenReturn(Map.of("roll", Math.toRadians(15.0d)));
    when(frame.isValid()).thenReturn(true);

    AttitudePacket packet = new AttitudePacket(frame);
    new AttitudeListener(twinManager).handle("drone-1", packet, context);

    assertEquals(15.0d, twin.getOrientation().getRollDegrees(), 0.0000001d);
    assertNull(twin.getOrientation().getPitchDegrees());
    assertNull(twin.getOrientation().getYawDegrees());
    assertEquals(NOW, twin.getMotionUpdatedAt());
  }

  @Test
  void nonFiniteAttitudeValuesAreNotStoredInTwin() {
    TwinManager twinManager = new TwinManager(false, 10_000L, 5_000L, 120_000L, null);
    TwinUpdateContext context = context();
    DroneTwin twin = new DroneTwin("drone-1");
    twinManager.registerTwin(twin, context);

    ProcessedFrame frame = mock(ProcessedFrame.class);
    when(frame.getFields()).thenReturn(Map.of(
        "roll", Double.POSITIVE_INFINITY,
        "pitch", Double.NEGATIVE_INFINITY,
        "yaw", Double.NaN
    ));
    when(frame.isValid()).thenReturn(true);

    AttitudePacket packet = new AttitudePacket(frame);
    new AttitudeListener(twinManager).handle("drone-1", packet, context);

    assertNull(twin.getOrientation().getRollDegrees());
    assertNull(twin.getOrientation().getPitchDegrees());
    assertNull(twin.getOrientation().getYawDegrees());
  }

  private TwinUpdateContext context() {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(NOW);
    return context;
  }
}
