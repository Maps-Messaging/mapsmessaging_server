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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class GeoRouteValidator {

  private GeoRouteValidator() {}

  public static GeoValidationResult validate(GeoSpatialArea area, GeoRoute route) {
    Objects.requireNonNull(area, "area must not be null");
    Objects.requireNonNull(route, "route must not be null");

    List<GeoViolation> violations = new ArrayList<>();
    if (route.points().isEmpty()) {
      violations.add(
          new GeoViolation(
              GeoViolationType.EMPTY_ROUTE,
              area.name(),
              null,
              null,
              null,
              null,
              false,
              "Route contains no points"));
      return GeoValidationResult.rejected(violations);
    }

    validatePoints(area, route.points(), violations);
    validateSegments(area, route.points(), violations);
    if (route.closed() && route.points().size() > 1) {
      validateSegment(
          area,
          route.points().get(route.points().size() - 1),
          route.points().get(0),
          route.points().size() - 1,
          0,
          true,
          violations);
    }

    return violations.isEmpty()
        ? GeoValidationResult.valid()
        : GeoValidationResult.rejected(violations);
  }

  private static void validatePoints(
      GeoSpatialArea area, List<GeoPoint> points, List<GeoViolation> violations) {
    for (int index = 0; index < points.size(); index++) {
      GeoPoint point = points.get(index);
      if (!area.coversInsideBoundaries(point)) {
        violations.add(
            new GeoViolation(
                GeoViolationType.POINT_OUTSIDE_INSIDE_BOUNDARIES,
                area.name(),
                null,
                index,
                null,
                null,
                false,
                "Point " + index + " is outside the permitted boundaries for area '" + area.name() + "'"));
      }

      for (GeoSpatialBoundary boundary : area.doNotEnterBoundaries()) {
        if (boundary.covers(point)) {
          violations.add(
              new GeoViolation(
                  GeoViolationType.POINT_IN_DO_NOT_ENTER_BOUNDARY,
                  area.name(),
                  boundary.name(),
                  index,
                  null,
                  null,
                  false,
                  "Point " + index + " is inside do-not-enter boundary '" + boundary.name() + "'"));
        }
      }
    }
  }

  private static void validateSegments(
      GeoSpatialArea area, List<GeoPoint> points, List<GeoViolation> violations) {
    for (int index = 1; index < points.size(); index++) {
      validateSegment(
          area,
          points.get(index - 1),
          points.get(index),
          index - 1,
          index,
          false,
          violations);
    }
  }

  private static void validateSegment(
      GeoSpatialArea area,
      GeoPoint start,
      GeoPoint end,
      int startIndex,
      int endIndex,
      boolean closingSegment,
      List<GeoViolation> violations) {
    if (!isCoveredByInsideBoundaries(area, start, end)) {
      violations.add(
          new GeoViolation(
              GeoViolationType.SEGMENT_OUTSIDE_INSIDE_BOUNDARIES,
              area.name(),
              null,
              null,
              startIndex,
              endIndex,
              closingSegment,
              segmentPrefix(closingSegment, startIndex, endIndex)
                  + " leaves the permitted boundaries for area '"
                  + area.name()
                  + "'"));
    }

    for (GeoSpatialBoundary boundary : area.doNotEnterBoundaries()) {
      if (boundary.segmentIntersects(start, end)) {
        violations.add(
            new GeoViolation(
                GeoViolationType.SEGMENT_INTERSECTS_DO_NOT_ENTER_BOUNDARY,
                area.name(),
                boundary.name(),
                null,
                startIndex,
                endIndex,
                closingSegment,
                segmentPrefix(closingSegment, startIndex, endIndex)
                    + " intersects do-not-enter boundary '"
                    + boundary.name()
                    + "'"));
      }
    }
  }

  private static boolean isCoveredByInsideBoundaries(
      GeoSpatialArea area, GeoPoint start, GeoPoint end) {
    if (area.insideBoundaries().isEmpty()) {
      return true;
    }
    if (!area.coversInsideBoundaries(start) || !area.coversInsideBoundaries(end)) {
      return false;
    }
    if (start.equals(end)) {
      return true;
    }

    List<Double> parameters = new ArrayList<>();
    parameters.add(0.0);
    parameters.add(1.0);
    for (GeoSpatialBoundary boundary : area.insideBoundaries()) {
      boundary.collectBoundaryIntersections(start, end, parameters);
    }

    parameters.sort(Comparator.naturalOrder());
    List<Double> uniqueParameters = new ArrayList<>();
    for (double parameter : parameters) {
      if (uniqueParameters.isEmpty()
          || Math.abs(uniqueParameters.get(uniqueParameters.size() - 1) - parameter)
              > GeometryMath.EPSILON) {
        uniqueParameters.add(parameter);
      }
    }

    for (int index = 1; index < uniqueParameters.size(); index++) {
      double intervalStart = uniqueParameters.get(index - 1);
      double intervalEnd = uniqueParameters.get(index);
      if (intervalEnd - intervalStart <= GeometryMath.EPSILON) {
        continue;
      }
      GeoPoint midpoint = GeometryMath.interpolate(start, end, (intervalStart + intervalEnd) / 2.0);
      if (!area.coversInsideBoundaries(midpoint)) {
        return false;
      }
    }
    return true;
  }

  private static String segmentPrefix(boolean closingSegment, int startIndex, int endIndex) {
    return (closingSegment ? "Closing segment " : "Segment ") + startIndex + " -> " + endIndex;
  }
}
