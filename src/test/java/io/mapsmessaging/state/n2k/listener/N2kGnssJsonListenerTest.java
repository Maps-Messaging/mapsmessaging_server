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

class N2kGnssJsonListenerTest {

  @Test
  void validPositionUpdatesGpsStateIncludingAltitude() {
    N2kGnssJsonListener listener = new N2kGnssJsonListener();
    DroneTwin twin = new DroneTwin("gnss");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-09-19T20:35:00Z"));

    JsonObject packet = new JsonObject();
    packet.addProperty("latitude", 38.4);
    packet.addProperty("longitude", -9.1);
    packet.addProperty("altitude", 23.5);

    listener.handle(twin, packet, context);

    assertEquals(N2kPgns.GNSS_POSITION_DATA, listener.getPgn());
    assertEquals(38.4, twin.getGeoPosition().getLatitude(), 0.0);
    assertEquals(-9.1, twin.getGeoPosition().getLongitude(), 0.0);
    assertEquals(23.5, twin.getGeoPosition().getAltitudeMslMeters(), 0.0);
    assertTrue(twin.getGpsValid());
    assertEquals(context.getReceivedTime(), twin.getNavigationUpdatedAt());
  }

  @Test
  void invalidLatitudeOrLongitudeIsRejected() {
    N2kGnssJsonListener listener = new N2kGnssJsonListener();

    DroneTwin latitudeTwin = new DroneTwin("bad-lat");
    JsonObject badLatitude = new JsonObject();
    badLatitude.addProperty("latitude", 91.0);
    badLatitude.addProperty("longitude", 0.0);
    listener.handle(latitudeTwin, badLatitude, new TwinUpdateContext());
    assertNull(latitudeTwin.getGeoPosition());

    DroneTwin longitudeTwin = new DroneTwin("bad-lon");
    JsonObject badLongitude = new JsonObject();
    badLongitude.addProperty("latitude", 0.0);
    badLongitude.addProperty("longitude", 181.0);
    listener.handle(longitudeTwin, badLongitude, new TwinUpdateContext());
    assertNull(longitudeTwin.getGeoPosition());
  }
}
