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
import io.mapsmessaging.state.drone.model.BatteryState;

import java.time.Instant;

public class N2kBatteryStatusJsonListener extends AbstractN2kJsonListener {

  public static final int LISTENER_ID = N2kPgns.BATTERY_STATUS;

  @Override
  public int getPgn() {
    return LISTENER_ID;
  }

  @Override
  public void handle(DroneTwin droneTwin, JsonObject packet, TwinUpdateContext context) {
    Double batteryInstance = getDouble(packet, "batteryInstance");
    Double batteryVoltage = getDouble(packet, "batteryVoltage");
    Double batteryCurrent = getDouble(packet, "batteryCurrent");
    Double batteryCaseTemperatureKelvin = getDouble(packet, "batteryCaseTemperature");

    if (batteryVoltage == null && batteryCurrent == null && batteryCaseTemperatureKelvin == null
        && batteryInstance == null) {
      return;
    }

    Instant now = resolveTimestamp(context);

    BatteryState batteryState = droneTwin.getBatteryState();
    if (batteryState == null) {
      batteryState = new BatteryState();
      droneTwin.setBatteryState(batteryState);
    }

    if (batteryVoltage != null) {
      batteryState.setVoltageVolts(batteryVoltage);
    }

    if (batteryCurrent != null) {
      batteryState.setCurrentAmps(batteryCurrent);
    }

    if (batteryCaseTemperatureKelvin != null) {
      batteryState.setTemperatureCelsius(batteryCaseTemperatureKelvin - 273.15d);
    }

    if (batteryInstance != null) {
      droneTwin.getAttributes().put("n2k.battery.instance", String.valueOf(batteryInstance.longValue()));
    }

    droneTwin.setPowerUpdatedAt(now);
    droneTwin.setLastSeenAt(now);
  }
}