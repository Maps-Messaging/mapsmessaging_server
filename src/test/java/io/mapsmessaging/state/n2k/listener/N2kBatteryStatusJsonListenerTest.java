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

class N2kBatteryStatusJsonListenerTest {

  @Test
  void batteryPacketCreatesAndUpdatesBatteryState() {
    N2kBatteryStatusJsonListener listener = new N2kBatteryStatusJsonListener();
    DroneTwin twin = new DroneTwin("n2k-battery");
    TwinUpdateContext context = context("2026-09-19T20:10:00Z");

    JsonObject packet = new JsonObject();
    packet.addProperty("batteryInstance", 2);
    packet.addProperty("batteryVoltage", 24.6);
    packet.addProperty("batteryCurrent", 8.4);
    packet.addProperty("batteryCaseTemperature", 300.15);

    listener.handle(twin, packet, context);

    assertEquals(N2kPgns.BATTERY_STATUS, listener.getPgn());
    assertNotNull(twin.getBatteryState());
    assertEquals(24.6, twin.getBatteryState().getVoltageVolts(), 0.0);
    assertEquals(8.4, twin.getBatteryState().getCurrentAmps(), 0.0);
    assertEquals(27.0, twin.getBatteryState().getTemperatureCelsius(), 0.000001);
    assertEquals("2", twin.getAttributes().get("n2k.battery.instance"));
    assertEquals(context.getReceivedTime(), twin.getPowerUpdatedAt());
    assertEquals(context.getReceivedTime(), twin.getLastSeenAt());
  }

  @Test
  void partialPacketPreservesExistingBatteryValues() {
    N2kBatteryStatusJsonListener listener = new N2kBatteryStatusJsonListener();
    DroneTwin twin = new DroneTwin("n2k-battery");
    TwinUpdateContext first = context("2026-09-19T20:10:00Z");

    JsonObject initial = new JsonObject();
    initial.addProperty("batteryVoltage", 12.4);
    initial.addProperty("batteryCurrent", 4.1);
    listener.handle(twin, initial, first);

    TwinUpdateContext second = context("2026-09-19T20:11:00Z");
    JsonObject update = new JsonObject();
    update.addProperty("batteryCurrent", 5.2);
    listener.handle(twin, update, second);

    assertEquals(12.4, twin.getBatteryState().getVoltageVolts(), 0.0);
    assertEquals(5.2, twin.getBatteryState().getCurrentAmps(), 0.0);
    assertEquals(second.getReceivedTime(), twin.getPowerUpdatedAt());
  }

  @Test
  void emptyPacketDoesNotCreateBatteryState() {
    DroneTwin twin = new DroneTwin("n2k-battery");
    new N2kBatteryStatusJsonListener().handle(twin, new JsonObject(), context("2026-09-19T20:10:00Z"));

    assertNull(twin.getBatteryState());
    assertNull(twin.getPowerUpdatedAt());
  }

  private static TwinUpdateContext context(String instant) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse(instant));
    return context;
  }
}
