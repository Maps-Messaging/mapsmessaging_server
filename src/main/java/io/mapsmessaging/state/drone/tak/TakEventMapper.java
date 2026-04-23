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

import io.mapsmessaging.dto.rest.config.protocol.impl.VehicleClass;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinRelationship;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.FixInfo;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.model.LinkState;
import io.mapsmessaging.state.drone.model.Orientation;
import io.mapsmessaging.state.drone.model.VelocityVector;
import io.mapsmessaging.state.drone.tak.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class TakEventMapper {

  private static final String DEFAULT_HOW = "h-g-i-g-o";
  private static final String DEFAULT_ALTITUDE_SOURCE = "GPS";
  private static final String MAPS_OS = "MapsMessaging";
  private static final String MAPS_VERSION = "1.0";
  private static final double DEFAULT_CE = 10.0;
  private static final double DEFAULT_LE = 15.0;
  private static final long DEFAULT_STALE_SECONDS = 30L;

  public TakEvent map(EntityTwin twin, TwinUpdateContext context) {
    if (twin == null || twin.getGeoPosition() == null) {
      return null;
    }

    Instant eventTime = resolveEventTime(twin, context);
    Instant staleTime = eventTime.plus(resolveStaleSeconds(twin), ChronoUnit.SECONDS);

    GeoPosition geoPosition = twin.getGeoPosition();
    FixInfo fixInfo = twin.getFixInfo();
    VelocityVector velocityVector = twin.getVelocityVector();
    Orientation orientation = twin.getOrientation();

    TakEvent event = new TakEvent();
    event.setUid(resolveUid(twin));
    event.setType(resolveCotType(twin));
    event.setHow(DEFAULT_HOW);
    event.setTime(formatInstant(eventTime));
    event.setStart(formatInstant(eventTime));
    event.setStale(formatInstant(staleTime));

    TakPoint point = new TakPoint();
    point.setLat(readLatitude(geoPosition));
    point.setLon(readLongitude(geoPosition));
    point.setHae(readAltitude(geoPosition));
    point.setCe(resolveCircularError(fixInfo));
    point.setLe(resolveLinearError(fixInfo));
    event.setPoint(point);

    TakDetail detail = new TakDetail();
    detail.setContact(buildContact(twin));
    detail.setTrack(buildTrack(twin, velocityVector, orientation));
    detail.setStatus(buildStatus(twin, "updated"));
    detail.setRemarks(resolveRemarks(twin));
    detail.setPrecisionLocation(buildPrecisionLocation());
    detail.setTakv(buildPlatform(twin));
    detail.setMapsLink(buildLinkState(twin.getLinkState()));

    if (twin.getRelationships() != null) {
      for (TwinRelationship relationship : twin.getRelationships()) {
        TakLink takLink = buildLink(relationship);
        if (takLink != null) {
          detail.getLinks().add(takLink);
        }
      }
    }

    event.setDetail(detail);
    return event;
  }

  public TakEvent mapRemoval(EntityTwin twin, TwinUpdateContext context) {
    if (twin == null) {
      return null;
    }

    Instant eventTime = resolveEventTime(twin, context);
    Instant staleTime = eventTime.plus(1, ChronoUnit.SECONDS);

    TakEvent event = new TakEvent();
    event.setUid(resolveUid(twin));
    event.setType(resolveCotType(twin));
    event.setHow(DEFAULT_HOW);
    event.setTime(formatInstant(eventTime));
    event.setStart(formatInstant(eventTime));
    event.setStale(formatInstant(staleTime));

    TakDetail detail = new TakDetail();
    detail.setContact(buildContact(twin));
    detail.setRemarks(resolveRemarks(twin));
    detail.setPrecisionLocation(buildPrecisionLocation());
    detail.setStatus(buildStatus(twin, "removed"));
    event.setDetail(detail);

    return event;
  }

  private TakContact buildContact(EntityTwin twin) {
    String callsign = resolveCallSign(twin);
    if (callsign == null || callsign.isBlank()) {
      return null;
    }

    TakContact contact = new TakContact();
    contact.setCallsign(callsign);
    return contact;
  }

  private TakTrack buildTrack(EntityTwin twin, VelocityVector velocityVector, Orientation orientation) {
    TakTrack track = new TakTrack();
    track.setSpeed(resolveSpeedMetersPerSecond(twin, velocityVector));
    track.setCourse(resolveCourseDegrees(twin, orientation, velocityVector));
    return track;
  }

  private TakStatus buildStatus(EntityTwin twin, String reason) {
    TakStatus status = new TakStatus();
    status.setLifecycle(twin.getLifecycleStatus() != null ? twin.getLifecycleStatus().name() : "UNKNOWN");
    status.setReason(reason);
    return status;
  }

  private TakPrecisionLocation buildPrecisionLocation() {
    TakPrecisionLocation precisionLocation = new TakPrecisionLocation();
    precisionLocation.setAltsrc(DEFAULT_ALTITUDE_SOURCE);
    return precisionLocation;
  }

  private TakPlatform buildPlatform(EntityTwin twin) {
    TakPlatform takPlatform = new TakPlatform();
    takPlatform.setDevice(safeString(twin.getTwinType(), "twin"));
    takPlatform.setPlatform(resolvePlatform(twin));
    takPlatform.setOs(MAPS_OS);
    takPlatform.setVersion(MAPS_VERSION);
    return takPlatform;
  }

  private TakLinkState buildLinkState(LinkState linkState) {
    if (linkState == null) {
      return null;
    }

    TakLinkState takLinkState = new TakLinkState();
    takLinkState.setState(linkState.getState());
    takLinkState.setConnected(linkState.getConnected());
    takLinkState.setRssiDbm(linkState.getRssiDbm());
    takLinkState.setSnrDb(linkState.getSnrDb());
    takLinkState.setLatencyMs(linkState.getLatencyMs());
    takLinkState.setRxErrorRate(linkState.getRxErrorRate());
    takLinkState.setTxErrorRate(linkState.getTxErrorRate());
    return takLinkState;
  }

  private TakLink buildLink(TwinRelationship relationship) {
    if (relationship == null) {
      return null;
    }

    TakLink link = new TakLink();
    link.setUid(sanitiseIdentifier(safeString(relationship.getTargetTwinId(), "unknown")));
    link.setRelation(safeString(relationship.getRelationshipType(), "related"));
    return link;
  }

  private String resolveUid(EntityTwin twin) {
    String rawValue;
    if (twin.getTwinId() != null && !twin.getTwinId().isBlank()) {
      rawValue = twin.getTwinId();
    }
    else if (twin.getDisplayName() != null && !twin.getDisplayName().isBlank()) {
      rawValue = twin.getDisplayName();
    }
    else {
      rawValue = "unknown-twin";
    }
    return sanitiseIdentifier(rawValue);
  }

  private String sanitiseIdentifier(String value) {
    if (value == null || value.isBlank()) {
      return "unknown";
    }

    String sanitised = value.trim().replaceAll("[^A-Za-z0-9._-]+", "-");
    sanitised = sanitised.replaceAll("-{2,}", "-");
    sanitised = sanitised.replaceAll("^-+", "");
    sanitised = sanitised.replaceAll("-+$", "");
    return sanitised.isBlank() ? "unknown" : sanitised;
  }

  private String resolveCotType(EntityTwin twin) {
    if (twin instanceof DroneTwin droneTwin) {
      VehicleClass vehicleClass = droneTwin.getVehicleClass();
      if (vehicleClass != null) {
        return switch (vehicleClass) {
          case UAV -> "a-f-A-M-F-U";
          case USV -> "a-f-S-X-M";
          case UGV -> "a-f-G-E-V";
          case UUV -> "a-f-U-X-M";
          case GCS -> "a-f-G-U-C";
          case UNKNOWN -> null;
        };
      }
    }

    TwinType twinType = twin.getTwinType();
    if (TwinType.DRONE.equals(twinType)) {
      return "a-f-A-M-F-U";
    }
    return "a-f-G-U-C";
  }

  private String resolvePlatform(EntityTwin twin) {
    if (twin instanceof DroneTwin droneTwin && droneTwin.getVehicleClass() != null) {
      return droneTwin.getVehicleClass().name();
    }
    return safeString(twin.getTwinType(), "UNKNOWN");
  }

  private String resolveCallSign(EntityTwin twin) {
    if (twin instanceof DroneTwin droneTwin) {
      if (droneTwin.getCallSign() != null && !droneTwin.getCallSign().isBlank()) {
        return droneTwin.getCallSign();
      }
      if (droneTwin.getRegistrationId() != null && !droneTwin.getRegistrationId().isBlank()) {
        return droneTwin.getRegistrationId();
      }
    }

    if (twin.getDisplayName() != null && !twin.getDisplayName().isBlank()) {
      return twin.getDisplayName();
    }

    return twin.getTwinId();
  }

  private String resolveRemarks(EntityTwin twin) {
    StringBuilder remarksBuilder = new StringBuilder();

    if (twin instanceof DroneTwin droneTwin) {
      appendRemarkText(remarksBuilder, droneTwin.getDescription());
      appendLabelledRemark(remarksBuilder, "mode", resolveFlightMode(droneTwin));
      appendLabelledRemark(remarksBuilder, "mission", droneTwin.getMissionState());
      appendLabelledRemark(remarksBuilder, "landed", droneTwin.getLandedState());
      appendLabelledRemark(remarksBuilder, "vtol", droneTwin.getVtolState());
    }

    if (remarksBuilder.isEmpty() && twin.getDisplayName() != null && !twin.getDisplayName().isBlank()) {
      remarksBuilder.append(twin.getDisplayName());
    }

    return remarksBuilder.isEmpty() ? null : remarksBuilder.toString();
  }

  private void appendRemarkText(StringBuilder remarksBuilder, String value) {
    if (value == null || value.isBlank()) {
      return;
    }

    if (!remarksBuilder.isEmpty()) {
      remarksBuilder.append(" | ");
    }
    remarksBuilder.append(value);
  }

  private void appendLabelledRemark(StringBuilder remarksBuilder, String label, String value) {
    if (value == null || value.isBlank()) {
      return;
    }

    if (!remarksBuilder.isEmpty()) {
      remarksBuilder.append(" | ");
    }
    remarksBuilder.append(label).append('=').append(value);
  }

  private String resolveFlightMode(DroneTwin droneTwin) {
    if (droneTwin == null) {
      return null;
    }

    String flightMode = droneTwin.getFlightMode();
    if (flightMode == null || flightMode.isBlank()) {
      return null;
    }

    if (flightMode.matches("\\d+")) {
      return null;
    }

    return flightMode;
  }

  private Instant resolveEventTime(EntityTwin twin, TwinUpdateContext context) {
    if (twin != null && twin.getLastSeenAt() != null) {
      return twin.getLastSeenAt();
    }

    if (twin instanceof DroneTwin droneTwin && droneTwin.getOperationalUpdatedAt() != null) {
      return droneTwin.getOperationalUpdatedAt();
    }

    if (context != null && context.getEventTime() != null) {
      return context.getEventTime();
    }

    return Instant.now();
  }

  private long resolveStaleSeconds(EntityTwin twin) {
    return DEFAULT_STALE_SECONDS;
  }

  private double resolveCircularError(FixInfo fixInfo) {
    if (fixInfo != null && fixInfo.getHorizontalAccuracyMeters() != null && fixInfo.getHorizontalAccuracyMeters() > 0.0) {
      return fixInfo.getHorizontalAccuracyMeters();
    }
    return DEFAULT_CE;
  }

  private double resolveLinearError(FixInfo fixInfo) {
    if (fixInfo != null && fixInfo.getVerticalAccuracyMeters() != null && fixInfo.getVerticalAccuracyMeters() > 0.0) {
      return fixInfo.getVerticalAccuracyMeters();
    }
    return DEFAULT_LE;
  }

  private double resolveSpeedMetersPerSecond(EntityTwin twin, VelocityVector velocityVector) {
    if (twin instanceof DroneTwin droneTwin && droneTwin.getGroundSpeedMetersPerSecond() != null) {
      return droneTwin.getGroundSpeedMetersPerSecond();
    }

    if (velocityVector == null) {
      return 0.0;
    }

    Double north = velocityVector.getNorthMetersPerSecond();
    Double east = velocityVector.getEastMetersPerSecond();

    double northValue = north != null ? north : 0.0;
    double eastValue = east != null ? east : 0.0;

    return Math.sqrt((northValue * northValue) + (eastValue * eastValue));
  }

  private double resolveCourseDegrees(EntityTwin twin, Orientation orientation, VelocityVector velocityVector) {
    if (twin instanceof DroneTwin droneTwin && droneTwin.getCourseOverGroundDegrees() != null) {
      return normaliseDegrees(droneTwin.getCourseOverGroundDegrees());
    }

    if (velocityVector != null) {
      Double north = velocityVector.getNorthMetersPerSecond();
      Double east = velocityVector.getEastMetersPerSecond();

      double northValue = north != null ? north : 0.0;
      double eastValue = east != null ? east : 0.0;

      if (northValue != 0.0 || eastValue != 0.0) {
        return normaliseDegrees(Math.toDegrees(Math.atan2(eastValue, northValue)));
      }
    }

    if (twin instanceof DroneTwin droneTwin && droneTwin.getHeadingDegrees() != null) {
      return normaliseDegrees(droneTwin.getHeadingDegrees());
    }

    if (orientation != null && orientation.getYawDegrees() != null) {
      return normaliseDegrees(orientation.getYawDegrees());
    }

    return 0.0;
  }

  private double normaliseDegrees(double degrees) {
    double value = degrees % 360.0;
    if (value < 0) {
      value += 360.0;
    }
    return value;
  }

  private double readLatitude(GeoPosition geoPosition) {
    return geoPosition.getLatitude() != null ? geoPosition.getLatitude() : 0.0;
  }

  private double readLongitude(GeoPosition geoPosition) {
    return geoPosition.getLongitude() != null ? geoPosition.getLongitude() : 0.0;
  }

  private double readAltitude(GeoPosition geoPosition) {
    return geoPosition.getAltitudeMslMeters() != null ? geoPosition.getAltitudeMslMeters() : 0.0;
  }

  private String formatInstant(Instant instant) {
    return instant.truncatedTo(ChronoUnit.MILLIS).toString();
  }

  private String safeString(Object value, String defaultValue) {
    if (value == null) {
      return defaultValue;
    }

    String stringValue = String.valueOf(value);
    return stringValue.isBlank() ? defaultValue : stringValue;
  }
}