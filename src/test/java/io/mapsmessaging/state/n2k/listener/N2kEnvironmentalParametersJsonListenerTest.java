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

class N2kEnvironmentalParametersJsonListenerTest {

  @Test
  void environmentalValuesAreStoredAsTwinAttributes() {
    N2kEnvironmentalParametersJsonListener listener = new N2kEnvironmentalParametersJsonListener();
    DroneTwin twin = new DroneTwin("n2k-environment");
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-09-19T20:12:00Z"));

    JsonObject packet = new JsonObject();
    packet.addProperty("temperature", 293.15);
    packet.addProperty("humidity", 68.5);
    packet.addProperty("atmosphericPressure", 101325.0);
    packet.addProperty("temperatureInstance", 3);
    packet.addProperty("humidityInstance", 4);

    listener.handle(twin, packet, context);

    assertEquals(N2kPgns.ENVIRONMENTAL_PARAMETERS, listener.getPgn());
    assertEquals("20.0", twin.getAttributes().get("n2k.temperatureCelsius"));
    assertEquals("68.5", twin.getAttributes().get("n2k.humidityPercent"));
    assertEquals("101325.0", twin.getAttributes().get("n2k.atmosphericPressurePascals"));
    assertEquals("3", twin.getAttributes().get("n2k.temperatureInstance"));
    assertEquals("4", twin.getAttributes().get("n2k.humidityInstance"));
    assertEquals(context.getReceivedTime(), twin.getOperationalUpdatedAt());
    assertEquals(context.getReceivedTime(), twin.getLastSeenAt());
  }

  @Test
  void emptyPacketLeavesTwinUntouched() {
    DroneTwin twin = new DroneTwin("n2k-environment");

    new N2kEnvironmentalParametersJsonListener()
        .handle(twin, new JsonObject(), new TwinUpdateContext());

    assertTrue(twin.getAttributes().isEmpty());
    assertNull(twin.getOperationalUpdatedAt());
  }
}
