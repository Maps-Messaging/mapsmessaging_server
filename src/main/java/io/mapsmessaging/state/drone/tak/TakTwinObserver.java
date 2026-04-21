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
 *
 */

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.config.TwinManagerConfig;
import io.mapsmessaging.dto.rest.config.TwinManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.VehicleClass;
import io.mapsmessaging.state.drone.core.*;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.BatteryState;
import io.mapsmessaging.state.drone.model.FixInfo;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.model.LinkState;
import io.mapsmessaging.state.drone.model.Orientation;
import io.mapsmessaging.state.drone.model.VelocityVector;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Twin observer that maps twin state into TAK CoT XML and publishes it to a topic.
 */
public class TakTwinObserver implements TwinObserver {

  private static final String DEFAULT_HOW = "h-g-i-g-o";
  private static final String DEFAULT_ALTITUDE_SOURCE = "GPS";
  private static final String MAPS_OS = "MapsMessaging";
  private static final String MAPS_VERSION = "1.0";
  private static final double DEFAULT_CE = 10.0;
  private static final double DEFAULT_LE = 15.0;
  private static final long DEFAULT_STALE_SECONDS = 30L;
  private static final long PUBLISH_INTERVAL_MS = 1000L;

  private final Map<String, TakTwinContext> takContexts;
  private final String takHost;
  private final int takPort;
  private final TwinManager twinManager;

  public TakTwinObserver(TwinManager twinManager) {
    this.twinManager = Objects.requireNonNull(twinManager, "twinManager cannot be null");
    TwinManagerConfigDTO config = ConfigurationManager.getInstance().getConfiguration(TwinManagerConfig.class);
    this.takContexts = new ConcurrentHashMap<>();
    if(config != null && config.getTak() != null) {
      this.takHost = config.getTak().getHostname();
      this.takPort = config.getTak().getPort();
      twinManager.addObserver(this);
    }
    else{
      this.takHost = null;
      this.takPort = 0;
    }
  }

  public void shutdown(){
    twinManager.removeObserver(this);
    for(TakTwinContext context : takContexts.values()){
      if(context.getSocketConnection() != null){
        context.getSocketConnection().close();
      }
    }
    takContexts.clear();
  }

  @Override
  public void onTwinAdded(EntityTwin twin, TwinUpdateContext context) {
    TakTwinContext context1 = takContexts.computeIfAbsent(twin.getTwinId(), key -> new TakTwinContext());
    publishTwin(twin, context, context1);
  }

  @Override
  public void onTwinUpdated(String twinId, EntityTwin previous, EntityTwin current, TwinUpdateContext context) {
    if (current == null) {
      return;
    }

    String resolvedTwinId = twinId;
    if (resolvedTwinId == null || resolvedTwinId.isBlank()) {
      resolvedTwinId = current.getTwinId();
    }

    if (resolvedTwinId != null && !resolvedTwinId.isBlank()) {
      long now = System.currentTimeMillis();
      TakTwinContext context1 = takContexts.computeIfAbsent(resolvedTwinId, key -> new TakTwinContext());
      if (context1.getLastUpdate() + PUBLISH_INTERVAL_MS > now) {
        return;
      }
      context1.setLastUpdate(now);
      publishTwin(current, context, context1);
    }
  }

  @Override
  public void onTwinRemoved(EntityTwin removed, TwinUpdateContext context) {
    TakTwinContext context1 = takContexts.get(removed.getTwinId());
    if(context1 != null){
      publishRemoval(removed, context, context1);
    }
    takContexts.remove(removed.getTwinId());
    if(context1 != null && context1.getSocketConnection() != null){
      context1.getSocketConnection().close();
    }
  }

  @Override
  public void onRelationshipUpdated(String twinId, TwinRelationship relationship, TwinUpdateContext context) {
    // ignored for now
  }

  @Override
  public void onTwinStatusChanged(String twinId,
                                  TwinLifecycleStatus previousStatus,
                                  TwinLifecycleStatus currentStatus,
                                  EntityTwin twin,
                                  TwinUpdateContext context) {
    TakTwinContext context1 = takContexts.computeIfAbsent(twinId, key -> new TakTwinContext());
    publishTwin(twin, context, context1);
  }

  private void publishTwin(EntityTwin twin, TwinUpdateContext context, TakTwinContext twinContext) {
    if (twin == null) {
      return;
    }

    GeoPosition geoPosition = twin.getGeoPosition();
    if (geoPosition == null) {
      return;
    }

    String xml = buildTakEvent(twin, context);
    if(twinContext.getSocketConnection() == null){
      twinContext.setSocketConnection(new TakSocketConnection(takHost, takPort));
    }
    twinContext.getSocketConnection().accept(xml);
  }

  private void publishRemoval(EntityTwin twin, TwinUpdateContext context, TakTwinContext twinContext) {
    if (twin == null ||twinContext.getSocketConnection() == null ) {
      return;
    }

    Instant eventTime = resolveEventTime(twin, context);
    Instant staleTime = eventTime.plus(1, ChronoUnit.SECONDS);

    StringBuilder stringBuilder = new StringBuilder(768);
    stringBuilder.append("<event version=\"2.0\"");
    stringBuilder.append(" uid=\"").append(escapeXml(resolveUid(twin))).append("\"");
    stringBuilder.append(" type=\"").append(escapeXml(resolveCotType(twin))).append("\"");
    stringBuilder.append(" how=\"").append(DEFAULT_HOW).append("\"");
    stringBuilder.append(" time=\"").append(formatInstant(eventTime)).append("\"");
    stringBuilder.append(" start=\"").append(formatInstant(eventTime)).append("\"");
    stringBuilder.append(" stale=\"").append(formatInstant(staleTime)).append("\">");
    stringBuilder.append("<detail>");
    appendContact(twin, stringBuilder);
    appendRemarks(twin, stringBuilder);
    appendPrecisionLocation(stringBuilder);
    stringBuilder.append("<status lifecycle=\"REMOVED\" reason=\"removed\"/>");
    stringBuilder.append("</detail>");
    stringBuilder.append("</event>");
    twinContext.getSocketConnection().accept(stringBuilder.toString());
  }

  private String buildTakEvent(EntityTwin twin, TwinUpdateContext context) {
    Instant eventTime = resolveEventTime(twin, context);
    Instant staleTime = eventTime.plus(resolveStaleSeconds(twin), ChronoUnit.SECONDS);
    GeoPosition geoPosition = twin.getGeoPosition();
    FixInfo fixInfo = twin.getFixInfo();
    VelocityVector velocityVector = twin.getVelocityVector();
    Orientation orientation = twin.getOrientation();

    double latitude = readLatitude(geoPosition);
    double longitude = readLongitude(geoPosition);
    double heightAboveEllipsoid = readAltitude(geoPosition);
    double circularError = resolveCircularError(fixInfo);
    double linearError = resolveLinearError(fixInfo);
    double speed = resolveSpeedMetersPerSecond(twin, velocityVector);
    double course = resolveCourseDegrees(twin, orientation, velocityVector);

    StringBuilder stringBuilder = new StringBuilder(2048);
    stringBuilder.append("<event version=\"2.0\"");
    stringBuilder.append(" uid=\"").append(escapeXml(resolveUid(twin))).append("\"");
    stringBuilder.append(" type=\"").append(escapeXml(resolveCotType(twin))).append("\"");
    stringBuilder.append(" how=\"").append(DEFAULT_HOW).append("\"");
    stringBuilder.append(" time=\"").append(formatInstant(eventTime)).append("\"");
    stringBuilder.append(" start=\"").append(formatInstant(eventTime)).append("\"");
    stringBuilder.append(" stale=\"").append(formatInstant(staleTime)).append("\">");

    stringBuilder.append("<point");
    stringBuilder.append(" lat=\"").append(latitude).append("\"");
    stringBuilder.append(" lon=\"").append(longitude).append("\"");
    stringBuilder.append(" hae=\"").append(heightAboveEllipsoid).append("\"");
    stringBuilder.append(" ce=\"").append(circularError).append("\"");
    stringBuilder.append(" le=\"").append(linearError).append("\"");
    stringBuilder.append("/>");

    stringBuilder.append("<detail>");
    appendContact(twin, stringBuilder);
    appendTrack(speed, course, stringBuilder);
    appendStatus(twin, stringBuilder);
    appendRemarks(twin, stringBuilder);
    appendPrecisionLocation(stringBuilder);
    appendTakPlatform(twin, stringBuilder);
    //appendDroneDetails(twin, stringBuilder);
    //appendBatteryState(twin.getBatteryState(), stringBuilder);
    appendLinkState(twin.getLinkState(), stringBuilder);
    appendLinks(twin, stringBuilder);
    stringBuilder.append("</detail>");
    stringBuilder.append("</event>");
    return stringBuilder.toString();
  }

  private void appendContact(EntityTwin twin, StringBuilder stringBuilder) {
    String callsign = resolveCallSign(twin);
    if (callsign == null || callsign.isBlank()) {
      return;
    }

    stringBuilder.append("<contact callsign=\"").append(escapeXml(callsign)).append("\"/>");
  }

  private void appendTrack(double speed, double course, StringBuilder stringBuilder) {
    stringBuilder.append("<track");
    stringBuilder.append(" speed=\"").append(speed).append("\"");
    stringBuilder.append(" course=\"").append(course).append("\"");
    stringBuilder.append("/>");
  }

  private void appendStatus(EntityTwin twin, StringBuilder stringBuilder) {
    String lifecycle = twin.getLifecycleStatus() != null ? twin.getLifecycleStatus().name() : "UNKNOWN";

    stringBuilder.append("<status");
    stringBuilder.append(" lifecycle=\"").append(escapeXml(lifecycle)).append("\"");
    stringBuilder.append(" reason=\"updated\"");
    stringBuilder.append("/>");
  }

  private void appendRemarks(EntityTwin twin, StringBuilder stringBuilder) {
    String remarks = resolveRemarks(twin);
    if (remarks == null || remarks.isBlank()) {
      return;
    }

    stringBuilder.append("<remarks>");
    stringBuilder.append(escapeXml(remarks));
    stringBuilder.append("</remarks>");
  }

  private void appendPrecisionLocation(StringBuilder stringBuilder) {
    stringBuilder.append("<precisionlocation altsrc=\"").append(DEFAULT_ALTITUDE_SOURCE).append("\"/>");
  }

  private void appendTakPlatform(EntityTwin twin, StringBuilder stringBuilder) {
    stringBuilder.append("<takv");
    stringBuilder.append(" device=\"").append(escapeXml(safeString(twin.getTwinType(), "twin"))).append("\"");
    stringBuilder.append(" platform=\"").append(escapeXml(resolvePlatform(twin))).append("\"");
    stringBuilder.append(" os=\"").append(escapeXml(MAPS_OS)).append("\"");
    stringBuilder.append(" version=\"").append(escapeXml(MAPS_VERSION)).append("\"");
    stringBuilder.append("/>");
  }

  private void appendDroneDetails(EntityTwin twin, StringBuilder stringBuilder) {
    if (!(twin instanceof DroneTwin droneTwin)) {
      return;
    }

    stringBuilder.append("<maps-drone");
    appendAttribute(stringBuilder, "systemId", droneTwin.getSystemId());
    appendAttribute(stringBuilder, "componentId", droneTwin.getComponentId());
    appendAttribute(stringBuilder, "vehicleClass", droneTwin.getVehicleClass());
    appendAttribute(stringBuilder, "registrationId", droneTwin.getRegistrationId());
    appendAttribute(stringBuilder, "armed", droneTwin.getArmed());
    appendAttribute(stringBuilder, "flightMode", resolveFlightMode(droneTwin));
    appendAttribute(stringBuilder, "failsafe", droneTwin.getFailsafe());
    appendAttribute(stringBuilder, "gpsValid", droneTwin.getGpsValid());
    appendAttribute(stringBuilder, "missionState", droneTwin.getMissionState());
    appendAttribute(stringBuilder, "landedState", droneTwin.getLandedState());
    appendAttribute(stringBuilder, "vtolState", droneTwin.getVtolState());
    appendAttribute(stringBuilder, "missionSequence", droneTwin.getCurrentMissionSequence());
    appendAttribute(stringBuilder, "controllingStationId", droneTwin.getControllingStationId());
    stringBuilder.append("/>");
  }

  private void appendBatteryState(BatteryState batteryState, StringBuilder stringBuilder) {
    if (batteryState == null) {
      return;
    }

    stringBuilder.append("<maps-power");
    appendAttribute(stringBuilder, "percentage", batteryState.getPercentage());
    appendAttribute(stringBuilder, "voltageVolts", batteryState.getVoltageVolts());
    appendAttribute(stringBuilder, "currentAmps", batteryState.getCurrentAmps());
    appendAttribute(stringBuilder, "remainingMilliampHours", batteryState.getRemainingMilliampHours());
    appendAttribute(stringBuilder, "temperatureCelsius", batteryState.getTemperatureCelsius());
    appendAttribute(stringBuilder, "charging", batteryState.getCharging());
    stringBuilder.append("/>");
  }

  private void appendLinkState(LinkState linkState, StringBuilder stringBuilder) {
    if (linkState == null) {
      return;
    }

    stringBuilder.append("<maps-link");
    appendAttribute(stringBuilder, "state", linkState.getState());
    appendAttribute(stringBuilder, "connected", linkState.getConnected());
    appendAttribute(stringBuilder, "rssiDbm", linkState.getRssiDbm());
    appendAttribute(stringBuilder, "snrDb", linkState.getSnrDb());
    appendAttribute(stringBuilder, "latencyMs", linkState.getLatencyMs());
    appendAttribute(stringBuilder, "rxErrorRate", linkState.getRxErrorRate());
    appendAttribute(stringBuilder, "txErrorRate", linkState.getTxErrorRate());
    stringBuilder.append("/>");
  }

  private void appendLinks(EntityTwin twin, StringBuilder stringBuilder) {
    if (twin.getRelationships() == null || twin.getRelationships().isEmpty()) {
      return;
    }

    for (TwinRelationship relationship : twin.getRelationships()) {
      if (relationship == null) {
        continue;
      }

      stringBuilder.append("<link");
      stringBuilder.append(" uid=\"").append(escapeXml(sanitiseIdentifier(safeString(relationship.getTargetTwinId(), "unknown")))).append("\"");
      stringBuilder.append(" relation=\"").append(escapeXml(safeString(relationship.getRelationshipType(), "related"))).append("\"");
      stringBuilder.append("/>");
    }
  }

  private void appendAttribute(StringBuilder stringBuilder, String name, Object value) {
    if (value == null) {
      return;
    }

    String stringValue = String.valueOf(value);
    if (stringValue.isBlank()) {
      return;
    }

    stringBuilder.append(' ');
    stringBuilder.append(name);
    stringBuilder.append("=\"");
    stringBuilder.append(escapeXml(stringValue));
    stringBuilder.append('"');
  }

  private String resolveUid(EntityTwin twin) {
    String rawValue;
    if ( twin.getTwinId() != null && !twin.getTwinId().isBlank()) {
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
        };
      }
    }

    String twinType = twin.getTwinType();
    if ("DRONE".equalsIgnoreCase(twinType)) {
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

    if (remarksBuilder.length() == 0 && twin.getDisplayName() != null && !twin.getDisplayName().isBlank()) {
      remarksBuilder.append(twin.getDisplayName());
    }

    return remarksBuilder.length() == 0 ? null : remarksBuilder.toString();
  }

  private void appendRemarkText(StringBuilder remarksBuilder, String value) {
    if (value == null || value.isBlank()) {
      return;
    }

    if (remarksBuilder.length() > 0) {
      remarksBuilder.append(" | ");
    }

    remarksBuilder.append(value);
  }

  private void appendLabelledRemark(StringBuilder remarksBuilder, String label, String value) {
    if (value == null || value.isBlank()) {
      return;
    }

    if (remarksBuilder.length() > 0) {
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

    return resolveEventTime(context);
  }

  private Instant resolveEventTime(TwinUpdateContext context) {
    if (context != null) {
      try {
        Instant timestamp = context.getEventTime();
        if (timestamp != null) {
          return timestamp;
        }
      }
      catch (Exception ignored) {
        // ignore
      }
    }

    return Instant.now();
  }

  private long resolveStaleSeconds(EntityTwin twin) {
    return DEFAULT_STALE_SECONDS;
  }

  private double resolveCircularError(FixInfo fixInfo) {
    if (fixInfo == null) {
      return DEFAULT_CE;
    }

    try {
      Double horizontalAccuracyMeters = fixInfo.getHorizontalAccuracyMeters();
      if (horizontalAccuracyMeters != null && horizontalAccuracyMeters > 0.0) {
        return horizontalAccuracyMeters;
      }
    }
    catch (Exception ignored) {
      // ignore
    }

    return DEFAULT_CE;
  }

  private double resolveLinearError(FixInfo fixInfo) {
    if (fixInfo == null) {
      return DEFAULT_LE;
    }

    try {
      Double verticalAccuracyMeters = fixInfo.getVerticalAccuracyMeters();
      if (verticalAccuracyMeters != null && verticalAccuracyMeters > 0.0) {
        return verticalAccuracyMeters;
      }
    }
    catch (Exception ignored) {
      // ignore
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

    if (north == null && east == null) {
      return 0.0;
    }

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

      if (north != null || east != null) {
        double northValue = north != null ? north : 0.0;
        double eastValue = east != null ? east : 0.0;

        if (northValue != 0.0 || eastValue != 0.0) {
          double radians = Math.atan2(eastValue, northValue);
          return normaliseDegrees(Math.toDegrees(radians));
        }
      }
    }

    if (twin instanceof DroneTwin droneTwin && droneTwin.getHeadingDegrees() != null) {
      return normaliseDegrees(droneTwin.getHeadingDegrees());
    }

    if (orientation == null) {
      return 0.0;
    }

    try {
      Double yawDegrees = orientation.getYawDegrees();
      if (yawDegrees != null) {
        return normaliseDegrees(yawDegrees);
      }
    }
    catch (Exception ignored) {
      // ignore
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
    try {
      Double latitude = geoPosition.getLatitude();
      return latitude != null ? latitude : 0.0;
    }
    catch (Exception ignored) {
      return 0.0;
    }
  }

  private double readLongitude(GeoPosition geoPosition) {
    try {
      Double longitude = geoPosition.getLongitude();
      return longitude != null ? longitude : 0.0;
    }
    catch (Exception ignored) {
      return 0.0;
    }
  }

  private double readAltitude(GeoPosition geoPosition) {
    try {
      Double altitudeMeters = geoPosition.getAltitudeMslMeters();
      return altitudeMeters != null ? altitudeMeters : 0.0;
    }
    catch (Exception ignored) {
      return 0.0;
    }
  }

  private String formatInstant(Instant instant) {
    return instant
        .truncatedTo(ChronoUnit.MILLIS)
        .toString();
  }

  private String safeString(Object value, String defaultValue) {
    if (value == null) {
      return defaultValue;
    }

    String stringValue = String.valueOf(value);
    if (stringValue.isBlank()) {
      return defaultValue;
    }

    return stringValue;
  }

  private String escapeXml(String value) {
    if (value == null) {
      return "";
    }

    return value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }
}