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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.BatteryState;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class N2kPowerEnvironmentalListenersTest {

  private static final Instant RECEIVED_TIME = Instant.parse("2026-07-28T11:20:00Z");
  private static final double DELTA = 0.0000001d;

  @Test
  void batteryStatus_convertsKelvinAndPreservesZeroValues() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("batteryInstance", 0);
    packet.addProperty("batteryVoltage", 0.0d);
    packet.addProperty("batteryCurrent", -4.5d);
    packet.addProperty("batteryCaseTemperature", 273.15d);

    new N2kBatteryStatusJsonListener().handle(droneTwin, packet, context());

    assertEquals(0.0d, droneTwin.getBatteryState().getVoltageVolts());
    assertEquals(-4.5d, droneTwin.getBatteryState().getCurrentAmps());
    assertEquals(0.0d, droneTwin.getBatteryState().getTemperatureCelsius(), DELTA);
    assertEquals("0", droneTwin.getAttributes().get("n2k.battery.instance"));
    assertSame(RECEIVED_TIME, droneTwin.getPowerUpdatedAt());
    assertSame(RECEIVED_TIME, droneTwin.getLastSeenAt());
  }

  @Test
  void batteryStatus_malformedCurrent_stillUpdatesVoltageAndExistingState() {
    DroneTwin droneTwin = new DroneTwin();
    BatteryState batteryState = new BatteryState();
    batteryState.setCurrentAmps(1.5d);
    droneTwin.setBatteryState(batteryState);
    JsonObject packet = new JsonObject();
    packet.addProperty("batteryVoltage", "24.75");
    packet.add("batteryCurrent", new JsonObject());

    new N2kBatteryStatusJsonListener().handle(droneTwin, packet, context());

    assertSame(batteryState, droneTwin.getBatteryState());
    assertEquals(24.75d, batteryState.getVoltageVolts());
    assertEquals(1.5d, batteryState.getCurrentAmps());
  }

  @Test
  void environmentalParameters_convertsTemperatureAndStoresMetricValues() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("temperature", 293.15d);
    packet.addProperty("humidity", 0.0d);
    packet.addProperty("atmosphericPressure", 101_325.0d);
    packet.addProperty("temperatureInstance", 1);
    packet.addProperty("humidityInstance", 2);

    new N2kEnvironmentalParametersJsonListener().handle(droneTwin, packet, context());

    assertEquals(20.0d, attributeDouble(droneTwin, "n2k.temperatureCelsius"), DELTA);
    assertEquals(0.0d, attributeDouble(droneTwin, "n2k.humidityPercent"));
    assertEquals(101_325.0d, attributeDouble(droneTwin, "n2k.atmosphericPressurePascals"));
    assertEquals("1", droneTwin.getAttributes().get("n2k.temperatureInstance"));
    assertEquals("2", droneTwin.getAttributes().get("n2k.humidityInstance"));
    assertSame(RECEIVED_TIME, droneTwin.getOperationalUpdatedAt());
  }

  @Test
  void environmentalParameters_malformedTemperature_stillStoresPressure() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("temperature", "NaN");
    packet.addProperty("atmosphericPressure", 90_000.0d);

    new N2kEnvironmentalParametersJsonListener().handle(droneTwin, packet, context());

    assertNull(droneTwin.getAttributes().get("n2k.temperatureCelsius"));
    assertEquals(90_000.0d, attributeDouble(droneTwin, "n2k.atmosphericPressurePascals"));
  }

  @Test
  void inverterStatus_partialPayload_preservesSupportedNumericValues() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("inverterInstance", 0);
    packet.addProperty("acInstance", "1");
    packet.add("dcInstance", JsonNull.INSTANCE);
    packet.addProperty("operatingState", 4);
    packet.addProperty("inverterEnabledisable", 1);

    new N2kInverterStatusJsonListener().handle(droneTwin, packet, context());

    assertEquals("0", droneTwin.getAttributes().get("n2k.inverter.instance"));
    assertEquals("1", droneTwin.getAttributes().get("n2k.inverter.acInstance"));
    assertNull(droneTwin.getAttributes().get("n2k.inverter.dcInstance"));
    assertEquals("4", droneTwin.getAttributes().get("n2k.inverter.operatingState"));
    assertEquals("1", droneTwin.getAttributes().get("n2k.inverter.enabled"));
    assertSame(RECEIVED_TIME, droneTwin.getPowerUpdatedAt());
  }

  @Test
  void inverterStatus_malformedField_doesNotBlockOtherFields() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("operatingState", true);
    packet.addProperty("inverterEnabledisable", 0);

    new N2kInverterStatusJsonListener().handle(droneTwin, packet, context());

    assertNull(droneTwin.getAttributes().get("n2k.inverter.operatingState"));
    assertEquals("0", droneTwin.getAttributes().get("n2k.inverter.enabled"));
  }

  @Test
  void nullOnlyPayload_doesNotCreateStateOrAdvanceTimestamp() {
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.add("batteryVoltage", JsonNull.INSTANCE);

    new N2kBatteryStatusJsonListener().handle(droneTwin, packet, context());

    assertNull(droneTwin.getBatteryState());
    assertNull(droneTwin.getPowerUpdatedAt());
    assertNull(droneTwin.getLastSeenAt());
  }

  private static TwinUpdateContext context() {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(RECEIVED_TIME);
    return context;
  }

  private static double attributeDouble(DroneTwin droneTwin, String key) {
    return Double.parseDouble(droneTwin.getAttributes().get(key));
  }
}
