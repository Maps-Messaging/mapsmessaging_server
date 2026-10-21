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

package io.mapsmessaging.state.util;

import io.mapsmessaging.state.drone.model.GeoPosition;

public class GeoUtils {

  private static final double EARTH_RADIUS_METERS = 6_371_000.0d;

  public static double distanceMeters(GeoPosition firstPosition, GeoPosition secondPosition) {
    validatePosition(firstPosition, "firstPosition");
    validatePosition(secondPosition, "secondPosition");

    double firstLatitudeRadians = Math.toRadians(firstPosition.getLatitude());
    double secondLatitudeRadians = Math.toRadians(secondPosition.getLatitude());
    double latitudeDeltaRadians = Math.toRadians(secondPosition.getLatitude() - firstPosition.getLatitude());
    double longitudeDeltaRadians = Math.toRadians(secondPosition.getLongitude() - firstPosition.getLongitude());

    double latitudeSin = Math.sin(latitudeDeltaRadians / 2.0d);
    double longitudeSin = Math.sin(longitudeDeltaRadians / 2.0d);

    double haversine =
        latitudeSin * latitudeSin
            + Math.cos(firstLatitudeRadians)
            * Math.cos(secondLatitudeRadians)
            * longitudeSin
            * longitudeSin;

    double angularDistance =
        2.0d * Math.atan2(Math.sqrt(haversine), Math.sqrt(1.0d - haversine));

    return EARTH_RADIUS_METERS * angularDistance;
  }

  public static double altitudeDeltaMeters(GeoPosition firstPosition, GeoPosition secondPosition) {
    Double firstAltitudeMeters = firstPosition.getPreferredAltitudeMeters();
    Double secondAltitudeMeters = secondPosition.getPreferredAltitudeMeters();

    if (firstAltitudeMeters == null || secondAltitudeMeters == null) {
      return Double.MAX_VALUE;
    }

    return Math.abs(firstAltitudeMeters - secondAltitudeMeters);
  }

  private static void validatePosition(GeoPosition position, String parameterName) {
    if (position == null) {
      throw new IllegalArgumentException(parameterName + " can not be null");
    }

    if (position.getLatitude() == null) {
      throw new IllegalArgumentException(parameterName + ".latitude can not be null");
    }

    if (position.getLongitude() == null) {
      throw new IllegalArgumentException(parameterName + ".longitude can not be null");
    }
  }

  public static float bearingDegrees(GeoPosition currentPosition, GeoPosition destinationPosition) {
    if (!hasValidCoordinates(currentPosition) || !hasValidCoordinates(destinationPosition)) {
      return 0.0f;
    }

    double currentLatitudeRadians = Math.toRadians(currentPosition.getLatitude());
    double currentLongitudeRadians = Math.toRadians(currentPosition.getLongitude());
    double destinationLatitudeRadians = Math.toRadians(destinationPosition.getLatitude());
    double destinationLongitudeRadians = Math.toRadians(destinationPosition.getLongitude());

    double longitudeDifferenceRadians = destinationLongitudeRadians - currentLongitudeRadians;

    double xComponent = Math.sin(longitudeDifferenceRadians) * Math.cos(destinationLatitudeRadians);

    double yComponent =
        Math.cos(currentLatitudeRadians) * Math.sin(destinationLatitudeRadians)
            - Math.sin(currentLatitudeRadians)
            * Math.cos(destinationLatitudeRadians)
            * Math.cos(longitudeDifferenceRadians);

    if (xComponent == 0.0 && yComponent == 0.0) {
      return 0.0f;
    }

    double bearingDegrees = Math.toDegrees(Math.atan2(xComponent, yComponent));

    return (float) ((bearingDegrees + 360.0) % 360.0);
  }

  private static boolean hasValidCoordinates(GeoPosition geoPosition) {
    if (geoPosition == null) {
      return false;
    }

    Double latitude = geoPosition.getLatitude();
    Double longitude = geoPosition.getLongitude();

    if (latitude == null || longitude == null) {
      return false;
    }

    if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
      return false;
    }

    return latitude >= -90.0
        && latitude <= 90.0
        && longitude >= -180.0
        && longitude <= 180.0;
  }
}
