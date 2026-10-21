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
import io.mapsmessaging.state.drone.model.FixInfo;

import java.time.Instant;

public class N2kGnssDopsJsonListener extends AbstractN2kJsonListener {

  public static final int LISTENER_ID = N2kPgns.GNSS_DOPS;

  @Override
  public int getPgn() {
    return LISTENER_ID;
  }

  @Override
  public void handle(DroneTwin droneTwin, JsonObject packet, TwinUpdateContext context) {
    Double horizontalDilution = getDouble(packet, "hdop");
    Double verticalDilution = getDouble(packet, "vdop");
    Double timeDilution = getDouble(packet, "tdop");
    Double setMode = getDouble(packet, "setMode");
    Double operationMode = getDouble(packet, "opMode");

    if (horizontalDilution == null && verticalDilution == null && timeDilution == null
        && setMode == null && operationMode == null) {
      return;
    }

    Instant now = resolveTimestamp(context);

    FixInfo fixInfo = droneTwin.getFixInfo();
    if (fixInfo == null) {
      fixInfo = new FixInfo();
      droneTwin.setFixInfo(fixInfo);
    }

    if (horizontalDilution != null) {
      fixInfo.setHdop(horizontalDilution);
    }

    if (verticalDilution != null) {
      fixInfo.setVdop(verticalDilution);
    }

    if (timeDilution != null) {
      droneTwin.getAttributes().put("n2k.gnss.tdop", String.valueOf(timeDilution));
    }

    if (setMode != null) {
      droneTwin.getAttributes().put("n2k.gnss.setMode", String.valueOf(setMode.longValue()));
    }

    if (operationMode != null) {
      droneTwin.getAttributes().put("n2k.gnss.operationMode", String.valueOf(operationMode.longValue()));
    }

    droneTwin.setGpsValid(true);
    droneTwin.setNavigationUpdatedAt(now);
    droneTwin.setLastSeenAt(now);
  }
}