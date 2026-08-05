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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoRouteValidatorTest {

  @Test
  void acceptsRouteInsidePermittedBoundary() throws IOException {
    GeoSpatialArea area = area("allowed", boundary("allowed", square(0, 0, 10, 10), GeoSpatialBoundaryType.INSIDE));

    GeoValidationResult result =
        area.validate(new GeoRoute(List.of(point(1, 1), point(5, 5), point(9, 1))));

    assertTrue(result.executable());
    assertTrue(result.violations().isEmpty());
  }

  @Test
  void rejectsPointOutsidePermittedBoundary() throws IOException {
    GeoSpatialArea area = area("allowed", boundary("allowed", square(0, 0, 10, 10), GeoSpatialBoundaryType.INSIDE));

    GeoValidationResult result =
        area.validate(new GeoRoute(List.of(point(1, 1), point(12, 5))));

    assertFalse(result.executable());
    assertTrue(hasViolation(result, GeoViolationType.POINT_OUTSIDE_INSIDE_BOUNDARIES));
    assertTrue(hasViolation(result, GeoViolationType.SEGMENT_OUTSIDE_INSIDE_BOUNDARIES));
  }

  @Test
  void rejectsSegmentThatCrossesHoleWithValidEndpoints() throws IOException {
    String polygonWithHole =
        """
        {
          "type":"Polygon",
          "coordinates":[
            [[0,0],[10,0],[10,10],[0,10],[0,0]],
            [[4,4],[6,4],[6,6],[4,6],[4,4]]
          ]
        }
        """;
    GeoSpatialArea area =
        area("allowed", boundary("allowed", polygonWithHole, GeoSpatialBoundaryType.INSIDE));

    GeoValidationResult result =
        area.validate(new GeoRoute(List.of(point(5, 2), point(5, 8))));

    assertFalse(result.executable());
    assertFalse(hasViolation(result, GeoViolationType.POINT_OUTSIDE_INSIDE_BOUNDARIES));
    assertTrue(hasViolation(result, GeoViolationType.SEGMENT_OUTSIDE_INSIDE_BOUNDARIES));
  }

  @Test
  void treatsMultipleInsideBoundariesAsAUnion() throws IOException {
    GeoSpatialBoundary first =
        boundary("first", square(0, 0, 6, 6), GeoSpatialBoundaryType.INSIDE);
    GeoSpatialBoundary second =
        boundary("second", square(4, 0, 10, 6), GeoSpatialBoundaryType.INSIDE);
    GeoSpatialArea area = area("joined", first, second);

    GeoValidationResult result =
        area.validate(new GeoRoute(List.of(point(3, 1), point(3, 9))));

    assertTrue(result.executable());
  }

  @Test
  void rejectsTravelBetweenDisconnectedInsideBoundaries() throws IOException {
    GeoSpatialBoundary first =
        boundary("first", square(0, 0, 2, 2), GeoSpatialBoundaryType.INSIDE);
    GeoSpatialBoundary second =
        boundary("second", square(4, 0, 6, 2), GeoSpatialBoundaryType.INSIDE);
    GeoSpatialArea area = area("disconnected", first, second);

    GeoValidationResult result =
        area.validate(new GeoRoute(List.of(point(1, 1), point(1, 5))));

    assertFalse(result.executable());
    assertFalse(hasViolation(result, GeoViolationType.POINT_OUTSIDE_INSIDE_BOUNDARIES));
    assertTrue(hasViolation(result, GeoViolationType.SEGMENT_OUTSIDE_INSIDE_BOUNDARIES));
  }

  @Test
  void rejectsRouteCrossingDoNotEnterBoundary() throws IOException {
    GeoSpatialBoundary allowed =
        boundary("allowed", square(0, 0, 10, 10), GeoSpatialBoundaryType.INSIDE);
    GeoSpatialBoundary restricted =
        boundary("restricted", square(4, 4, 6, 6), GeoSpatialBoundaryType.DO_NOT_ENTER);
    GeoSpatialArea area = area("operating-area", allowed, restricted);

    GeoValidationResult result =
        area.validate(new GeoRoute(List.of(point(5, 2), point(5, 8))));

    assertFalse(result.executable());
    GeoViolation violation =
        result.violations().stream()
            .filter(value -> value.type() == GeoViolationType.SEGMENT_INTERSECTS_DO_NOT_ENTER_BOUNDARY)
            .findFirst()
            .orElseThrow();
    assertEquals("restricted", violation.boundaryName());
    assertEquals(Integer.valueOf(0), violation.segmentStartIndex());
    assertEquals(Integer.valueOf(1), violation.segmentEndIndex());
  }

  @Test
  void rejectsRouteTouchingDoNotEnterBoundary() throws IOException {
    GeoSpatialBoundary allowed =
        boundary("allowed", square(0, 0, 10, 10), GeoSpatialBoundaryType.INSIDE);
    GeoSpatialBoundary restricted =
        boundary("restricted", square(4, 4, 6, 6), GeoSpatialBoundaryType.DO_NOT_ENTER);
    GeoSpatialArea area = area("operating-area", allowed, restricted);

    GeoValidationResult result =
        area.validate(new GeoRoute(List.of(point(4, 2), point(4, 8))));

    assertFalse(result.executable());
    assertTrue(hasViolation(result, GeoViolationType.SEGMENT_INTERSECTS_DO_NOT_ENTER_BOUNDARY));
  }

  @Test
  void validatesClosingSegmentForRepeatedRoute() throws IOException {
    String polygonWithHole =
        """
        {
          "type":"Polygon",
          "coordinates":[
            [[0,0],[10,0],[10,10],[0,10],[0,0]],
            [[4,4],[6,4],[6,6],[4,6],[4,4]]
          ]
        }
        """;
    GeoSpatialArea area =
        area("allowed", boundary("allowed", polygonWithHole, GeoSpatialBoundaryType.INSIDE));

    GeoValidationResult result =
        area.validate(new GeoRoute(List.of(point(5, 2), point(8, 5), point(5, 8)), true));

    assertFalse(result.executable());
    assertTrue(
        result.violations().stream()
            .anyMatch(
                violation ->
                    violation.type() == GeoViolationType.SEGMENT_OUTSIDE_INSIDE_BOUNDARIES
                        && violation.closingSegment()));
  }

  @Test
  void allowsUnrestrictedRouteWhenAreaOnlyContainsDoNotEnterBoundaries() throws IOException {
    GeoSpatialArea area =
        area(
            "restricted-only",
            boundary("restricted", square(4, 4, 6, 6), GeoSpatialBoundaryType.DO_NOT_ENTER));

    GeoValidationResult result =
        area.validate(new GeoRoute(List.of(point(-20, -20), point(-10, -10))));

    assertTrue(result.executable());
  }

  @Test
  void rejectsProvidedInlandMissionAgainstPortugueseWaterAreas() throws IOException {
    GeoSpatialArea area =
        GeoSpatialArea.builder("portugal-usv")
            .add(loadResource("offshore.geojson"))
            .add(loadResource("rio_1.geojson"))
            .add(loadResource("rio_2.geojson"))
            .build();

    GeoRoute route =
        new GeoRoute(
            List.of(
                point(39.53405089931635, -8.884699636525404),
                point(39.54230663121304, -8.504314146402102),
                point(39.331912630616216, -8.533982349564374),
                point(39.31314171710805, -8.097012942158953)));

    GeoValidationResult result = area.validate(route);

    assertFalse(result.executable());
    assertEquals(4, countViolations(result, GeoViolationType.POINT_OUTSIDE_INSIDE_BOUNDARIES));
  }

  private static GeoSpatialArea area(String name, GeoSpatialBoundary... boundaries) {
    return GeoSpatialArea.builder(name).addAll(List.of(boundaries)).build();
  }

  private static GeoSpatialBoundary boundary(
      String name, String geoJson, GeoSpatialBoundaryType type) throws IOException {
    return GeoJsonBoundaryLoader.load(name, stream(geoJson), name + ".geojson", type);
  }

  private static GeoSpatialBoundary loadResource(String name) throws IOException {
    try (InputStream inputStream =
        GeoRouteValidatorTest.class.getResourceAsStream("/geospatial/" + name)) {
      if (inputStream == null) {
        throw new IllegalStateException("Missing test resource " + name);
      }
      return GeoJsonBoundaryLoader.load(inputStream, name, GeoSpatialBoundaryType.INSIDE);
    }
  }

  private static String square(double minLongitude, double minLatitude, double maxLongitude, double maxLatitude) {
    return String.format(
        "{\"type\":\"Polygon\",\"coordinates\":[[[%f,%f],[%f,%f],[%f,%f],[%f,%f],[%f,%f]]]}",
        minLongitude,
        minLatitude,
        maxLongitude,
        minLatitude,
        maxLongitude,
        maxLatitude,
        minLongitude,
        maxLatitude,
        minLongitude,
        minLatitude);
  }

  private static GeoPoint point(double latitude, double longitude) {
    return new GeoPoint(latitude, longitude);
  }

  private static InputStream stream(String value) {
    return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
  }

  private static boolean hasViolation(GeoValidationResult result, GeoViolationType type) {
    return result.violations().stream().anyMatch(violation -> violation.type() == type);
  }

  private static long countViolations(GeoValidationResult result, GeoViolationType type) {
    return result.violations().stream().filter(violation -> violation.type() == type).count();
  }
}
