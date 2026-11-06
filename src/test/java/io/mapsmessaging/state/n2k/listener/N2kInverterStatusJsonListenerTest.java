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

class N2kInverterStatusJsonListenerTest {

  @Test
  void inverterStatusUpdatesPowerAttributes() {
    N2kInverterStatusJsonListener listener = new N2kInverterStatusJsonListener();
    DroneTwin twin = new DroneTwin("n2k-inverter");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-09-19T20:16:00Z"));

    JsonObject packet = new JsonObject();
    packet.addProperty("inverterInstance", 1);
    packet.addProperty("acInstance", 2);
    packet.addProperty("dcInstance", 3);
    packet.addProperty("operatingState", 4);
    packet.addProperty("inverterEnabledisable", 1);

    listener.handle(twin, packet, context);

    assertEquals(N2kPgns.INVERTER_STATUS, listener.getPgn());
    assertEquals("1", twin.getAttributes().get("n2k.inverter.instance"));
    assertEquals("2", twin.getAttributes().get("n2k.inverter.acInstance"));
    assertEquals("3", twin.getAttributes().get("n2k.inverter.dcInstance"));
    assertEquals("4", twin.getAttributes().get("n2k.inverter.operatingState"));
    assertEquals("1", twin.getAttributes().get("n2k.inverter.enabled"));
    assertEquals(context.getReceivedTime(), twin.getPowerUpdatedAt());
    assertEquals(context.getReceivedTime(), twin.getLastSeenAt());
  }

  @Test
  void emptyPacketLeavesPowerStateUntouched() {
    DroneTwin twin = new DroneTwin("n2k-inverter");

    new N2kInverterStatusJsonListener().handle(twin, new JsonObject(), new TwinUpdateContext());

    assertTrue(twin.getAttributes().isEmpty());
    assertNull(twin.getPowerUpdatedAt());
  }
}
