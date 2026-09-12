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
import io.mapsmessaging.state.drone.tak.model.TakDetail;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import io.mapsmessaging.state.drone.tak.model.TakPoint;
import io.mapsmessaging.state.drone.tak.model.TakPrecisionLocation;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

final class CotEventPolicy {

  private static final long DEFAULT_STALE_TIMEOUT_MILLIS = 30_000L;
  private static final double DEFAULT_CE_METERS = 10.0d;
  private static final double DEFAULT_LE_METERS = 15.0d;
  private static final String DEFAULT_HOW = "h-g-i-g-o";
  private static final String DEFAULT_ALTITUDE_SOURCE = "GPS";

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

    event.setType(resolveCotType(twin, config));
    event.setHow(valueOrDefault(config == null ? null : config.getHow(), DEFAULT_HOW));
    event.setUid(prefixUid(event.getUid(), config == null ? null : config.getUidPrefix()));

    if (!removal) {
      long staleTimeoutMillis = config == null ? DEFAULT_STALE_TIMEOUT_MILLIS : config.getStaleTimeoutMillis();
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
    }
  }

  private String resolveCotType(EntityTwin twin, CotConfigDTO config) {
    String affiliation = resolveAffiliationCode(twin, config);
    VehicleClass vehicleClass = resolveVehicleClass(twin);
    String classification =
        switch (vehicleClass) {
          case UAV -> "A-M-F-U";
          case USV -> "S-X-M";
          case UGV -> "G-E-V";
          case UUV -> "U-X-M";
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
      case "HOSTILE", "ENEMY" -> "h";
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

  private void applyPointDefaults(TakPoint point, EntityTwin twin, CotConfigDTO config) {
    if (point == null) {
      return;
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

  private String valueOrDefault(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value;
  }
}
