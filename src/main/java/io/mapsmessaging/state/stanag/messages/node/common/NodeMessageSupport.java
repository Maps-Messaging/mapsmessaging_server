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

package io.mapsmessaging.state.stanag.messages.node.common;

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeMessageSupport {

  private static final String POSITION_TYPE_LATITUDE_LONGITUDE_ALTITUDE = "PositionTypeEnum_LATITUDE_LONGITUDE_ALTITUDE";
  private static final String ALTITUDE_TYPE_WGS = "AltitudeTypeEnum_WGS";
  private static final String ORIENTATION_TYPE_EULER_ANGLES = "OrientationTypeEnum_EULER_ANGLES";
  private static final String VELOCITY_TYPE_SPEED_COURSE_CLIMB_RATE = "VelocityTypeEnum_SPEED_COURSE_CLIMB_RATE";

  public Map<String, Object> buildDescription(DroneTwin droneTwin) {
    Map<String, Object> description = new HashMap<>();

    if (droneTwin.getDescription() != null) {
      description.putAll(droneTwin.getDescription());
    }

    if (droneTwin.getCallSign() != null) {
      description.put("name", droneTwin.getCallSign());
    }

    return description;
  }

  public Pose buildPose(DroneTwin droneTwin) {
    return Pose.builder()
        .position(buildPosition(droneTwin.getGeoPosition()))
        .orientation(buildOrientation(droneTwin.getOrientation()))
        .build();
  }

  public Velocity buildVelocity(DroneTwin droneTwin) {
    return Velocity.builder()
        .discriminator(VELOCITY_TYPE_SPEED_COURSE_CLIMB_RATE)
        .speedCourseClimbRate(buildSpeedCourseClimbRate(droneTwin))
        .build();
  }

  private Position buildPosition(GeoPosition geoPosition) {
    return Position.builder()
        .discriminator(POSITION_TYPE_LATITUDE_LONGITUDE_ALTITUDE)
        .latitudeLongitudeAltitude(buildLatitudeLongitudeAltitude(geoPosition))
        .build();
  }

  private LatitudeLongitudeAltitude buildLatitudeLongitudeAltitude(GeoPosition geoPosition) {
    if (geoPosition == null) {
      return LatitudeLongitudeAltitude.builder().build();
    }

    return LatitudeLongitudeAltitude.builder()
        .latitude(geoPosition.getLatitude())
        .longitude(geoPosition.getLongitude())
        .altitude(buildAltitude(geoPosition))
        .build();
  }

  private List<Altitude> buildAltitude(GeoPosition geoPosition) {
    if (geoPosition.getAltitudeMslMeters() == null) {
      return null;
    }

    Altitude altitude = Altitude.builder()
        .value(geoPosition.getAltitudeMslMeters())
        .type(ALTITUDE_TYPE_WGS)
        .build();

    return List.of(altitude);
  }

  private Orientation buildOrientation(io.mapsmessaging.state.drone.model.Orientation orientation) {
    return Orientation.builder()
        .discriminator(ORIENTATION_TYPE_EULER_ANGLES)
        .eulerAngles(buildEulerAngles(orientation))
        .build();
  }

  private EulerAngles buildEulerAngles(io.mapsmessaging.state.drone.model.Orientation orientation) {
    if (orientation == null) {
      return EulerAngles.builder().build();
    }

    return EulerAngles.builder()
        .roll(orientation.getRollDegrees())
        .pitch(orientation.getPitchDegrees())
        .yaw(orientation.getYawDegrees())
        .build();
  }

  private SpeedCourseClimbRate buildSpeedCourseClimbRate(DroneTwin droneTwin) {
    return SpeedCourseClimbRate.builder()
        .speed(droneTwin.getGroundSpeedMetersPerSecond())
        .course(droneTwin.getHeadingDegrees())
        .climbRate(normaliseZero(droneTwin.getVerticalSpeedMetersPerSecond()))
        .build();
  }

  private Double normaliseZero(Double value) {
    if (value == null) {
      return null;
    }

    if (Double.compare(value, -0.0d) == 0 || Math.abs(value) < 0.0000001d) {
      return 0.0d;
    }

    return value;
  }

  public Pose buildPose(GeoPosition geoPosition) {
    Position position = buildPosition(geoPosition);

    if (position == null) {
      return null;
    }

    return Pose.builder()
        .position(position)
        .build();
  }
}