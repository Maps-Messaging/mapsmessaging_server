package io.mapsmessaging.geospatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoPointCoverageSweepTest {
  @Test
  void acceptsBoundaryCoordinatesAndRejectsNonFiniteOrOutOfRangeValues() {
    assertEquals(new GeoPoint(-90, -180), new GeoPoint(-90, -180));
    assertEquals(new GeoPoint(90, 180), new GeoPoint(90, 180));

    assertThrows(IllegalArgumentException.class, () -> new GeoPoint(91, 0));
    assertThrows(IllegalArgumentException.class, () -> new GeoPoint(0, 181));
    assertThrows(IllegalArgumentException.class, () -> new GeoPoint(Double.NaN, 0));
    assertThrows(IllegalArgumentException.class, () -> new GeoPoint(0, Double.POSITIVE_INFINITY));
  }
}
