package io.mapsmessaging.geospatial;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoRouteValidatorCoverageSweepTest {
  @Test
  void validatorRejectsEmptyAndOutsideRoutesAndAcceptsInsideRoute() throws Exception {
    String json =
        "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[10,0],[10,10],[0,10],[0,0]]]}";
    GeoSpatialBoundary inside = GeoJsonBoundaryLoader.load(
        "inside",
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
        "inside.geojson",
        GeoSpatialBoundaryType.INSIDE);
    GeoSpatialArea area = GeoSpatialArea.builder("area").add(inside).build();

    GeoValidationResult empty = GeoRouteValidator.validate(area, new GeoRoute(List.of()));
    assertFalse(empty.executable());
    assertEquals(GeoViolationType.EMPTY_ROUTE, empty.primaryViolation().orElseThrow().type());

    GeoValidationResult valid = GeoRouteValidator.validate(
        area,
        new GeoRoute(List.of(new GeoPoint(1, 1), new GeoPoint(2, 2))));
    assertTrue(valid.executable());

    GeoValidationResult outside = GeoRouteValidator.validate(
        area,
        new GeoRoute(List.of(new GeoPoint(1, 1), new GeoPoint(20, 20))));
    assertFalse(outside.executable());
  }
}
