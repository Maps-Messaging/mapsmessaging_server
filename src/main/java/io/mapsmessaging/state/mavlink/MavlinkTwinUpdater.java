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

import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_TWIN_CREATED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_TASK_COMPLETION_GATES_DISABLED;

import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.config.DataProductConfig;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.StopActionEnum;
import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.model.DroneContactManager;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinReadinessEvaluator;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapEventPublisher;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapProfile;
import io.mapsmessaging.state.mavlink.bootstrap.MavlinkBootstrapStateEngine;
import io.mapsmessaging.state.mavlink.listener.ListenerManager;
import io.mapsmessaging.state.mavlink.model.ModelManager;
import io.mapsmessaging.state.mavlink.model.UxvModel;
import io.mapsmessaging.state.mavlink.packet.BatteryStatusPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.sender.MavlinkEventListSender;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class MavlinkTwinUpdater implements AutoCloseable {

  private static final String COMPLETE_TASK_ON_ARRIVAL_TOLERANCE_ATTRIBUTE =
      "completeTaskOnArrivalTolerance";
  private static final String COMPLETE_TASK_ON_AUTO_TO_LOITER_ATTRIBUTE =
      "completeTaskOnAutoToLoiter";
  private static final String TAK_VIDEO_URLS_ATTRIBUTE = "tak.videoUrls";

  private final Logger logger = LoggerFactory.getLogger(MavlinkTwinUpdater.class);

  private final TwinManager twinManager;
  private final ListenerManager listenerManager;
  private final MavlinkDroneMonitor droneMonitor;
  private final AtomicBoolean closed;
  private final MavlinkIntegrationJMX integrationJMX;

  // Metrics, exposed to Grafana via the JMX->Prometheus exporter (see MavlinkIntegrationJMX).
  private final LongAdder messagesProcessedCount = new LongAdder();
  private final LongAdder twinsCreatedCount = new LongAdder();
  private final LongAdder classificationOverrideCount = new LongAdder();

  public MavlinkTwinUpdater(@NonNull @NotNull TwinManager twinManager, @NonNull @NotNull ListenerManager listenerManager) {
    this(twinManager, listenerManager, null, "mavlink");
  }

  public MavlinkTwinUpdater(
      @NonNull @NotNull TwinManager twinManager,
      @NonNull @NotNull ListenerManager listenerManager,
      MavlinkBootstrapEventPublisher bootstrapEventPublisher) {
    this(twinManager, listenerManager, bootstrapEventPublisher, "mavlink");
  }

  public MavlinkTwinUpdater(
      @NonNull @NotNull TwinManager twinManager,
      @NonNull @NotNull ListenerManager listenerManager,
      MavlinkBootstrapEventPublisher bootstrapEventPublisher,
      String integrationSource) {
    this.twinManager = twinManager;
    this.listenerManager = listenerManager;
    this.droneMonitor = new MavlinkDroneMonitor(
        twinManager,
        new DroneTwinReadinessEvaluator(),
        new MavlinkBootstrapStateEngine(new MavlinkBootstrapProfile()),
        bootstrapEventPublisher);
    this.closed = new AtomicBoolean();
    this.integrationJMX = new MavlinkIntegrationJMX(this, integrationSource);
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
    this.integrationJMX = new MavlinkIntegrationJMX(this, "mavlink");
    twinManager.addObserver(droneMonitor);
  }

  public void updateTwinState(@NonNull @NotNull ProcessedFrame env, @NonNull @NotNull MavlinkPacket packet, @NonNull @NotNull TwinUpdateContext context, @NonNull @NotNull MavlinkKnownSourceDTO knownSource, DroneInfoDTO droneInfo) {
    if (closed.get()) {
      return;
    }
    messagesProcessedCount.increment();

    String twinId = buildTwinId(env, knownSource);
    droneMonitor.beginTwinUpdate(twinId);
    try {
      EntityTwin entityTwin = twinManager.getTwin(twinId).orElseGet(() -> createTwin(twinId, env, context, knownSource, droneInfo));
      twinManager.updateTwin(
          twinId,
          twinToUpdate -> {
            if (twinToUpdate instanceof DroneTwin drone) {
              drone.setSystemId(knownSource.getSystemId());
              drone.setComponentId(knownSource.getComponentId());
              updateTwinResponseTopic(twinToUpdate, context.getResponseTopic());
              drone.setUniqueOutboundIdentifier(context.getUniqueOutboundIdentifier());
              applyTaskCompletionConfiguration(drone, droneInfo);
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
        applyModelDetectionEvent(droneTwin, packet, context, droneInfo);
      }
    } finally {
      droneMonitor.endTwinUpdate(twinId, context);
    }
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      integrationJMX.close();
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

  private void applyModelDetectionEvent(
      DroneTwin droneTwin,
      MavlinkPacket packet,
      TwinUpdateContext context,
      DroneInfoDTO droneInfo) {
    String modelName = droneTwin.getModelName();
    if (modelName == null || modelName.isBlank()) {
      return;
    }

    try {
      UxvModel uxvModel = ModelManager.getInstance().getRequiredModel(modelName);
      if (uxvModel != null) {
        Optional<DetectionEvent> detectionEvent = uxvModel.interpretDetection(droneTwin, packet);
        detectionEvent.ifPresent(
            event -> {
              attachVideoUrls(event, droneInfo);
              applyDetectionEvent(droneTwin, event, context);
            });
      }
    } catch (IllegalArgumentException e) {
      // no such model, ignore
    }
  }

  private void attachVideoUrls(DetectionEvent event, DroneInfoDTO droneInfo) {
    if (event == null || droneInfo == null) {
      return;
    }

    List<String> urls = new ArrayList<>();
    for (DataProductConfig dataProduct : droneInfo.getDataProducts()) {
      if (dataProduct == null || dataProduct.getUri() == null || dataProduct.getUri().isBlank()) {
        continue;
      }
      urls.add(dataProduct.getUri().trim());
    }
    if (!urls.isEmpty()) {
      event.addAttribute(TAK_VIDEO_URLS_ATTRIBUTE, List.copyOf(urls));
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

  private void applyTaskCompletionConfiguration(
      DroneTwin droneTwin,
      DroneInfoDTO droneInfo
  ) {
    droneTwin.getAttributes().put(
        COMPLETE_TASK_ON_ARRIVAL_TOLERANCE_ATTRIBUTE,
        Boolean.toString(droneInfo.isCompleteTaskOnArrivalTolerance()));
    droneTwin.getAttributes().put(
        COMPLETE_TASK_ON_AUTO_TO_LOITER_ATTRIBUTE,
        Boolean.toString(droneInfo.isCompleteTaskOnAutoToLoiter()));
  }

  private void applyDetectionEvent(
      DroneTwin droneTwin, DetectionEvent event, TwinUpdateContext context) {
    if (!isValidDetectionEvent(event)) {
      return;
    }

    DroneContactManager contactManager = droneTwin.getContactManager();

    switch (event.getEventType()) {
      case DETECTED, UPDATED -> {
        upsertContact(contactManager, event);
        if (isPublishableDetectionEvent(event)) {
          twinManager.notifyDetectionEvent(droneTwin, event, context);
        }
      }
      case LOST -> removeContact(contactManager, event);
    }
  }

  private boolean isPublishableDetectionEvent(DetectionEvent event) {
    return event.getPosition() != null
        && event.getTtlMillis() != null
        && event.getTtlMillis() > 0;
  }

  private boolean isValidDetectionEvent(DetectionEvent event) {
    return event != null && event.getEventType() != null && event.getContactId() != null;
  }

  private void upsertContact(DroneContactManager contactManager, DetectionEvent event) {
    Long ttlMillis = event.getTtlMillis();
    if (ttlMillis == null || ttlMillis <= 0) {
      return;
    }

    contactManager.updateContact(event.getContactId(), event.getName(), event.getPosition(), ttlMillis);
  }

  private void removeContact(DroneContactManager contactManager, DetectionEvent event) {
    if (contactManager.hasContact(event.getContactId())) {
      contactManager.removeContact(event.getContactId());
    }
  }

  private EntityTwin createTwin(String twinId, ProcessedFrame env, TwinUpdateContext context, MavlinkKnownSourceDTO knownSource, DroneInfoDTO droneInfo) {
    twinsCreatedCount.increment();
    DroneTwin droneTwin = new DroneTwin(twinId, droneInfo.getUuid());
    droneTwin.setVehicleClass(resolveVehicleClass(knownSource));
    if (knownSource.getCotClassification() != null && !knownSource.getCotClassification().isBlank()) {
      // See CotEventPolicy.COT_CLASSIFICATION_ATTRIBUTE - overrides the vehicleClass-derived
      // CoT classification for this specific asset (e.g. distinguishing it from another asset
      // sharing the same vehicleClass that isn't actually the same kind of thing).
      droneTwin.getAttributes().put("cotClassification", knownSource.getCotClassification());
      classificationOverrideCount.increment();
    }
    droneTwin.setDescriptionString(resolveDescription(twinId, env, knownSource));
    droneTwin.setCallSign(resolveCallSign(twinId, knownSource));
    droneTwin.setDisplayName(resolveDisplayName(twinId, knownSource));
    droneTwin.setSystemId(knownSource.getSystemId());
    droneTwin.setComponentId(knownSource.getComponentId());
    droneTwin.setModelName(droneInfo.getModelName());
    droneTwin.setAltitudeMode(droneInfo.getAltitudeMode());
    droneTwin.setAltitudeMeters(droneInfo.getAltitudeMeters());
    droneTwin.setSurveyRadiusMeters(droneInfo.getSurveyRadiusMeters());
    droneTwin.setArrivalToleranceMeters(droneInfo.getArrivalToleranceMeters());
    applyTaskCompletionConfiguration(droneTwin, droneInfo);
    if (!droneInfo.isCompleteTaskOnArrivalTolerance()
        && !droneInfo.isCompleteTaskOnAutoToLoiter()) {
      logger.log(MAVLINK_TASK_COMPLETION_GATES_DISABLED, twinId);
    }
    if (droneInfo.getStopAction() != null) {
      droneTwin.setStopAction(droneInfo.getStopAction());
    } else {
      droneTwin.setStopAction(StopActionEnum.STOP);
    }
    if (droneInfo.getCapabilities() != null) {
      droneTwin.setCapabilities(droneInfo.getCapabilities());
      droneTwin.setDescription(droneInfo.getDescription());
    }
    if (droneInfo.getSpecialization() != null) {
      droneTwin.setSpecialization(droneInfo.getSpecialization());
    }
    if (!droneInfo.getDataProducts().isEmpty()) {
      droneTwin.setDataProducts(droneInfo.getDataProducts());
    }

    if (droneInfo.getBatteryCapacityHours() > 0) {
      droneTwin.setBatteryCapacityHours(droneInfo.getBatteryCapacityHours());
    } else if (droneInfo.getBatteryCapacityAh() > 0) {

    }

    EntityTwin registeredTwin = twinManager.registerTwin(droneTwin, context);

    logger.log(
        MAVLINK_STATE_TWIN_CREATED,
        twinId,
        env.getFrame().getSystemId(),
        env.getFrame().getComponentId()
    );

    return registeredTwin;
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

    if (knownSource != null) {
      return "MAVLink system " + knownSource.getSystemId() + " component " + knownSource.getComponentId();
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

  // --- Metrics, read by MavlinkIntegrationJMX. ---

  long getMessagesProcessedCount() {
    return messagesProcessedCount.sum();
  }

  long getTwinsCreatedCount() {
    return twinsCreatedCount.sum();
  }

  long getClassificationOverrideCount() {
    return classificationOverrideCount.sum();
  }
}
