package io.mapsmessaging.geospatial;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoRouteCoverageSweepTest {
  @Test
  void routeCopiesInputPointsAndRejectsNullMembers() {
    List<GeoPoint> source = new ArrayList<>();
    source.add(new GeoPoint(1, 2));

    GeoRoute route = new GeoRoute(source, true);
    source.add(new GeoPoint(3, 4));

    assertEquals(1, route.points().size());
    assertTrue(route.closed());
    assertThrows(UnsupportedOperationException.class, () -> route.points().add(new GeoPoint(5, 6)));
    assertThrows(NullPointerException.class, () -> new GeoRoute(null));
    assertThrows(NullPointerException.class, () -> new GeoRoute(java.util.Arrays.asList((GeoPoint) null)));
  }
}
