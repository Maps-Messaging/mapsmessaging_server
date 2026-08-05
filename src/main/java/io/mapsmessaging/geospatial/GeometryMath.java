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

final class GeometryMath {

  static final double EPSILON = 1.0e-10;

  private GeometryMath() {}

  static boolean pointOnSegment(GeoPoint point, GeoPoint start, GeoPoint end) {
    double segmentX = end.longitude() - start.longitude();
    double segmentY = end.latitude() - start.latitude();
    double pointX = point.longitude() - start.longitude();
    double pointY = point.latitude() - start.latitude();

    if (Math.abs(cross(segmentX, segmentY, pointX, pointY)) > EPSILON) {
      return false;
    }

    double dot = pointX * segmentX + pointY * segmentY;
    if (dot < -EPSILON) {
      return false;
    }
    double lengthSquared = segmentX * segmentX + segmentY * segmentY;
    return dot <= lengthSquared + EPSILON;
  }

  static boolean segmentBoundsOverlap(
      GeoPoint start,
      GeoPoint end,
      double minimumLatitude,
      double maximumLatitude,
      double minimumLongitude,
      double maximumLongitude) {
    double segmentMinimumLatitude = Math.min(start.latitude(), end.latitude());
    double segmentMaximumLatitude = Math.max(start.latitude(), end.latitude());
    double segmentMinimumLongitude = Math.min(start.longitude(), end.longitude());
    double segmentMaximumLongitude = Math.max(start.longitude(), end.longitude());

    return segmentMaximumLatitude >= minimumLatitude - EPSILON
        && segmentMinimumLatitude <= maximumLatitude + EPSILON
        && segmentMaximumLongitude >= minimumLongitude - EPSILON
        && segmentMinimumLongitude <= maximumLongitude + EPSILON;
  }

  static void collectSegmentIntersections(
      GeoPoint firstStart,
      GeoPoint firstEnd,
      GeoPoint secondStart,
      GeoPoint secondEnd,
      List<Double> parameters) {
    double firstX = firstEnd.longitude() - firstStart.longitude();
    double firstY = firstEnd.latitude() - firstStart.latitude();
    double secondX = secondEnd.longitude() - secondStart.longitude();
    double secondY = secondEnd.latitude() - secondStart.latitude();
    double offsetX = secondStart.longitude() - firstStart.longitude();
    double offsetY = secondStart.latitude() - firstStart.latitude();

    double firstLengthSquared = firstX * firstX + firstY * firstY;
    if (firstLengthSquared <= EPSILON * EPSILON) {
      if (pointOnSegment(firstStart, secondStart, secondEnd)) {
        addParameter(parameters, 0.0);
      }
      return;
    }

    double denominator = cross(firstX, firstY, secondX, secondY);
    if (Math.abs(denominator) > EPSILON) {
      double firstParameter = cross(offsetX, offsetY, secondX, secondY) / denominator;
      double secondParameter = cross(offsetX, offsetY, firstX, firstY) / denominator;
      if (withinSegment(firstParameter) && withinSegment(secondParameter)) {
        addParameter(parameters, clamp(firstParameter));
      }
      return;
    }

    if (Math.abs(cross(offsetX, offsetY, firstX, firstY)) > EPSILON) {
      return;
    }

    double secondStartParameter = (offsetX * firstX + offsetY * firstY) / firstLengthSquared;
    double secondEndOffsetX = secondEnd.longitude() - firstStart.longitude();
    double secondEndOffsetY = secondEnd.latitude() - firstStart.latitude();
    double secondEndParameter =
        (secondEndOffsetX * firstX + secondEndOffsetY * firstY) / firstLengthSquared;

    double overlapStart = Math.max(0.0, Math.min(secondStartParameter, secondEndParameter));
    double overlapEnd = Math.min(1.0, Math.max(secondStartParameter, secondEndParameter));
    if (overlapStart <= overlapEnd + EPSILON) {
      addParameter(parameters, clamp(overlapStart));
      addParameter(parameters, clamp(overlapEnd));
    }
  }

  static GeoPoint interpolate(GeoPoint start, GeoPoint end, double parameter) {
    return new GeoPoint(
        start.latitude() + (end.latitude() - start.latitude()) * parameter,
        start.longitude() + (end.longitude() - start.longitude()) * parameter);
  }

  private static double cross(double firstX, double firstY, double secondX, double secondY) {
    return firstX * secondY - firstY * secondX;
  }

  private static boolean withinSegment(double value) {
    return value >= -EPSILON && value <= 1.0 + EPSILON;
  }

  private static double clamp(double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }

  private static void addParameter(List<Double> parameters, double value) {
    for (double existing : parameters) {
      if (Math.abs(existing - value) <= EPSILON) {
        return;
      }
    }
    parameters.add(value);
  }
}
