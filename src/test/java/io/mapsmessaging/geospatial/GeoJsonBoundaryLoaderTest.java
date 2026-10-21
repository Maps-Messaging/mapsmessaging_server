/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.geospatial;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoJsonBoundaryLoaderTest {

  @Test
  void loadsProvidedGeoJsonBoundaries() throws IOException {
    GeoSpatialBoundary offshore = loadResource("offshore.geojson", GeoSpatialBoundaryType.INSIDE);
    GeoSpatialBoundary rio1 = loadResource("rio_1.geojson", GeoSpatialBoundaryType.INSIDE);
    GeoSpatialBoundary rio2 = loadResource("rio_2.geojson", GeoSpatialBoundaryType.DO_NOT_ENTER);

    assertEquals("offshore", offshore.name());
    assertEquals("rio_1", rio1.name());
    assertEquals("rio_2", rio2.name());
    assertEquals(GeoSpatialBoundaryType.INSIDE, offshore.type());
    assertEquals(GeoSpatialBoundaryType.DO_NOT_ENTER, rio2.type());
    assertEquals(1, offshore.polygonCount());
    assertTrue(offshore.covers(new GeoPoint(38.14177506229976, -9.233324543996128)));
    assertTrue(rio1.covers(new GeoPoint(38.48868403969463, -8.869582907469646)));
    assertTrue(rio2.covers(new GeoPoint(38.48491076570355, -8.85870710743473)));
  }

  @Test
  void supportsExplicitBoundaryNameAndMultiPolygon() throws IOException {
    String geoJson =
        """
        {
          "type": "MultiPolygon",
          "coordinates": [
            [[[0,0],[2,0],[2,2],[0,2],[0,0]]],
            [[[4,0],[6,0],[6,2],[4,2],[4,0]]]
          ]
        }
        """;

    GeoSpatialBoundary boundary =
        GeoJsonBoundaryLoader.load(
            "two-zones", stream(geoJson), "multi.geojson", GeoSpatialBoundaryType.INSIDE);

    assertEquals("two-zones", boundary.name());
    assertEquals(2, boundary.polygonCount());
    assertTrue(boundary.covers(new GeoPoint(1, 1)));
    assertTrue(boundary.covers(new GeoPoint(1, 5)));
  }

  @Test
  void rejectsUnclosedRing() {
    String geoJson =
        """
        {
          "type": "Polygon",
          "coordinates": [[[0,0],[2,0],[2,2],[0,2]]]
        }
        """;

    assertThrows(
        GeoJsonBoundaryException.class,
        () ->
            GeoJsonBoundaryLoader.load(
                stream(geoJson), "unclosed.geojson", GeoSpatialBoundaryType.INSIDE));
  }

  @Test
  void rejectsSelfIntersectingRing() {
    String geoJson =
        """
        {
          "type": "Polygon",
          "coordinates": [[[0,0],[2,2],[0,2],[2,0],[0,0]]]
        }
        """;

    assertThrows(
        GeoJsonBoundaryException.class,
        () ->
            GeoJsonBoundaryLoader.load(
                stream(geoJson), "self-intersecting.geojson", GeoSpatialBoundaryType.INSIDE));
  }

  @Test
  void rejectsUnsupportedGeometry() {
    String geoJson =
        """
        {
          "type": "LineString",
          "coordinates": [[0,0],[1,1]]
        }
        """;

    assertThrows(
        GeoJsonBoundaryException.class,
        () ->
            GeoJsonBoundaryLoader.load(
                stream(geoJson), "line.geojson", GeoSpatialBoundaryType.INSIDE));
  }

  @Test
  void rejectsUnsupportedCoordinateReferenceSystem() {
    String geoJson =
        """
        {
          "type": "Polygon",
          "crs": {"type":"name","properties":{"name":"EPSG:3857"}},
          "coordinates": [[[0,0],[2,0],[2,2],[0,2],[0,0]]]
        }
        """;

    assertThrows(
        GeoJsonBoundaryException.class,
        () ->
            GeoJsonBoundaryLoader.load(
                stream(geoJson), "mercator.geojson", GeoSpatialBoundaryType.INSIDE));
  }

  private static GeoSpatialBoundary loadResource(
      String name, GeoSpatialBoundaryType boundaryType) throws IOException {
    try (InputStream inputStream =
        GeoJsonBoundaryLoaderTest.class.getResourceAsStream("/geospatial/" + name)) {
      if (inputStream == null) {
        throw new IllegalStateException("Missing test resource " + name);
      }
      return GeoJsonBoundaryLoader.load(inputStream, name, boundaryType);
    }
  }

  private static InputStream stream(String value) {
    return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
  }
}
