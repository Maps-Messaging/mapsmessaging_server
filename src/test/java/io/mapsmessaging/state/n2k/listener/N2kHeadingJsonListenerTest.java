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

class N2kHeadingJsonListenerTest {

  @Test
  void headingPacketUpdatesHeadingMetadataAndTimestamp() {
    N2kHeadingJsonListener listener = new N2kHeadingJsonListener();
    DroneTwin twin = new DroneTwin("heading");
    TwinUpdateContext context = context();

    JsonObject packet = new JsonObject();
    packet.addProperty("headingSensorReading", Math.toRadians(-10.0));
    packet.addProperty("variation", Math.toRadians(2.0));
    packet.addProperty("deviation", Math.toRadians(-1.0));
    packet.addProperty("headingSensorReference", 1);

    listener.handle(twin, packet, context);

    assertEquals(N2kPgns.VESSEL_HEADING, listener.getPgn());
    assertEquals(350.0, twin.getHeadingDegrees(), 0.000001);
    assertEquals("2.0", twin.getAttributes().get("n2k.heading.variationDegrees"));
    assertEquals("-1.0", twin.getAttributes().get("n2k.heading.deviationDegrees"));
    assertEquals("1", twin.getAttributes().get("n2k.heading.sensorReference"));
    assertEquals(context.getReceivedTime(), twin.getNavigationUpdatedAt());
    assertEquals(context.getReceivedTime(), twin.getLastSeenAt());
  }

  @Test
  void emptyPacketDoesNothing() {
    DroneTwin twin = new DroneTwin("heading");
    new N2kHeadingJsonListener().handle(twin, new JsonObject(), context());

    assertNull(twin.getHeadingDegrees());
    assertNull(twin.getNavigationUpdatedAt());
  }

  private static TwinUpdateContext context() {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-09-19T20:30:00Z"));
    return context;
  }
}
