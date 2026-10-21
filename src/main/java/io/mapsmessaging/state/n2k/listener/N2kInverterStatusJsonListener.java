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

public class N2kInverterStatusJsonListener extends AbstractN2kJsonListener {

  public static final int LISTENER_ID = N2kPgns.INVERTER_STATUS;

  @Override
  public int getPgn() {
    return LISTENER_ID;
  }

  @Override
  public void handle(DroneTwin droneTwin, JsonObject packet, TwinUpdateContext context) {
    Double inverterInstance = getDouble(packet, "inverterInstance");
    Double acInstance = getDouble(packet, "acInstance");
    Double dcInstance = getDouble(packet, "dcInstance");
    Double operatingState = getDouble(packet, "operatingState");
    Double inverterEnableDisable = getDouble(packet, "inverterEnabledisable");

    if (inverterInstance == null && acInstance == null && dcInstance == null
        && operatingState == null && inverterEnableDisable == null) {
      return;
    }

    Instant now = resolveTimestamp(context);

    if (inverterInstance != null) {
      droneTwin.getAttributes().put("n2k.inverter.instance", String.valueOf(inverterInstance.longValue()));
    }

    if (acInstance != null) {
      droneTwin.getAttributes().put("n2k.inverter.acInstance", String.valueOf(acInstance.longValue()));
    }

    if (dcInstance != null) {
      droneTwin.getAttributes().put("n2k.inverter.dcInstance", String.valueOf(dcInstance.longValue()));
    }

    if (operatingState != null) {
      droneTwin.getAttributes().put("n2k.inverter.operatingState", String.valueOf(operatingState.longValue()));
    }

    if (inverterEnableDisable != null) {
      droneTwin.getAttributes().put("n2k.inverter.enabled", String.valueOf(inverterEnableDisable.longValue()));
    }

    droneTwin.setPowerUpdatedAt(now);
    droneTwin.setLastSeenAt(now);
  }
}