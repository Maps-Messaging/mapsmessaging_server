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

package io.mapsmessaging.network.protocol.impl.n2k.handler;

import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.network.protocol.impl.n2k.msg.AisMappingSupport;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.drone.DroneTwin;

public abstract class AbstractDronePgnHandler implements DronePgnHandler {

  protected static final double POSITION_THRESHOLD_METERS = 5.0d;
  protected static final double HEADING_THRESHOLD_DEGREES = 5.0d;
  protected static final double COURSE_THRESHOLD_DEGREES = 5.0d;
  protected static final double SPEED_THRESHOLD_METERS_PER_SECOND = 0.5d;

  private final int pgn;
  protected final N2kMessageParser n2kMessageParser;

  protected AbstractDronePgnHandler(int pgn, N2kMessageParser n2kMessageParser) {
    this.pgn = pgn;
    this.n2kMessageParser = n2kMessageParser;
  }

  public abstract String getName();

  @Override
  public int getPgn() {
    return pgn;
  }

  protected boolean isActiveAndPositionValid(DroneTwin droneTwin) {
    return droneTwin != null
        && droneTwin.getLifecycleStatus() == TwinLifecycleStatus.ACTIVE
        && droneTwin.getMmsi() != null
        && droneTwin.getGeoPosition() != null
        && droneTwin.getGeoPosition().getLatitude() != null
        && droneTwin.getGeoPosition().getLongitude() != null
        && Boolean.TRUE.equals(droneTwin.getGpsValid())
        && droneTwin.getNavigationUpdatedAt() != null;
  }

  protected boolean hasMaterialMotionChange(DroneTwin droneTwin, PgnEmissionState emissionState) {
    if (emissionState.getLastLatitude() == null || emissionState.getLastLongitude() == null) {
      return true;
    }

    double movedMeters =
        distanceMeters(
            emissionState.getLastLatitude(),
            emissionState.getLastLongitude(),
            droneTwin.getGeoPosition().getLatitude(),
            droneTwin.getGeoPosition().getLongitude());

    if (movedMeters >= POSITION_THRESHOLD_METERS) {
      return true;
    }

    if (angularDifferenceDegrees(
        emissionState.getLastHeadingDegrees(),
        droneTwin.getHeadingDegrees()) >= HEADING_THRESHOLD_DEGREES) {
      return true;
    }

    if (angularDifferenceDegrees(
        emissionState.getLastCourseOverGroundDegrees(),
        droneTwin.getCourseOverGroundDegrees()) >= COURSE_THRESHOLD_DEGREES) {
      return true;
    }

    return absoluteDifference(
        emissionState.getLastGroundSpeedMetersPerSecond(),
        droneTwin.getGroundSpeedMetersPerSecond()) >= SPEED_THRESHOLD_METERS_PER_SECOND;
  }

  protected void updateMotionSnapshot(DroneTwin droneTwin, PgnEmissionState emissionState) {
    emissionState.setLastLatitude(droneTwin.getGeoPosition().getLatitude());
    emissionState.setLastLongitude(droneTwin.getGeoPosition().getLongitude());
    emissionState.setLastHeadingDegrees(droneTwin.getHeadingDegrees());
    emissionState.setLastCourseOverGroundDegrees(droneTwin.getCourseOverGroundDegrees());
    emissionState.setLastGroundSpeedMetersPerSecond(droneTwin.getGroundSpeedMetersPerSecond());
  }

  protected static double absoluteDifference(Double previousValue, Double currentValue) {
    if (previousValue == null || currentValue == null) {
      return Double.MAX_VALUE;
    }
    return Math.abs(previousValue - currentValue);
  }

  protected static double angularDifferenceDegrees(Double previousDegrees, Double currentDegrees) {
    if (previousDegrees == null || currentDegrees == null) {
      return Double.MAX_VALUE;
    }

    double difference = Math.abs(normalizeDegrees(previousDegrees) - normalizeDegrees(currentDegrees));
    return Math.min(difference, 360.0d - difference);
  }

  protected static double normalizeDegrees(double degrees) {
    return AisMappingSupport.normalizeDegrees(degrees);
  }

  protected static double distanceMeters(
      double latitude1,
      double longitude1,
      double latitude2,
      double longitude2
  ) {
    double latitudeDeltaRadians = Math.toRadians(latitude2 - latitude1);
    double longitudeDeltaRadians = Math.toRadians(longitude2 - longitude1);

    double latitude1Radians = Math.toRadians(latitude1);
    double latitude2Radians = Math.toRadians(latitude2);

    double a =
        Math.sin(latitudeDeltaRadians / 2.0d) * Math.sin(latitudeDeltaRadians / 2.0d)
            + Math.cos(latitude1Radians) * Math.cos(latitude2Radians)
            * Math.sin(longitudeDeltaRadians / 2.0d) * Math.sin(longitudeDeltaRadians / 2.0d);

    double c = 2.0d * Math.atan2(Math.sqrt(a), Math.sqrt(1.0d - a));
    return 6_371_000.0d * c;
  }
}