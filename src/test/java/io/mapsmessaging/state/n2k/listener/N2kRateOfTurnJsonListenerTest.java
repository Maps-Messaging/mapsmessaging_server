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

class N2kRateOfTurnJsonListenerTest {

  @Test
  void rateOfTurnIsConvertedToDegreesPerSecond() {
    N2kRateOfTurnJsonListener listener = new N2kRateOfTurnJsonListener();
    DroneTwin twin = new DroneTwin("turn");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-09-19T20:40:00Z"));

    JsonObject packet = new JsonObject();
    packet.addProperty("rateOfTurn", Math.toRadians(-12.5));

    listener.handle(twin, packet, context);

    assertEquals(N2kPgns.RATE_OF_TURN, listener.getPgn());
    assertEquals("-12.5", twin.getAttributes().get("n2k.rateOfTurnDegreesPerSecond"));
    assertEquals(context.getReceivedTime(), twin.getMotionUpdatedAt());
    assertEquals(context.getReceivedTime(), twin.getLastSeenAt());
  }

  @Test
  void missingRateDoesNothing() {
    DroneTwin twin = new DroneTwin("turn");

    new N2kRateOfTurnJsonListener().handle(twin, new JsonObject(), new TwinUpdateContext());

    assertTrue(twin.getAttributes().isEmpty());
    assertNull(twin.getMotionUpdatedAt());
  }
}
