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

class N2kWindJsonListenerTest {

  @Test
  void windPacketUpdatesAttributesAndOperationalTimestamp() {
    N2kWindJsonListener listener = new N2kWindJsonListener();
    DroneTwin twin = new DroneTwin("wind");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-09-19T20:33:00Z"));

    JsonObject packet = new JsonObject();
    packet.addProperty("windSpeed", 12.3);
    packet.addProperty("windDirection", Math.toRadians(225.0));
    packet.addProperty("windReference", 4);

    listener.handle(twin, packet, context);

    assertEquals(N2kPgns.WIND_DATA, listener.getPgn());
    assertEquals("12.3", twin.getAttributes().get("n2k.windSpeedMetersPerSecond"));
    assertEquals("225.0", twin.getAttributes().get("n2k.windDirectionDegrees"));
    assertEquals("4", twin.getAttributes().get("n2k.windReference"));
    assertEquals(context.getReceivedTime(), twin.getOperationalUpdatedAt());
  }

  @Test
  void emptyPacketDoesNothing() {
    DroneTwin twin = new DroneTwin("wind");
    new N2kWindJsonListener().handle(twin, new JsonObject(), new TwinUpdateContext());

    assertTrue(twin.getAttributes().isEmpty());
    assertNull(twin.getOperationalUpdatedAt());
  }
}
