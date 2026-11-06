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

class N2kMagneticVariationJsonListenerTest {

  @Test
  void variationPacketUpdatesAttributesAndNavigationTimestamp() {
    N2kMagneticVariationJsonListener listener = new N2kMagneticVariationJsonListener();
    DroneTwin twin = new DroneTwin("variation");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-09-19T20:32:00Z"));

    JsonObject packet = new JsonObject();
    packet.addProperty("variation", Math.toRadians(-4.5));
    packet.addProperty("variationSource", 3);
    packet.addProperty("ageOfServiceDate", 20123);

    listener.handle(twin, packet, context);

    assertEquals(N2kPgns.MAGNETIC_VARIATION, listener.getPgn());
    assertEquals("-4.5", twin.getAttributes().get("n2k.magneticVariationDegrees"));
    assertEquals("3", twin.getAttributes().get("n2k.magneticVariationSource"));
    assertEquals("20123", twin.getAttributes().get("n2k.magneticVariationAgeOfServiceDate"));
    assertEquals(context.getReceivedTime(), twin.getNavigationUpdatedAt());
  }

  @Test
  void emptyPacketDoesNothing() {
    DroneTwin twin = new DroneTwin("variation");
    new N2kMagneticVariationJsonListener().handle(twin, new JsonObject(), new TwinUpdateContext());

    assertTrue(twin.getAttributes().isEmpty());
    assertNull(twin.getNavigationUpdatedAt());
  }
}
