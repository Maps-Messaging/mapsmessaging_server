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

class N2kGnssDopsJsonListenerTest {

  @Test
  void gnssDopsUpdateFixAndNavigationState() {
    N2kGnssDopsJsonListener listener = new N2kGnssDopsJsonListener();
    DroneTwin twin = new DroneTwin("n2k-gnss");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-09-19T20:14:00Z"));

    JsonObject packet = new JsonObject();
    packet.addProperty("hdop", 0.8);
    packet.addProperty("vdop", 1.2);
    packet.addProperty("tdop", 1.4);
    packet.addProperty("setMode", 2);
    packet.addProperty("opMode", 3);

    listener.handle(twin, packet, context);

    assertEquals(N2kPgns.GNSS_DOPS, listener.getPgn());
    assertNotNull(twin.getFixInfo());
    assertEquals(0.8, twin.getFixInfo().getHdop(), 0.0);
    assertEquals(1.2, twin.getFixInfo().getVdop(), 0.0);
    assertEquals("1.4", twin.getAttributes().get("n2k.gnss.tdop"));
    assertEquals("2", twin.getAttributes().get("n2k.gnss.setMode"));
    assertEquals("3", twin.getAttributes().get("n2k.gnss.operationMode"));
    assertTrue(twin.getGpsValid());
    assertEquals(context.getReceivedTime(), twin.getNavigationUpdatedAt());
    assertEquals(context.getReceivedTime(), twin.getLastSeenAt());
  }

  @Test
  void emptyPacketDoesNotClaimGpsValidity() {
    DroneTwin twin = new DroneTwin("n2k-gnss");

    new N2kGnssDopsJsonListener().handle(twin, new JsonObject(), new TwinUpdateContext());

    assertNull(twin.getFixInfo());
    assertNull(twin.getGpsValid());
    assertNull(twin.getNavigationUpdatedAt());
  }
}
