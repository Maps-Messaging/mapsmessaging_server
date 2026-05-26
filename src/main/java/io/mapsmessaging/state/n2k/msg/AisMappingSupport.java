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

package io.mapsmessaging.state.n2k.msg;

import io.mapsmessaging.state.drone.drone.DroneTwin;

import java.time.Instant;
import java.util.Locale;

public final class AisMappingSupport {

  private AisMappingSupport() {
  }

  public static boolean hasCorePosition(DroneTwin droneTwin) {
    return droneTwin != null
        && droneTwin.getMmsi() != null
        && droneTwin.getGeoPosition() != null
        && droneTwin.getGeoPosition().getLatitude() != null
        && droneTwin.getGeoPosition().getLongitude() != null;
  }

  public static Long toSecondOfMinute(Instant instant) {
    if (instant == null) {
      return null;
    }
    return (long) Math.floorMod(instant.getEpochSecond(), 60);
  }

  public static Double toRadians(Double degrees) {
    if (degrees == null) {
      return null;
    }
    return Math.toRadians(normalizeDegrees(degrees));
  }

  public static double normalizeDegrees(double degrees) {
    double normalized = degrees % 360.0d;
    if (normalized < 0.0d) {
      normalized += 360.0d;
    }
    return normalized;
  }

  public static Long deriveSequenceId(DroneTwin droneTwin, Long configuredSequenceId) {
    if (configuredSequenceId != null) {
      return configuredSequenceId;
    }
    if (droneTwin == null || droneTwin.getTwinId() == null || droneTwin.getTwinId().isBlank()) {
      return 0L;
    }
    return (long) Math.floorMod(droneTwin.getTwinId().hashCode(), 253);
  }

  public static String resolveName(DroneTwin droneTwin, String configuredName) {
    if (configuredName != null && !configuredName.isBlank()) {
      return truncate(configuredName, 20);
    }
    if (droneTwin == null) {
      return null;
    }
    if (droneTwin.getDisplayName() != null && !droneTwin.getDisplayName().isBlank()) {
      return truncate(droneTwin.getDisplayName(), 20);
    }
    if (droneTwin.getRegistrationId() != null && !droneTwin.getRegistrationId().isBlank()) {
      return truncate(droneTwin.getRegistrationId(), 20);
    }
    if (droneTwin.getTwinId() != null && !droneTwin.getTwinId().isBlank()) {
      return truncate(droneTwin.getTwinId(), 20);
    }
    return null;
  }

  public static String resolveVendorId(String configuredVendorId) {
    if (configuredVendorId == null || configuredVendorId.isBlank()) {
      return null;
    }
    return truncate(configuredVendorId.toUpperCase(Locale.ROOT), 7);
  }

  public static String resolveCallsign(DroneTwin droneTwin, String configuredCallsign) {

    if (droneTwin == null) {
      if (configuredCallsign != null && !configuredCallsign.isBlank()) {
        return truncate(configuredCallsign.toUpperCase(Locale.ROOT), 7);
      }
      return null;
    }
    if (droneTwin.getCallSign() != null && !droneTwin.getCallSign().isBlank()) {
      return truncate(droneTwin.getCallSign(), 7);
    }
    return null;
  }

  public static String truncate(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    value = value.replaceAll("[^A-Za-z0-9 ]", " ")
        .trim()
        .replaceAll(" +", " ");
    if (value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}