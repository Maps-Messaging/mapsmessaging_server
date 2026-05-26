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

package io.mapsmessaging.state.n2k;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.dto.rest.config.protocol.impl.n2k.N2KAisConfigDTO;
import io.mapsmessaging.network.protocol.impl.n2k.N2kProtocol;
import io.mapsmessaging.state.n2k.handler.*;
import io.mapsmessaging.state.n2k.msg.AisClassBEmitterConfig;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DroneMonitor implements TwinObserver{

  private final N2kProtocol n2kProtocol;

  private final List<AbstractDronePgnHandler> handlers;
  private final Map<String, DroneEmissionState> droneStateMap;

  public DroneMonitor(N2kProtocol n2kProtocol, N2kMessageParser parser, N2KAisConfigDTO aisConfig) {
    this.n2kProtocol = n2kProtocol;
    this.droneStateMap = new ConcurrentHashMap<>();

    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    handlers = new ArrayList<>();
    if (aisConfig == null) {
      aisConfig = new N2KAisConfigDTO();
    }
    if (aisConfig.isEnabled()) {
      if (aisConfig.getPgn129039().isEnabled()) handlers.add(new Ais129039Handler(parser, config, aisConfig.getPgn129039().getIntervalMilliseconds()));
      if (aisConfig.getPgn129040().isEnabled()) handlers.add(new Ais129040Handler(parser, config, aisConfig.getPgn129040().getIntervalMilliseconds()));
      if (aisConfig.getPgn129809().isEnabled()) handlers.add(new Ais129809Handler(parser, config, aisConfig.getPgn129809().getIntervalMilliseconds()));
      if (aisConfig.getPgn129810().isEnabled()) handlers.add(new Ais129810Handler(parser, config, aisConfig.getPgn129810().getIntervalMilliseconds()));
    }
  }
  public void close() {
    MessageDaemon.getInstance().getSubSystemManager().getTwinManager().removeObserver(this);
  }

  /**
   * Rename this method to match the actual TwinObserver callback in your codebase.
   */
  @Override
  public void onTwinUpdated(String twinId, EntityTwin current, TwinUpdateContext context) {
    if (!(current instanceof DroneTwin droneTwin)) {
      return;
    }

    if (twinId == null || twinId.isBlank()) {
      return;
    }

    DroneEmissionState droneEmissionState = droneStateMap.computeIfAbsent(twinId, ignored -> new DroneEmissionState());

    long now = System.currentTimeMillis();
    for (AbstractDronePgnHandler handler : handlers) {
      try {
        handler.emit(droneTwin, droneEmissionState, now).ifPresent(this::safeSendMessage);
      }
      catch (Exception exception) {
        // Log this
      }
    }
  }

  /**
   * Rename this method to match the actual TwinObserver callback in your codebase.
   */
  public void onTwinRemoved(String twinId) {
    if (twinId != null && !twinId.isBlank()) {
      droneStateMap.remove(twinId);
    }
  }

  private void safeSendMessage(PgnEmission pgnEmission) {
    try {
      sendMessage(pgnEmission);
    }
    catch (IOException ioException) {
      // Log this
    }
  }

  private void sendMessage(PgnEmission pgnEmission) throws IOException {
    if (pgnEmission == null || pgnEmission.getPayload() == null || pgnEmission.getPayload().length == 0) {
      return;
    }
    n2kProtocol.writePgn(pgnEmission.getPgn(), 0xff, pgnEmission.getPayload());
  }
}
