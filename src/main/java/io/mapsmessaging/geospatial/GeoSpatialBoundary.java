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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GeoSpatialBoundary {

  private final String name;
  private final GeoSpatialBoundaryType type;
  private final List<GeoPolygon> polygons;

  GeoSpatialBoundary(String name, GeoSpatialBoundaryType type, List<GeoPolygon> polygons) {
    this.name = requireName(name);
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.polygons = List.copyOf(Objects.requireNonNull(polygons, "polygons must not be null"));
    if (this.polygons.isEmpty()) {
      throw new IllegalArgumentException("boundary must contain at least one polygon");
    }
  }

  public String name() {
    return name;
  }

  public GeoSpatialBoundaryType type() {
    return type;
  }

  public int polygonCount() {
    return polygons.size();
  }

  public boolean covers(GeoPoint point) {
    return locate(Objects.requireNonNull(point, "point must not be null")) != GeoLocation.OUTSIDE;
  }

  GeoLocation locate(GeoPoint point) {
    GeoLocation result = GeoLocation.OUTSIDE;
    for (GeoPolygon polygon : polygons) {
      GeoLocation location = polygon.locate(point);
      if (location == GeoLocation.BOUNDARY) {
        return GeoLocation.BOUNDARY;
      }
      if (location == GeoLocation.INSIDE) {
        result = GeoLocation.INSIDE;
      }
    }
    return result;
  }

  void collectBoundaryIntersections(GeoPoint start, GeoPoint end, List<Double> parameters) {
    for (GeoPolygon polygon : polygons) {
      polygon.collectBoundaryIntersections(start, end, parameters);
    }
  }

  boolean segmentIntersects(GeoPoint start, GeoPoint end) {
    if (covers(start) || covers(end)) {
      return true;
    }

    List<Double> intersections = new ArrayList<>();
    collectBoundaryIntersections(start, end, intersections);
    return !intersections.isEmpty();
  }

  private static String requireName(String value) {
    Objects.requireNonNull(value, "name must not be null");
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    return trimmed;
  }
}
