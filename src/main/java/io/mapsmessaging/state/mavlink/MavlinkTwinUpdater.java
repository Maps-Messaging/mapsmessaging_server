/*
 *
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

package io.mapsmessaging.state.mavlink;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.VehicleClass;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinReadinessEvaluator;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapEventPublisher;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapProfile;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapStateEngine;
import io.mapsmessaging.state.mavlink.listener.ListenerManager;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_STATE_TWIN_CREATED;

public class MavlinkTwinUpdater {

  private final Logger logger = LoggerFactory.getLogger(MavlinkTwinUpdater.class);

  private final TwinManager twinManager;
  private final ListenerManager listenerManager;
  private final MavlinkDroneMonitor droneMonitor;

  public MavlinkTwinUpdater(
      @NonNull @NotNull TwinManager twinManager,
      @NonNull @NotNull ListenerManager listenerManager
  ) {
    this.twinManager = twinManager;
    this.listenerManager = listenerManager;
    this.droneMonitor = new MavlinkDroneMonitor(
        twinManager,
        new DroneTwinReadinessEvaluator(),
        new MavlinkBootstrapStateEngine(new MavlinkBootstrapProfile()),
        null
    );
    twinManager.addObserver(droneMonitor);
  }

  public void updateTwinState(
      @NonNull @NotNull MessageEvent messageEvent,
      @NonNull @NotNull ProcessedFrame env,
      @NonNull @NotNull MavlinkPacket packet,
      @NonNull @NotNull TwinUpdateContext context,
      MavlinkKnownSourceDTO knownSource,
      @NonNull @NotNull MavlinkStateSubscriptionMode subscriptionMode
  ) {
    String twinId = buildTwinId(env, knownSource);

    EntityTwin twin = twinManager.getTwin(twinId)
        .orElseGet(() -> createTwin(twinId, env, context, knownSource));

    twinManager.updateTwin(twinId, twinToUpdate -> {
      if (twinToUpdate instanceof DroneTwin drone) {
        drone.setSystemId(env.getFrame().getSystemId());
        drone.setComponentId(env.getFrame().getComponentId());
      }
    }, context);

    if (subscriptionMode.includesTwinState()) {
      listenerManager.handle(env.getFrame().getMessageId(), twinId, packet, context);
    }
  }

  private EntityTwin createTwin(
      String twinId,
      ProcessedFrame env,
      TwinUpdateContext context,
      MavlinkKnownSourceDTO knownSource
  ) {
    DroneTwin droneTwin = new DroneTwin(twinId);
    droneTwin.setVehicleClass(resolveVehicleClass(knownSource));
    droneTwin.setDescription(resolveDescription(twinId, env, knownSource));
    droneTwin.setCallSign(resolveCallSign(twinId, knownSource));
    droneTwin.setDisplayName(resolveDisplayName(twinId, knownSource));
    droneTwin.setSystemId(env.getFrame().getSystemId());
    droneTwin.setComponentId(env.getFrame().getComponentId());

    twinManager.registerTwin(droneTwin, context);

    logger.log(
        MAVLINK_STATE_TWIN_CREATED,
        twinId,
        env.getFrame().getSystemId(),
        env.getFrame().getComponentId()
    );

    return droneTwin;
  }

  private VehicleClass resolveVehicleClass(MavlinkKnownSourceDTO knownSource) {
    if (knownSource == null || knownSource.getVehicleClass() == null) {
      return VehicleClass.UAV;
    }

    return knownSource.getVehicleClass();
  }

  private String resolveDescription(
      String twinId,
      ProcessedFrame env,
      MavlinkKnownSourceDTO knownSource
  ) {
    if (knownSource != null && knownSource.getDescription() != null && !knownSource.getDescription().isBlank()) {
      return knownSource.getDescription();
    }

    return "MAVLink system " + env.getFrame().getSystemId() + " component " + env.getFrame().getComponentId();
  }

  private String resolveCallSign(String twinId, MavlinkKnownSourceDTO knownSource) {
    if (knownSource != null && knownSource.getName() != null && !knownSource.getName().isBlank()) {
      return knownSource.getName();
    }

    if (twinId.length() > 7) {
      return twinId.substring(twinId.length() - 7);
    }

    return twinId;
  }

  private String resolveDisplayName(String twinId, MavlinkKnownSourceDTO knownSource) {
    if (knownSource != null && knownSource.getDescription() != null && !knownSource.getDescription().isBlank()) {
      return knownSource.getDescription();
    }

    if (knownSource != null && knownSource.getName() != null && !knownSource.getName().isBlank()) {
      return knownSource.getName();
    }

    return twinId;
  }

  private String buildTwinId(ProcessedFrame env, MavlinkKnownSourceDTO knownSource) {
    if (knownSource != null && knownSource.getName() != null && !knownSource.getName().isBlank()) {
      return "mavlink:" + knownSource.getName();
    }

    return "mavlink:"
        + env.getFrame().getSystemId()
        + ":"
        + env.getFrame().getComponentId();
  }
}