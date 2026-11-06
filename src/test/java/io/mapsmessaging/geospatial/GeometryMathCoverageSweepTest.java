package io.mapsmessaging.geospatial;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeometryMathCoverageSweepTest {
  @Test
  void geometryHelpersDetectPointsIntersectionsAndInterpolation() {
    GeoPoint a = new GeoPoint(0, 0);
    GeoPoint b = new GeoPoint(10, 10);

    assertTrue(GeometryMath.pointOnSegment(new GeoPoint(5, 5), a, b));
    assertFalse(GeometryMath.pointOnSegment(new GeoPoint(5, 6), a, b));
    assertTrue(GeometryMath.segmentBoundsOverlap(a, b, 4, 6, 4, 6));
    assertFalse(GeometryMath.segmentBoundsOverlap(a, b, 20, 30, 20, 30));

    List<Double> intersections = new ArrayList<>();
    GeometryMath.collectSegmentIntersections(
        a,
        b,
        new GeoPoint(0, 10),
        new GeoPoint(10, 0),
        intersections);
    assertEquals(1, intersections.size());
    assertEquals(0.5, intersections.getFirst(), 1.0e-9);

    assertEquals(new GeoPoint(5, 5), GeometryMath.interpolate(a, b, 0.5));
  }
}
