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
package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.config.CotAffiliation;
import io.mapsmessaging.state.config.CotConfigDTO;
import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.FixInfo;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakContact;
import io.mapsmessaging.state.drone.tak.model.TakDetail;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import io.mapsmessaging.state.drone.tak.model.TakPoint;
import io.mapsmessaging.state.drone.tak.model.TakPrecisionLocation;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

final class CotEventPolicy {

  // Metrics, exposed to Grafana via the JMX->Prometheus exporter (see CotIntegrationJMX).
  private final LongAdder appliedCount = new LongAdder();
  private final LongAdder originalTypeFallbackCount = new LongAdder();
  private final LongAdder classificationOverrideCount = new LongAdder();
  private final LongAdder vehicleClassDerivedCount = new LongAdder();
  private final LongAdder unknownVehicleClassCount = new LongAdder();
  private final LongAdder mtiAffiliationOverrideCount = new LongAdder();
  private final LongAdder mtiReadinessDegradedCount = new LongAdder();
  private final LongAdder mtiCyberIconAppliedCount = new LongAdder();

  private static final long DEFAULT_STALE_TIMEOUT_MILLIS = 30_000L;
  private static final long CONTACT_STALE_TIMEOUT_MILLIS = 3_600_000L;
  private static final double DEFAULT_CE_METERS = 10.0d;
  private static final double DEFAULT_LE_METERS = 15.0d;
  private static final double UNKNOWN_ALTITUDE_METERS = 9_999_999.0d;
  private static final String DEFAULT_HOW = "h-g-i-g-o";
  private static final String CONTACT_HOW = "m-g";
  private static final String CONTACT_COT_TYPE = "a-u-U";
  // MIL-STD-2525 "battle dimension" character - the 3rd hyphen-separated segment of a CoT type
  // string (e.g. the "A" in "a-f-A-M-F-U"). Used to scope the cyber-compromise usericon override
  // to drones only, regardless of which ingest path resolved the twin's classification.
  private static final char AIR_BATTLE_DIMENSION = 'A';
  // Custom iconset a partner supplied for visualising compromised drones (WinTAK/ATAK only -
  // needs to be locally imported on the client; see MtiLookupResult.cyberIconFile). Fixed per
  // this exercise's TAK deployment - every client is expected to already have this exact
  // iconset imported under this exact id.
  private static final String CYBER_ICONSET_UUID = "8ed4bdba4a2ff2972685f3420274f87cc8e2d7547ba7262bce94d8991e7f7a9b";
  private static final String CYBER_ICON_GROUP = "cyber_icons";
  // Twin attribute set from mavlink.knownSources[].cotClassification (see MavlinkTwinUpdater) -
  // overrides the vehicleClass-derived classification segment for this specific asset.
  private static final String COT_CLASSIFICATION_ATTRIBUTE = "cotClassification";
  private static final int CONTACT_COLOR_ARGB_RED = -65536;
  private static final String DEFAULT_ALTITUDE_SOURCE = "GPS";
  private static final String DEFAULT_GEOPOINT_SOURCE = "GPS";

  void apply(TakEvent event, EntityTwin twin, TwinUpdateContext context, CotConfigDTO config) {
    apply(event, twin, context, config, false);
  }

  void applyRemoval(TakEvent event, EntityTwin twin, TwinUpdateContext context, CotConfigDTO config) {
    apply(event, twin, context, config, true);
  }

  private void apply(
      TakEvent event,
      EntityTwin twin,
      TwinUpdateContext context,
      CotConfigDTO config,
      boolean removal) {
    if (event == null || twin == null) {
      return;
    }
    appliedCount.increment();

    boolean contact = TwinType.CONTACT.equals(twin.getTwinType());
    MtiLookupResult mti = MtiStatusRegistry.lookup(twin.getTwinId());
    event.setUid(prefixUid(event.getUid(), config == null ? null : config.getUidPrefix()));
    String baseType = contact ? CONTACT_COT_TYPE : resolveBaseCotType(twin, config);
    event.setType(applyMtiAffiliation(baseType, mti));
    event.setHow(contact ? CONTACT_HOW : valueOrDefault(config == null ? null : config.getHow(), DEFAULT_HOW));

    if (removal) {
      ensureRemovalPoint(event, twin, config);
    } else {
      long staleTimeoutMillis =
          contact
              ? CONTACT_STALE_TIMEOUT_MILLIS
              : config == null ? DEFAULT_STALE_TIMEOUT_MILLIS : config.getStaleTimeoutMillis();
      if (staleTimeoutMillis < 1) {
        staleTimeoutMillis = DEFAULT_STALE_TIMEOUT_MILLIS;
      }
      Instant eventTime = resolveEventTime(event, context);
      if (eventTime != null) {
        event.setStale(eventTime.plusMillis(staleTimeoutMillis).toString());
      }
      applyPointDefaults(event.getPoint(), twin, config);
    }

    TakDetail detail = event.getDetail();
    if (detail != null) {
      TakPrecisionLocation precisionLocation = detail.getPrecisionLocation();
      if (precisionLocation == null) {
        precisionLocation = new TakPrecisionLocation();
        detail.setPrecisionLocation(precisionLocation);
      }
      precisionLocation.setAltsrc(
          valueOrDefault(config == null ? null : config.getAltitudeSource(), DEFAULT_ALTITUDE_SOURCE));
      precisionLocation.setGeopointsrc(DEFAULT_GEOPOINT_SOURCE);

      if (contact) {
        detail.setArchive(true);
        detail.setColorArgb(CONTACT_COLOR_ARGB_RED);
        applyContactDetail(detail, twin);
      }
      applyMtiDetail(detail, mti, baseType);
    }
  }

  /**
   * A twin created via the CoT-ingest route (CotToTwinMapper) carries the type it originally
   * arrived with - fall back to that, not the vehicle-class guess below, so an inbound CoT track
   * with no MTI match renders exactly as it was initially mapped, per the agreed MTI design
   * (delete/no-match = "route the message through as initially mapped"). Mavlink/N2K-sourced
   * twins never carry this attribute, so their fallback is unchanged.
   */
  private String resolveBaseCotType(EntityTwin twin, CotConfigDTO config) {
    String originalType = twin.getAttributes().get(CotToTwinMapper.ORIGINAL_COT_TYPE_ATTRIBUTE);
    if (originalType != null && !originalType.isBlank()) {
      originalTypeFallbackCount.increment();
      return originalType;
    }
    return resolveCotType(twin, config);
  }

  private String applyMtiAffiliation(String baseType, MtiLookupResult mti) {
    if (mti == null || mti.affiliationOverride() == null || mti.affiliationOverride().isBlank()) {
      return baseType;
    }
    String[] parts = baseType.split("-", 3);
    if (parts.length < 3) {
      return baseType;
    }
    mtiAffiliationOverrideCount.increment();
    return parts[0] + "-" + mti.affiliationOverride() + "-" + parts[2];
  }

  private void applyMtiDetail(TakDetail detail, MtiLookupResult mti, String baseType) {
    if (mti == null) {
      return;
    }
    if (mti.colorArgb() != null) {
      detail.setColorArgb(mti.colorArgb());
    }
    if (mti.remarksSuffix() != null && !mti.remarksSuffix().isBlank()) {
      String existing = detail.getRemarks();
      detail.setRemarks(existing == null || existing.isBlank()
          ? mti.remarksSuffix()
          : existing + " | " + mti.remarksSuffix());
    }
    if (mti.readiness() != null && detail.getStatus() != null) {
      detail.getStatus().setReadiness(mti.readiness());
      if (Boolean.FALSE.equals(mti.readiness())) {
        mtiReadinessDegradedCount.increment();
      }
    }
    if (mti.cyberIconFile() != null && !mti.cyberIconFile().isBlank() && isDroneClassification(baseType)) {
      detail.setUsericonIconsetPath(CYBER_ICONSET_UUID + "/" + CYBER_ICON_GROUP + "/" + mti.cyberIconFile());
      mtiCyberIconAppliedCount.increment();
    }
  }

  /**
   * Scopes the cyber-compromise usericon to drones only, by checking the CoT type's MIL-STD-2525
   * battle-dimension segment (the "A" in "a-f-A-M-F-U") rather than {@code TwinType}/
   * {@code VehicleClass} directly - a twin arriving via CoT ingest (see CotToTwinMapper) never
   * has a resolved {@code VehicleClass} of its own, only whatever classification its
   * {@code originalCotType} already carries, so checking the type string is the one thing that
   * works for a drone regardless of which ingest path produced it.
   */
  private boolean isDroneClassification(String baseType) {
    if (baseType == null) {
      return false;
    }
    String[] parts = baseType.split("-", 4);
    return parts.length >= 3 && parts[2].length() == 1 && parts[2].charAt(0) == AIR_BATTLE_DIMENSION;
  }

  private void applyContactDetail(TakDetail detail, EntityTwin twin) {
    Map<String, String> attributes = twin.getAttributes();
    String category = attributes.getOrDefault("contactCategory", "CONTACT");
    String detectionId = attributes.getOrDefault("sourceDetectionId", twin.getTwinId());

    TakContact contact = detail.getContact();
    if (contact == null) {
      contact = new TakContact();
      detail.setContact(contact);
    }
    contact.setCallsign(category + '-' + detectionId);

    StringBuilder remarks = new StringBuilder();
    String task = attributes.getOrDefault("requestedTaskType", "INSPECT");
    String specialization = attributes.get("requestedTaskSpecialization");
    String taskLabel = task + (specialization == null ? "" : " (" + specialization + ")");
    String status = twin.getLifecycleStatus() == null ? "ACTIVE" : twin.getLifecycleStatus().name();
    appendRemarkText(remarks, "Tasking: " + taskLabel + ". Status: " + status + ". Updated " + formatHhmmZ(twin.getLastSeenAt()) + ".");
    appendLabelledRemark(remarks, "probability", attributes.get("probabilityDisplay"));
    appendLabelledRemark(remarks, "depth", attributes.get("depthDisplay"));
    appendLabelledRemark(remarks, "size", attributes.get("sizeDisplay"));
    appendLabelledRemark(remarks, "source", attributes.get("sourceSensor"));
    appendLabelledRemark(remarks, "detection_id", attributes.get("sourceDetectionId"));
    detail.setRemarks(remarks.toString());
  }

  private String resolveCotType(EntityTwin twin, CotConfigDTO config) {
    String affiliation = resolveAffiliationCode(twin, config);
    // Per-asset override (set via mavlink.knownSources[].cotClassification, carried as a twin
    // attribute) - for distinguishing an unmanned platform from another asset that happens to
    // share the same VehicleClass but isn't actually the same kind of thing (e.g. a real manned
    // patrol boat vs. an unmanned surface vehicle, both configured as vehicleClass: USV).
    String classificationOverride = twin.getAttributes().get(COT_CLASSIFICATION_ATTRIBUTE);
    if (classificationOverride != null && !classificationOverride.isBlank()) {
      classificationOverrideCount.increment();
      return "a-" + affiliation + '-' + classificationOverride;
    }
    vehicleClassDerivedCount.increment();
    VehicleClass vehicleClass = resolveVehicleClass(twin);
    if (vehicleClass == VehicleClass.UNKNOWN) {
      unknownVehicleClassCount.increment();
    }
    String classification =
        switch (vehicleClass) {
          case UAV -> "A-M-F-U";
          // Field-tested 2026-09-15 against WebTAK: "S-X-M" (Sea Surface, unspecified equipment
          // type) has no real icon artwork in this icon set - friendly and unknown affiliation
          // rendered as the same generic fallback icon, making affiliation-based signalling
          // (e.g. the MTI "unknown" state) invisible for every USV twin. "S-C-P" (Sea Surface,
          // Combatant, Patrol - the closest real category to a RIB/patrol-type USV) has full
          // coverage: confirmed distinct, standard-colour icons (blue friendly / yellow unknown /
          // red hostile), same as the well-supported "A-M-F-U" UAV code already used above.
          case USV -> "S-C-P";
          case UGV -> "G-E-V";
          // Same fix as USV above, same reasoning: "U-X-M" (Subsurface, unspecified) had no real
          // icon coverage. "U-C" (Subsurface, Combatant) does - field-tested 2026-09-15, same
          // distinct blue/yellow/red affiliation colouring confirmed.
          case UUV -> "U-C";
          case GCS -> "G-U-C";
          case UNKNOWN -> "X";
        };
    return "a-" + affiliation + '-' + classification;
  }

  private VehicleClass resolveVehicleClass(EntityTwin twin) {
    if (twin instanceof DroneTwin droneTwin && droneTwin.getVehicleClass() != null) {
      return droneTwin.getVehicleClass();
    }
    if (TwinType.GROUND_CONTROL.equals(twin.getTwinType())) {
      return VehicleClass.GCS;
    }
    return VehicleClass.UNKNOWN;
  }

  private String resolveAffiliationCode(EntityTwin twin, CotConfigDTO config) {
    CotAffiliation affiliation = config == null ? CotAffiliation.FRIENDLY : config.getAffiliation();
    if (affiliation == null) {
      affiliation = CotAffiliation.SOURCE;
    }

    return switch (affiliation) {
      case FRIENDLY -> "f";
      case HOSTILE -> "h";
      case NEUTRAL -> "n";
      case UNKNOWN -> "u";
      case SOURCE -> resolveSourceAffiliation(twin);
    };
  }

  private String resolveSourceAffiliation(EntityTwin twin) {
    if (!(twin instanceof DroneTwin droneTwin)) {
      return "u";
    }

    Map<String, Object> description = droneTwin.getDescription();
    if (description == null || description.isEmpty()) {
      return "u";
    }

    Object value = firstValue(description, "standard_identity", "standardIdentity", "affiliation");
    if (value == null) {
      return "u";
    }

    String normalised = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    if (normalised.startsWith("STANDARDIDENTITYENUM_")) {
      normalised = normalised.substring("STANDARDIDENTITYENUM_".length());
    }

    return switch (normalised) {
      case "FRIEND", "FRIENDLY" -> "f";
      case "ASSUMED_FRIEND" -> "a";
      case "NEUTRAL" -> "n";
      case "SUSPECT" -> "s";
      case "HOSTILE" -> "h";
      case "PENDING" -> "p";
      default -> "u";
    };
  }

  private Object firstValue(Map<String, Object> values, String... keys) {
    for (String key : keys) {
      Object value = values.get(key);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private void ensureRemovalPoint(TakEvent event, EntityTwin twin, CotConfigDTO config) {
    TakPoint point = event.getPoint();
    if (point == null) {
      GeoPosition geoPosition = twin.getGeoPosition();
      if (geoPosition == null) {
        return;
      }
      point = new TakPoint();
      point.setLat(geoPosition.getLatitude());
      point.setLon(geoPosition.getLongitude());
      point.setHae(geoPosition.getAltitudeMslMeters());
      event.setPoint(point);
    }
    applyPointDefaults(point, twin, config);
  }

  private void applyPointDefaults(TakPoint point, EntityTwin twin, CotConfigDTO config) {
    if (point == null) {
      return;
    }

    if (TwinType.CONTACT.equals(twin.getTwinType())) {
      GeoPosition geoPosition = twin.getGeoPosition();
      if (geoPosition == null || geoPosition.getAltitudeMslMeters() == null) {
        point.setHae(UNKNOWN_ALTITUDE_METERS);
      }
    }

    FixInfo fixInfo = twin.getFixInfo();
    if (fixInfo == null
        || fixInfo.getHorizontalAccuracyMeters() == null
        || fixInfo.getHorizontalAccuracyMeters() <= 0.0d) {
      point.setCe(resolveError(config == null ? null : config.getDefaultCircularErrorMeters(), DEFAULT_CE_METERS));
    }
    if (fixInfo == null
        || fixInfo.getVerticalAccuracyMeters() == null
        || fixInfo.getVerticalAccuracyMeters() <= 0.0d) {
      point.setLe(resolveError(config == null ? null : config.getDefaultLinearErrorMeters(), DEFAULT_LE_METERS));
    }
  }

  private double resolveError(Double configuredValue, double defaultValue) {
    return configuredValue != null && Double.isFinite(configuredValue) && configuredValue >= 0.0d
        ? configuredValue
        : defaultValue;
  }

  private Instant resolveEventTime(TakEvent event, TwinUpdateContext context) {
    if (event.getTime() != null && !event.getTime().isBlank()) {
      try {
        return Instant.parse(event.getTime());
      } catch (RuntimeException ignored) {
      }
    }
    return context == null ? null : context.getEventTime();
  }

  private String prefixUid(String uid, String prefix) {
    if (prefix == null || prefix.isBlank()) {
      return uid;
    }
    return prefix + (uid == null ? "" : uid);
  }

  private void appendRemarkText(StringBuilder builder, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    if (!builder.isEmpty()) {
      builder.append(" | ");
    }
    builder.append(value);
  }

  private void appendLabelledRemark(StringBuilder builder, String label, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    if (!builder.isEmpty()) {
      builder.append(" | ");
    }
    builder.append(label).append('=').append(value);
  }

  private String formatHhmmZ(Instant instant) {
    if (instant == null) {
      return "unknown";
    }
    String iso = instant.truncatedTo(ChronoUnit.MINUTES).toString();
    return iso.substring(11, 13) + iso.substring(14, 16) + "Z";
  }

  private String valueOrDefault(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value;
  }

  // --- Metrics, read by CotIntegrationJMX. ---

  long getAppliedCount() {
    return appliedCount.sum();
  }

  long getOriginalTypeFallbackCount() {
    return originalTypeFallbackCount.sum();
  }

  long getClassificationOverrideCount() {
    return classificationOverrideCount.sum();
  }

  long getVehicleClassDerivedCount() {
    return vehicleClassDerivedCount.sum();
  }

  long getUnknownVehicleClassCount() {
    return unknownVehicleClassCount.sum();
  }

  long getMtiAffiliationOverrideCount() {
    return mtiAffiliationOverrideCount.sum();
  }

  long getMtiReadinessDegradedCount() {
    return mtiReadinessDegradedCount.sum();
  }

  long getMtiCyberIconAppliedCount() {
    return mtiCyberIconAppliedCount.sum();
  }
}
