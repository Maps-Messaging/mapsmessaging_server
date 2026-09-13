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

import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.network.protocol.impl.n2k.N2kProtocol;
import io.mapsmessaging.state.config.n2k.N2KAisConfigDTO;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.n2k.handler.AbstractDronePgnHandler;
import io.mapsmessaging.state.n2k.handler.Ais129039Handler;
import io.mapsmessaging.state.n2k.handler.Ais129040Handler;
import io.mapsmessaging.state.n2k.handler.Ais129809Handler;
import io.mapsmessaging.state.n2k.handler.Ais129810Handler;
import io.mapsmessaging.state.n2k.handler.PgnEmission;
import io.mapsmessaging.state.n2k.msg.AisClassBEmitterConfig;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public class DroneMonitor implements TwinObserver {

  private final TwinManager twinManager;
  @Getter
  private final N2kProtocol n2kProtocol;
  private final List<AbstractDronePgnHandler> handlers;
  private final Map<String, DroneEmissionState> droneStateMap;
  private final LongSupplier currentTimeMillis;

  public DroneMonitor(TwinManager twinManager, N2KAisConfigDTO aisConfig, N2kMessageParser parser, N2kProtocol n2kProtocol) {
    this(twinManager, n2kProtocol, buildHandlers(aisConfig, parser), System::currentTimeMillis);
  }

  DroneMonitor(TwinManager twinManager, N2kProtocol n2kProtocol, List<AbstractDronePgnHandler> handlers, LongSupplier currentTimeMillis) {
    this.droneStateMap = new ConcurrentHashMap<>();
    this.twinManager = Objects.requireNonNull(twinManager, "twinManager must not be null");
    this.n2kProtocol = Objects.requireNonNull(n2kProtocol, "n2kProtocol must not be null");
    this.handlers = List.copyOf(Objects.requireNonNull(handlers, "handlers must not be null"));
    this.currentTimeMillis = Objects.requireNonNull(currentTimeMillis, "currentTimeMillis must not be null");
  }

  private static List<AbstractDronePgnHandler> buildHandlers(N2KAisConfigDTO aisConfig, N2kMessageParser parser) {
    N2KAisConfigDTO effectiveConfig = aisConfig == null ? new N2KAisConfigDTO() : aisConfig;
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    List<AbstractDronePgnHandler> configuredHandlers = new ArrayList<>();

    if (effectiveConfig.getPgn129039().isEnabled()) {
      configuredHandlers.add(new Ais129039Handler(parser, config, effectiveConfig.getPgn129039().getIntervalMilliseconds()));
    }
    if (effectiveConfig.getPgn129040().isEnabled()) {
      configuredHandlers.add(new Ais129040Handler(parser, config, effectiveConfig.getPgn129040().getIntervalMilliseconds()));
    }
    if (effectiveConfig.getPgn129809().isEnabled()) {
      configuredHandlers.add(new Ais129809Handler(parser, config, effectiveConfig.getPgn129809().getIntervalMilliseconds()));
    }
    if (effectiveConfig.getPgn129810().isEnabled()) {
      configuredHandlers.add(new Ais129810Handler(parser, config, effectiveConfig.getPgn129810().getIntervalMilliseconds()));
    }
    return configuredHandlers;
  }

  public void close() {
    twinManager.removeObserver(this);
    droneStateMap.clear();
  }

  @Override
  public void onTwinUpdated(String twinId, EntityTwin current, TwinUpdateContext context) {
    if (!(current instanceof DroneTwin droneTwin)) {
      return;
    }
    if (twinId == null || twinId.isBlank()) {
      return;
    }

    DroneEmissionState droneEmissionState = droneStateMap.computeIfAbsent(twinId, ignored -> new DroneEmissionState());
    long now = currentTimeMillis.getAsLong();
    for (AbstractDronePgnHandler handler : handlers) {
      try {
        handler.emit(droneTwin, droneEmissionState, now).ifPresent(this::safeSendMessage);
      } catch (Exception exception) {
        // A handler must not prevent the remaining configured PGNs from being evaluated.
      }
    }
  }

  @Override
  public void onTwinRemoved(EntityTwin removed, TwinUpdateContext context) {
    if (removed != null && removed.getTwinId() != null) {
      droneStateMap.remove(removed.getTwinId());
    }
  }

  private void safeSendMessage(PgnEmission pgnEmission) {
    try {
      sendMessage(pgnEmission);
    } catch (IOException ioException) {
      // A failed CAN write must not break twin observer callbacks.
    }
  }

  private void sendMessage(PgnEmission pgnEmission) throws IOException {
    if (pgnEmission == null || pgnEmission.getPayload() == null || pgnEmission.getPayload().length == 0) {
      return;
    }
    n2kProtocol.writePgn(pgnEmission.getPgn(), 0xff, pgnEmission.getPayload());
  }
}
