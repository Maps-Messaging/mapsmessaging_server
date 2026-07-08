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

import com.google.gson.JsonObject;
import io.mapsmessaging.state.config.DroneInfo;
import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.n2k.listener.N2kJsonListener;
import io.mapsmessaging.state.n2k.listener.N2kJsonListenerRegistry;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class N2kTwinUpdater {

  private static final VehicleClass DEFAULT_VEHICLE_CLASS = VehicleClass.USV;

  private final TwinManager twinManager;
  private final N2kJsonListenerRegistry listenerRegistry;

  public N2kTwinUpdater(@NonNull @NotNull TwinManager twinManager) {
    this.twinManager = twinManager;
    this.listenerRegistry = new N2kJsonListenerRegistry();
  }

  public void updateTwinState(int pgn, @NonNull @NotNull JsonObject packet, @NonNull @NotNull TwinUpdateContext context, @NonNull @NotNull N2KTwinConfig config, DroneInfo droneInfo) {
    String twinId = buildTwinId(config);
    N2kJsonListener listener = listenerRegistry.getListener(pgn);

    twinManager.getTwin(twinId).orElseGet(() -> createTwin(twinId, config, context, droneInfo));

    twinManager.updateTwin(twinId, twinToUpdate -> {
      if (twinToUpdate instanceof DroneTwin droneTwin) {
        updateTwinIdentity(droneTwin, config, context);
        updateTwinResponseTopic(droneTwin, context.getResponseTopic());
        droneTwin.setUniqueOutboundIdentifier(context.getUniqueOutboundIdentifier());

        if (listener != null) {
          listener.handle(droneTwin, packet, context);
        }
      }
    }, context);
  }

  private EntityTwin createTwin(String twinId, N2KTwinConfig config, TwinUpdateContext context, DroneInfo droneInfo) {
    DroneTwin droneTwin = new DroneTwin(twinId);
    updateTwinIdentity(droneTwin, config, context);
    if (droneInfo.getCapabilities() != null) {
      droneTwin.setCapabilities(droneInfo.getCapabilities());
      droneTwin.setDescription(droneInfo.getDescription());
    }

    if (droneInfo.getBatteryCapacityHours() > 0) {
      droneTwin.setBatteryCapacityHours(droneInfo.getBatteryCapacityHours());
    } else if (droneInfo.getBatteryCapacityAh() > 0) {

    }


    twinManager.registerTwin(droneTwin, context);
    return droneTwin;
  }

  private void updateTwinIdentity(DroneTwin droneTwin, N2KTwinConfig config, TwinUpdateContext context) {
    Instant now = resolveTimestamp(context);

    droneTwin.setDisplayName(resolveDisplayName(droneTwin.getTwinId(), config));
    droneTwin.setDescriptionString(resolveDescription(droneTwin.getTwinId(), config));
    droneTwin.setCallSign(resolveCallSign(droneTwin.getTwinId(), config));
    droneTwin.setVehicleClass(resolveVehicleClass(config));
    droneTwin.setIdentityUpdatedAt(now);
    droneTwin.setLastSeenAt(now);

    if (config.getTopic() != null && !config.getTopic().isBlank()) {
      droneTwin.getAttributes().put("n2k.subscription.topic", config.getTopic());
    }
  }

  private void updateTwinResponseTopic(EntityTwin twin, String responseTopic) {
    if (responseTopic == null || responseTopic.isBlank()) {
      return;
    }

    String currentResponseTopic = twin.getResponseTopicName();
    if (currentResponseTopic == null || currentResponseTopic.isBlank()) {
      twin.setResponseTopicName(responseTopic);
    }
  }

  private String buildTwinId(N2KTwinConfig config) {
    if (config.getName() != null && !config.getName().isBlank()) {
      return "n2k:" + normalizeTwinId(config.getName());
    }

    if (config.getTopic() != null && !config.getTopic().isBlank()) {
      return "n2k:" + normalizeTwinId(config.getTopic());
    }

    return "n2k:default";
  }

  private String resolveDisplayName(String twinId, N2KTwinConfig config) {
    if (config.getName() != null && !config.getName().isBlank()) {
      return config.getName();
    }

    return twinId;
  }

  private String resolveDescription(String twinId, N2KTwinConfig config) {
//    if (config.getDescription() != null && !config.getDescription().isBlank()) {
//      return config.getDescription();
//    }

    if (config.getName() != null && !config.getName().isBlank()) {
      return "N2K STANAG feed " + config.getName();
    }

    return "N2K STANAG feed " + twinId;
  }

  private String resolveCallSign(String twinId, N2KTwinConfig config) {
    if (config.getName() != null && !config.getName().isBlank()) {
      return config.getName();
    }

    return twinId;
  }

  private String trimCallSign(String value) {
    String normalizedValue = normalizeTwinId(value);
    if (normalizedValue.length() > 7) {
      return normalizedValue.substring(normalizedValue.length() - 7);
    }

    return normalizedValue;
  }

  private VehicleClass resolveVehicleClass(N2KTwinConfig config) {
    if (config.getVehicleClass() == null || config.getVehicleClass().isBlank()) {
      return DEFAULT_VEHICLE_CLASS;
    }

    try {
      return VehicleClass.valueOf(config.getVehicleClass().trim().toUpperCase());
    } catch (IllegalArgumentException illegalArgumentException) {
      return DEFAULT_VEHICLE_CLASS;
    }
  }

  private String normalizeTwinId(String value) {
    return value.trim()
        .replace('/', '-')
        .replace('#', '_')
        .replace('+', '_')
        .replaceAll("-+", "-")
        .replaceAll("^-", "")
        .replaceAll("-$", "");
  }

  private Instant resolveTimestamp(TwinUpdateContext context) {
    if (context.getReceivedTime() != null) {
      return context.getReceivedTime();
    }

    return Instant.now();
  }
}