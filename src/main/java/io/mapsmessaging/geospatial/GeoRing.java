/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 */

package io.mapsmessaging.geospatial;

import java.util.List;
import java.util.Objects;

final class GeoRing {

  private final List<GeoPoint> points;
  private final double minimumLatitude;
  private final double maximumLatitude;
  private final double minimumLongitude;
  private final double maximumLongitude;

  GeoRing(List<GeoPoint> points) {
    this.points = List.copyOf(Objects.requireNonNull(points, "points must not be null"));
    if (this.points.size() < 4) {
      throw new IllegalArgumentException("a polygon ring must contain at least four positions");
    }
    if (!this.points.get(0).equals(this.points.get(this.points.size() - 1))) {
      throw new IllegalArgumentException("a polygon ring must be closed");
    }

    double minLatitude = Double.POSITIVE_INFINITY;
    double maxLatitude = Double.NEGATIVE_INFINITY;
    double minLongitude = Double.POSITIVE_INFINITY;
    double maxLongitude = Double.NEGATIVE_INFINITY;
    for (GeoPoint point : this.points) {
      minLatitude = Math.min(minLatitude, point.latitude());
      maxLatitude = Math.max(maxLatitude, point.latitude());
      minLongitude = Math.min(minLongitude, point.longitude());
      maxLongitude = Math.max(maxLongitude, point.longitude());
    }
    minimumLatitude = minLatitude;
    maximumLatitude = maxLatitude;
    minimumLongitude = minLongitude;
    maximumLongitude = maxLongitude;
  }

  GeoLocation locate(GeoPoint point) {
    if (point.latitude() < minimumLatitude - GeometryMath.EPSILON
        || point.latitude() > maximumLatitude + GeometryMath.EPSILON
        || point.longitude() < minimumLongitude - GeometryMath.EPSILON
        || point.longitude() > maximumLongitude + GeometryMath.EPSILON) {
      return GeoLocation.OUTSIDE;
    }

    boolean inside = false;
    for (int index = 1; index < points.size(); index++) {
      GeoPoint first = points.get(index - 1);
      GeoPoint second = points.get(index);
      if (GeometryMath.pointOnSegment(point, first, second)) {
        return GeoLocation.BOUNDARY;
      }

      boolean crossesLatitude =
          (first.latitude() > point.latitude()) != (second.latitude() > point.latitude());
      if (crossesLatitude) {
        double crossingLongitude =
            first.longitude()
                + (point.latitude() - first.latitude())
                    * (second.longitude() - first.longitude())
                    / (second.latitude() - first.latitude());
        if (crossingLongitude > point.longitude()) {
          inside = !inside;
        }
      }
    }
    return inside ? GeoLocation.INSIDE : GeoLocation.OUTSIDE;
  }

  void collectBoundaryIntersections(GeoPoint start, GeoPoint end, List<Double> parameters) {
    if (!GeometryMath.segmentBoundsOverlap(
        start,
        end,
        minimumLatitude,
        maximumLatitude,
        minimumLongitude,
        maximumLongitude)) {
      return;
    }

    for (int index = 1; index < points.size(); index++) {
      GeometryMath.collectSegmentIntersections(
          start, end, points.get(index - 1), points.get(index), parameters);
    }
  }
}
