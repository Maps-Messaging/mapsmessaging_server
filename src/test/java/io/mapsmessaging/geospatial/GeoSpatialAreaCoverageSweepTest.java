package io.mapsmessaging.geospatial;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GeoSpatialAreaCoverageSweepTest {
  @Test
  void areaRequiresBoundariesAndAppliesInsideAndDoNotEnterRules() throws Exception {
    GeoSpatialBoundary inside = boundary(
        "inside",
        GeoSpatialBoundaryType.INSIDE,
        "[[0,0],[10,0],[10,10],[0,10],[0,0]]");
    GeoSpatialBoundary excluded = boundary(
        "excluded",
        GeoSpatialBoundaryType.DO_NOT_ENTER,
        "[[4,4],[6,4],[6,6],[4,6],[4,4]]");

    GeoSpatialArea area = GeoSpatialArea.builder("test-area")
        .add(inside)
        .add(excluded)
        .build();

    assertTrue(area.allows(new GeoPoint(2, 2)));
    assertFalse(area.allows(new GeoPoint(5, 5)));
    assertFalse(area.allows(new GeoPoint(20, 20)));
    assertTrue(area.boundary("inside").isPresent());
    assertThrows(IllegalArgumentException.class, () -> GeoSpatialArea.builder("empty").build());
  }

  private static GeoSpatialBoundary boundary(
      String name,
      GeoSpatialBoundaryType type,
      String ring) throws Exception {
    String json = "{\"type\":\"Polygon\",\"coordinates\":[" + ring + "]}";
    return GeoJsonBoundaryLoader.load(
        name,
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
        name + ".geojson",
        type);
  }
}
