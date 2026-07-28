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

package io.mapsmessaging.state.mavlink;

import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.StopActionEnum;
import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.Contact;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.model.DroneContactManager;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinReadinessEvaluator;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapProfile;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapStateEngine;
import io.mapsmessaging.state.mavlink.listener.ListenerManager;
import io.mapsmessaging.state.mavlink.model.ModelManager;
import io.mapsmessaging.state.mavlink.model.UxvModel;
import io.mapsmessaging.state.mavlink.packet.BatteryStatusPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.sender.MavlinkEventListSender;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_TWIN_CREATED;

public class MavlinkTwinUpdater implements AutoCloseable {

  private final Logger logger = LoggerFactory.getLogger(MavlinkTwinUpdater.class);

  private final TwinManager twinManager;
  private final ListenerManager listenerManager;
  private final MavlinkDroneMonitor droneMonitor;
  private final AtomicBoolean closed;

  public MavlinkTwinUpdater(@NonNull @NotNull TwinManager twinManager, @NonNull @NotNull ListenerManager listenerManager) {
    this.twinManager = twinManager;
    this.listenerManager = listenerManager;
    this.droneMonitor = new MavlinkDroneMonitor(twinManager, new DroneTwinReadinessEvaluator(), new MavlinkBootstrapStateEngine(new MavlinkBootstrapProfile()), null);
    this.closed = new AtomicBoolean();
    twinManager.addObserver(droneMonitor);
  }

  MavlinkTwinUpdater(
      TwinManager twinManager,
      ListenerManager listenerManager,
      MavlinkDroneMonitor droneMonitor
  ) {
    this.twinManager = Objects.requireNonNull(twinManager, "twinManager must not be null");
    this.listenerManager = Objects.requireNonNull(listenerManager, "listenerManager must not be null");
    this.droneMonitor = Objects.requireNonNull(droneMonitor, "droneMonitor must not be null");
    this.closed = new AtomicBoolean();
    twinManager.addObserver(droneMonitor);
  }

  public void updateTwinState(@NonNull @NotNull ProcessedFrame env, @NonNull @NotNull MavlinkPacket packet, @NonNull @NotNull TwinUpdateContext context, @NonNull @NotNull MavlinkKnownSourceDTO knownSource, DroneInfoDTO droneInfo) {
    if (closed.get()) {
      return;
    }

    String twinId = buildTwinId(env, knownSource);
    EntityTwin entityTwin = twinManager.getTwin(twinId).orElseGet(() -> createTwin(twinId, env, context, knownSource, droneInfo));
    twinManager.updateTwin(
        twinId,
        twinToUpdate -> {
          if (twinToUpdate instanceof DroneTwin drone) {
            drone.setSystemId(env.getFrame().getSystemId());
            drone.setComponentId(env.getFrame().getComponentId());
            updateTwinResponseTopic(twinToUpdate, context.getResponseTopic());
            drone.setUniqueOutboundIdentifier(context.getUniqueOutboundIdentifier());
            updateMessageFreshness(drone, packet, context);
          }
        },
        context
    );

    listenerManager.handle(env.getFrame().getMessageId(), twinId, packet, context);

    if (entityTwin instanceof DroneTwin droneTwin) {
      MavlinkEventListSender sender = droneTwin.getActiveMavlinkSender();
      if (sender != null) {
        sender.onMavlinkMessage(packet);
      }
      applyModelDetectionEvent(droneTwin, packet);
    }
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      droneMonitor.close();
    }
  }

  private void updateMessageFreshness(
      DroneTwin droneTwin,
      MavlinkPacket packet,
      TwinUpdateContext context
  ) {
    if (packet instanceof BatteryStatusPacket batteryStatusPacket
        && batteryStatusPacket.isValid()
        && context.getReceivedTime() != null) {
      droneTwin.setPowerUpdatedAt(context.getReceivedTime());
    }
  }

  private void applyModelDetectionEvent(DroneTwin droneTwin, MavlinkPacket packet) {
    String modelName = droneTwin.getModelName();
    if (modelName == null || modelName.isBlank()) {
      return;
    }

    try {
      UxvModel uxvModel = ModelManager.getInstance().getRequiredModel(modelName);
      if (uxvModel != null) {
        Optional<DetectionEvent> detectionEvent = uxvModel.interpretDetection(droneTwin, packet);
        detectionEvent.ifPresent(event -> applyDetectionEvent(droneTwin, event));
      }
    } catch (IllegalArgumentException e) {
      // no such model, ignore
    }
  }

  private void updateTwinResponseTopic(EntityTwin twin, String responseTopic) {
    if (responseTopic == null || responseTopic.isEmpty()) {
      return;
    }

    String currentResponseTopic = twin.getResponseTopicName();
    if (currentResponseTopic == null || currentResponseTopic.isEmpty()) {
      twin.setResponseTopicName(responseTopic);
    }
  }

  private void applyDetectionEvent(DroneTwin droneTwin, DetectionEvent event) {
    if (!isValidDetectionEvent(event)) {
      return;
    }

    DroneContactManager contactManager = droneTwin.getContactManager();

    switch (event.getEventType()) {
      case DETECTED, UPDATED -> upsertContact(contactManager, event);
      case LOST -> removeContact(contactManager, event);
    }
  }

  private boolean isValidDetectionEvent(DetectionEvent event) {
    return event != null && event.getEventType() != null && event.getContactId() != null;
  }

  private void upsertContact(DroneContactManager contactManager, DetectionEvent event) {
    Long ttlMillis = event.getTtlMillis();
    if (ttlMillis == null || ttlMillis <= 0) {
      return;
    }

    GeoPosition position = event.getPosition();
    String name = event.getName();

    if (contactManager.hasContact(event.getContactId())) {
      contactManager.updateContact(event.getContactId(), name, position, ttlMillis);
    } else {
      Contact contact = new Contact(name, position, ttlMillis);
      contactManager.addContact(contact);
    }
  }

  private void removeContact(DroneContactManager contactManager, DetectionEvent event) {
    if (contactManager.hasContact(event.getContactId())) {
      contactManager.removeContact(event.getContactId());
    }
  }

  private EntityTwin createTwin(String twinId, ProcessedFrame env, TwinUpdateContext context, MavlinkKnownSourceDTO knownSource, DroneInfoDTO droneInfo) {
    DroneTwin droneTwin = new DroneTwin(twinId, droneInfo.getUuid());
    droneTwin.setVehicleClass(resolveVehicleClass(knownSource));
    droneTwin.setDescriptionString(resolveDescription(twinId, env, knownSource));
    droneTwin.setCallSign(resolveCallSign(twinId, knownSource));
    droneTwin.setDisplayName(resolveDisplayName(twinId, knownSource));
    droneTwin.setSystemId(env.getFrame().getSystemId());
    droneTwin.setComponentId(env.getFrame().getComponentId());
    droneTwin.setModelName(droneInfo.getModelName());
    if (droneInfo.getStopAction() != null) {
      droneTwin.setStopAction(droneInfo.getStopAction());
    } else {
      droneTwin.setStopAction(StopActionEnum.STOP);
    }
    if (droneInfo.getCapabilities() != null) {
      droneTwin.setCapabilities(droneInfo.getCapabilities());
      droneTwin.setDescription(droneInfo.getDescription());
    }

    if (droneInfo.getBatteryCapacityHours() > 0) {
      droneTwin.setBatteryCapacityHours(droneInfo.getBatteryCapacityHours());
    } else if (droneInfo.getBatteryCapacityAh() > 0) {

    }

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
      return knownSource.getName();
    }

    return "mavlink-" + env.getFrame().getSystemId() + ":" + env.getFrame().getComponentId();
  }
}
