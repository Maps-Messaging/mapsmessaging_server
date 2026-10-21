package io.mapsmessaging.geospatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoViolationCoverageSweepTest {
  @Test
  void violationRetainsIndexesAndRequiresCoreFields() {
    GeoViolation violation = new GeoViolation(
        GeoViolationType.SEGMENT_INTERSECTS_DO_NOT_ENTER_BOUNDARY,
        "area",
        "restricted",
        null,
        2,
        3,
        true,
        "intersects");

    assertEquals("area", violation.areaName());
    assertEquals("restricted", violation.boundaryName());
    assertEquals(2, violation.segmentStartIndex());
    assertEquals(3, violation.segmentEndIndex());
    assertTrue(violation.closingSegment());

    assertThrows(
        NullPointerException.class,
        () -> new GeoViolation(null, "a", null, null, null, null, false, "r"));
    assertThrows(
        NullPointerException.class,
        () -> new GeoViolation(GeoViolationType.EMPTY_ROUTE, null, null, null, null, null, false, "r"));
  }
}
