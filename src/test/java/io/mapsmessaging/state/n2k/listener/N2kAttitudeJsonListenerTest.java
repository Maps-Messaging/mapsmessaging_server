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

package io.mapsmessaging.state.n2k.listener;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class N2kAttitudeJsonListenerTest {

  @Test
  void attitudePacketMapsRadiansToDegrees() {
    N2kAttitudeJsonListener listener = new N2kAttitudeJsonListener();
    DroneTwin twin = new DroneTwin("attitude");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-09-19T20:34:00Z"));

    JsonObject packet = new JsonObject();
    packet.addProperty("roll", Math.toRadians(10.0));
    packet.addProperty("pitch", Math.toRadians(-5.0));
    packet.addProperty("yaw", Math.toRadians(90.0));

    listener.handle(twin, packet, context);

    assertEquals(N2kPgns.ATTITUDE, listener.getPgn());
    assertNotNull(twin.getOrientation());
    assertEquals(10.0, twin.getOrientation().getRollDegrees(), 0.000001);
    assertEquals(-5.0, twin.getOrientation().getPitchDegrees(), 0.000001);
    assertEquals(90.0, twin.getOrientation().getYawDegrees(), 0.000001);
    assertEquals(context.getReceivedTime(), twin.getMotionUpdatedAt());
  }

  @Test
  void partialAttitudeLeavesMissingAxesNull() {
    DroneTwin twin = new DroneTwin("attitude");
    JsonObject packet = new JsonObject();
    packet.addProperty("yaw", Math.PI);

    new N2kAttitudeJsonListener().handle(twin, packet, new TwinUpdateContext());

    assertNull(twin.getOrientation().getRollDegrees());
    assertNull(twin.getOrientation().getPitchDegrees());
    assertEquals(180.0, twin.getOrientation().getYawDegrees(), 0.000001);
  }

  @Test
  void emptyPacketDoesNothing() {
    DroneTwin twin = new DroneTwin("attitude");
    new N2kAttitudeJsonListener().handle(twin, new JsonObject(), new TwinUpdateContext());

    assertNull(twin.getOrientation());
  }
}
