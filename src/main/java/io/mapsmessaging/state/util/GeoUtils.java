package io.mapsmessaging.state.util;

import io.mapsmessaging.state.drone.model.GeoPosition;

public class GeoUtils {

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
