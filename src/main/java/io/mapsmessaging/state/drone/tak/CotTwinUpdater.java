/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinFieldObservation;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObservationOrigin;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.FixInfo;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakDetail;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import io.mapsmessaging.state.drone.tak.model.TakPoint;
import io.mapsmessaging.state.drone.tak.model.TakTrack;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CotTwinUpdater {

  private final TwinManager twinManager;
  private final DroneInfoRegistry droneInfoRegistry;
  private final CotIdentityRegistry identityRegistry;
  private final CotEventClassifier classifier;

  public CotTwinUpdater(
      TwinManager twinManager,
      DroneInfoRegistry droneInfoRegistry,
      CotIdentityRegistry identityRegistry) {
    this.twinManager = Objects.requireNonNull(twinManager, "twinManager must not be null");
    this.droneInfoRegistry = Objects.requireNonNull(droneInfoRegistry, "droneInfoRegistry must not be null");
    this.identityRegistry = Objects.requireNonNull(identityRegistry, "identityRegistry must not be null");
    this.classifier = new CotEventClassifier();
  }

  public CotUpdateResult update(String endpoint, TakEvent event, Instant receivedAt) {
    CotIdentityBinding binding = identityRegistry.resolve(endpoint, event.getUid()).orElse(null);
    CotObjectClass objectClass = classifier.classify(event, binding != null);
    if (objectClass != CotObjectClass.MANAGED_PLATFORM) {
      return new CotUpdateResult(objectClass, null, false, List.of());
    }

    DroneInfoDTO droneInfo = droneInfoRegistry.getDroneInfo(binding.twinId());
    if (droneInfo == null) {
      return new CotUpdateResult(
          objectClass,
          binding.twinId(),
          false,
          List.of("configured CoT alias references an unknown twin"));
    }

    Instant eventTime = Instant.parse(event.getTime());
    Instant staleTime = Instant.parse(event.getStale());
    Instant effectiveReceivedAt = receivedAt == null ? Instant.now() : receivedAt;
    TwinUpdateContext context = context(endpoint, event, eventTime, effectiveReceivedAt);
    ensureTwin(binding, droneInfo, context);

    List<String> warnings = new ArrayList<>();
    boolean[] changed = new boolean[1];
    twinManager.updateTwin(
        binding.twinId(),
        twin -> applyEvent(twin, binding, event, eventTime, staleTime, effectiveReceivedAt, warnings, changed),
        context);
    return new CotUpdateResult(objectClass, binding.twinId(), changed[0], List.copyOf(warnings));
  }

  private void ensureTwin(
      CotIdentityBinding binding, DroneInfoDTO droneInfo, TwinUpdateContext context) {
    if (twinManager.getTwin(binding.twinId()).isPresent()) {
      return;
    }
    DroneTwin twin = new DroneTwin(binding.twinId(), droneInfo.getUuid());
    twin.setDisplayName(droneInfo.getName());
    twin.setCallSign(droneInfo.getName());
    twin.setModelName(droneInfo.getModelName());
    twin.setAltitudeMode(droneInfo.getAltitudeMode());
    twin.setAltitudeMeters(droneInfo.getAltitudeMeters());
    twin.setCapabilities(droneInfo.getCapabilities());
    twin.setDescription(droneInfo.getDescription());
    twinManager.registerTwin(twin, context);
  }

  private void applyEvent(
      EntityTwin entityTwin,
      CotIdentityBinding binding,
      TakEvent event,
      Instant eventTime,
      Instant staleTime,
      Instant receivedAt,
      List<String> warnings,
      boolean[] changed) {
    if (!(entityTwin instanceof DroneTwin twin)) {
      warnings.add("configured CoT alias resolved to a non-drone twin");
      return;
    }
    TakPoint point = event.getPoint();
    if (point != null) {
      applyPosition(twin, binding, event, point, eventTime, staleTime, receivedAt, warnings, changed);
      applyAccuracy(twin, binding, event, point, eventTime, staleTime, receivedAt, changed);
    }
    TakDetail detail = event.getDetail();
    if (detail != null) {
      if (detail.getContact() != null) {
        apply(twin, binding, event, "identity.callsign", detail.getContact().getCallsign(), eventTime, staleTime, receivedAt,
            value -> twin.setCallSign((String) value), changed);
      }
      TakTrack track = detail.getTrack();
      if (track != null) {
        apply(twin, binding, event, "motion.speed", track.getSpeed(), eventTime, staleTime, receivedAt,
            value -> twin.setGroundSpeedMetersPerSecond((Double) value), changed);
        apply(twin, binding, event, "motion.course", track.getCourse(), eventTime, staleTime, receivedAt,
            value -> twin.setCourseOverGroundDegrees((Double) value), changed);
      }
      if (detail.getExtensions() != null && !detail.getExtensions().isEmpty()) {
        apply(twin, binding, event, "extensions.cot", List.copyOf(detail.getExtensions()), eventTime, staleTime, receivedAt,
            value -> twin.getAttributes().put("translation.cot.extensions", Integer.toString(((List<?>) value).size())), changed);
      }
    }
    twin.setNavigationUpdatedAt(eventTime);
    twin.setMotionUpdatedAt(eventTime);
    twin.setValidTill(staleTime);
  }

  private void applyPosition(
      DroneTwin twin,
      CotIdentityBinding binding,
      TakEvent event,
      TakPoint point,
      Instant eventTime,
      Instant staleTime,
      Instant receivedAt,
      List<String> warnings,
      boolean[] changed) {
    apply(twin, binding, event, "position.latitude", point.getLat(), eventTime, staleTime, receivedAt,
        value -> position(twin).setLatitude((Double) value), changed);
    apply(twin, binding, event, "position.longitude", point.getLon(), eventTime, staleTime, receivedAt,
        value -> position(twin).setLongitude((Double) value), changed);
    if (point.getHae() != null) {
      if (binding.haeToMslOffsetMeters() == null) {
        warnings.add("height above ellipsoid omitted because haeToMslOffsetMeters is not configured");
      } else {
        double altitudeMsl = point.getHae() + binding.haeToMslOffsetMeters();
        apply(twin, binding, event, "position.altitudeMsl", altitudeMsl, eventTime, staleTime, receivedAt,
            value -> position(twin).setAltitudeMslMeters((Double) value), changed);
      }
    }
  }

  private void applyAccuracy(
      DroneTwin twin,
      CotIdentityBinding binding,
      TakEvent event,
      TakPoint point,
      Instant eventTime,
      Instant staleTime,
      Instant receivedAt,
      boolean[] changed) {
    apply(twin, binding, event, "quality.horizontalAccuracy", point.getCe(), eventTime, staleTime, receivedAt,
        value -> fix(twin).setHorizontalAccuracyMeters((Double) value), changed);
    apply(twin, binding, event, "quality.verticalAccuracy", point.getLe(), eventTime, staleTime, receivedAt,
        value -> fix(twin).setVerticalAccuracyMeters((Double) value), changed);
  }

  private void apply(
      DroneTwin twin,
      CotIdentityBinding binding,
      TakEvent event,
      String field,
      Object value,
      Instant eventTime,
      Instant staleTime,
      Instant receivedAt,
      java.util.function.Consumer<Object> updater,
      boolean[] changed) {
    if (value == null) {
      return;
    }
    TwinFieldObservation observation = TwinFieldObservation.builder()
        .field(field)
        .value(value)
        .sourceProtocol("cot")
        .sourceEndpoint(binding.endpoint())
        .sourceId(event.getUid())
        .observedAt(eventTime)
        .receivedAt(receivedAt)
        .validUntil(staleTime)
        .priority(binding.sourcePriority())
        .origin(TwinObservationOrigin.OBSERVED)
        .quality(Map.of("how", event.getHow() == null ? "unknown" : event.getHow()))
        .build();
    if (twinManager.getObservationRegistry().record(twin.getTwinId(), observation, receivedAt)) {
      updater.accept(value);
      changed[0] = true;
    }
  }

  private GeoPosition position(DroneTwin twin) {
    if (twin.getGeoPosition() == null) {
      twin.setGeoPosition(new GeoPosition());
    }
    return twin.getGeoPosition();
  }

  private FixInfo fix(DroneTwin twin) {
    if (twin.getFixInfo() == null) {
      twin.setFixInfo(new FixInfo());
    }
    return twin.getFixInfo();
  }

  private TwinUpdateContext context(
      String endpoint, TakEvent event, Instant eventTime, Instant receivedAt) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setUpdateSource("cot");
    context.setSourceInstanceId("cot:" + endpoint + ":" + event.getUid());
    context.setEventTime(eventTime);
    context.setReceivedTime(receivedAt);
    context.setReason(event.getType());
    context.setFullSnapshot(false);
    return context;
  }
}
