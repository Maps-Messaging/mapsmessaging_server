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

class N2kMotionJsonListenerTest {

  @Test
  void motionPacketUpdatesCourseSpeedReferenceAndTimestamps() {
    N2kMotionJsonListener listener = new N2kMotionJsonListener();
    DroneTwin twin = new DroneTwin("motion");
    TwinUpdateContext context = context();

    JsonObject packet = new JsonObject();
    packet.addProperty("courseOverGround", Math.toRadians(370.0));
    packet.addProperty("speedOverGround", 4.6);
    packet.addProperty("cogReference", 2);

    listener.handle(twin, packet, context);

    assertEquals(N2kPgns.COG_SOG_RAPID_UPDATE, listener.getPgn());
    assertEquals(10.0, twin.getCourseOverGroundDegrees(), 0.000001);
    assertEquals(4.6, twin.getGroundSpeedMetersPerSecond(), 0.0);
    assertEquals("2", twin.getAttributes().get("n2k.motion.courseReference"));
    assertEquals(context.getReceivedTime(), twin.getOperationalUpdatedAt());
    assertEquals(context.getReceivedTime(), twin.getMotionUpdatedAt());
    assertEquals(context.getReceivedTime(), twin.getLastSeenAt());
  }

  @Test
  void emptyPacketDoesNothing() {
    DroneTwin twin = new DroneTwin("motion");
    new N2kMotionJsonListener().handle(twin, new JsonObject(), context());

    assertNull(twin.getCourseOverGroundDegrees());
    assertNull(twin.getGroundSpeedMetersPerSecond());
    assertNull(twin.getMotionUpdatedAt());
  }

  private static TwinUpdateContext context() {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-09-19T20:31:00Z"));
    return context;
  }
}
