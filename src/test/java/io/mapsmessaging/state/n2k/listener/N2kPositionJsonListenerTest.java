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

class N2kPositionJsonListenerTest {

  @Test
  void validRapidPositionUpdatesGpsState() {
    N2kPositionJsonListener listener = new N2kPositionJsonListener();
    DroneTwin twin = new DroneTwin("position");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-09-19T20:36:00Z"));

    JsonObject packet = new JsonObject();
    packet.addProperty("latitude", -33.86);
    packet.addProperty("longitude", 151.20);

    listener.handle(twin, packet, context);

    assertEquals(N2kPgns.POSITION_RAPID_UPDATE, listener.getPgn());
    assertEquals(-33.86, twin.getGeoPosition().getLatitude(), 0.0);
    assertEquals(151.20, twin.getGeoPosition().getLongitude(), 0.0);
    assertNull(twin.getGeoPosition().getAltitudeMslMeters());
    assertTrue(twin.getGpsValid());
    assertEquals(context.getReceivedTime(), twin.getNavigationUpdatedAt());
  }

  @Test
  void missingCoordinateIsRejected() {
    DroneTwin twin = new DroneTwin("position");
    JsonObject packet = new JsonObject();
    packet.addProperty("latitude", 10.0);

    new N2kPositionJsonListener().handle(twin, packet, new TwinUpdateContext());

    assertNull(twin.getGeoPosition());
    assertNull(twin.getGpsValid());
  }
}
