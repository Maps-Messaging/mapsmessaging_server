package io.mapsmessaging.geospatial;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GeoJsonBoundaryLoaderFinalCoverageTest {

  @Test
  void polygonRootUsesGeoJsonNameAndCoversInteriorButNotExterior() throws Exception {
    GeoSpatialBoundary boundary = load(
        """
        {"type":"Polygon","name":"Harbour","coordinates":[[[0,0],[10,0],[10,10],[0,10],[0,0]]]}
        """,
        "harbour.geojson");

    assertEquals("Harbour", boundary.name());
    assertEquals(GeoSpatialBoundaryType.INSIDE, boundary.type());
    assertEquals(1, boundary.polygonCount());
    assertTrue(boundary.covers(new GeoPoint(5, 5)));
    assertFalse(boundary.covers(new GeoPoint(20, 20)));
  }

  @Test
  void explicitBoundaryNameOverridesDocumentNameAndFilenameFallbackStripsExtension() throws Exception {
    String json =
        """
        {"type":"Polygon","name":"DocumentName","coordinates":[[[0,0],[2,0],[2,2],[0,2],[0,0]]]}
        """;
    GeoSpatialBoundary explicit = GeoJsonBoundaryLoader.load(
        "  Explicit  ",
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
        "document.geojson",
        GeoSpatialBoundaryType.DO_NOT_ENTER);
    assertEquals("Explicit", explicit.name());
    assertEquals(GeoSpatialBoundaryType.DO_NOT_ENTER, explicit.type());

    String noName =
        """
        {"type":"Polygon","coordinates":[[[0,0],[2,0],[2,2],[0,2],[0,0]]]}
        """;
    GeoSpatialBoundary fallback = load(noName, "dir/path/area.geojson");
    assertEquals("area", fallback.name());
  }

  @Test
  void featureCollectionAndMultiPolygonAccumulateEveryPolygon() throws Exception {
    GeoSpatialBoundary featureCollection = load(
        """
        {"type":"FeatureCollection","features":[
          {"type":"Feature","geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,1],[0,0]]] }},
          {"type":"Feature","geometry":{"type":"Polygon","coordinates":[[[2,2],[3,2],[3,3],[2,3],[2,2]]] }}
        ]}
        """,
        "features.geojson");
    assertEquals(2, featureCollection.polygonCount());

    GeoSpatialBoundary multi = load(
        """
        {"type":"MultiPolygon","coordinates":[
          [[[0,0],[1,0],[1,1],[0,1],[0,0]]],
          [[[2,2],[3,2],[3,3],[2,3],[2,2]]]
        ]}
        """,
        "multi.geojson");
    assertEquals(2, multi.polygonCount());
  }

  @Test
  void invalidSourceCrsAndRingShapesAreRejected() {
    assertThrows(
        NullPointerException.class,
        () -> GeoJsonBoundaryLoader.load(
            new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)),
            null,
            GeoSpatialBoundaryType.INSIDE));

    assertThrows(
        IllegalArgumentException.class,
        () -> GeoJsonBoundaryLoader.load(
            new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)),
            " ",
            GeoSpatialBoundaryType.INSIDE));

    assertThrows(
        GeoJsonBoundaryException.class,
        () -> load(
            """
            {"type":"Polygon","crs":{"properties":{"name":"EPSG:3857"}},
             "coordinates":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]}
            """,
            "bad-crs.geojson"));

    assertThrows(
        GeoJsonBoundaryException.class,
        () -> load(
            """
            {"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,1]]]}
            """,
            "open.geojson"));
  }

  private static GeoSpatialBoundary load(String json, String source) throws Exception {
    return GeoJsonBoundaryLoader.load(
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
        source,
        GeoSpatialBoundaryType.INSIDE);
  }
}