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

import java.time.Instant;

public class N2kEnvironmentalParametersJsonListener extends AbstractN2kJsonListener {

  public static final int LISTENER_ID = N2kPgns.ENVIRONMENTAL_PARAMETERS;

  @Override
  public int getPgn() {
    return LISTENER_ID;
  }

  @Override
  public void handle(DroneTwin droneTwin, JsonObject packet, TwinUpdateContext context) {
    Double temperatureKelvin = getDouble(packet, "temperature");
    Double humidityPercent = getDouble(packet, "humidity");
    Double atmosphericPressurePascals = getDouble(packet, "atmosphericPressure");
    Double temperatureInstance = getDouble(packet, "temperatureInstance");
    Double humidityInstance = getDouble(packet, "humidityInstance");

    if (temperatureKelvin == null && humidityPercent == null && atmosphericPressurePascals == null
        && temperatureInstance == null && humidityInstance == null) {
      return;
    }

    Instant now = resolveTimestamp(context);

    if (temperatureKelvin != null) {
      droneTwin.getAttributes().put("n2k.temperatureCelsius", String.valueOf(temperatureKelvin - 273.15d));
    }

    if (humidityPercent != null) {
      droneTwin.getAttributes().put("n2k.humidityPercent", String.valueOf(humidityPercent));
    }

    if (atmosphericPressurePascals != null) {
      droneTwin.getAttributes().put(
          "n2k.atmosphericPressurePascals", String.valueOf(atmosphericPressurePascals));
    }

    if (temperatureInstance != null) {
      droneTwin.getAttributes().put("n2k.temperatureInstance", String.valueOf(temperatureInstance.longValue()));
    }

    if (humidityInstance != null) {
      droneTwin.getAttributes().put("n2k.humidityInstance", String.valueOf(humidityInstance.longValue()));
    }

    droneTwin.setOperationalUpdatedAt(now);
    droneTwin.setLastSeenAt(now);
  }
}