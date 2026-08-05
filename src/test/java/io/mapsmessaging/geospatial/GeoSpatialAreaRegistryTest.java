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
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoSpatialAreaRegistryTest {

  @Test
  void storesNamedAreasForReuse() throws IOException {
    GeoSpatialArea sharedArea = area("shared");
    GeoSpatialArea vesselArea = area("vessel-10");

    GeoSpatialAreaRegistry registry =
        GeoSpatialAreaRegistry.builder().add(sharedArea).add(vesselArea).build();

    assertEquals(2, registry.areas().size());
    assertTrue(registry.require("shared") == sharedArea);
    assertTrue(registry.find("vessel-10").isPresent());
  }

  @Test
  void rejectsDuplicateAreaNames() throws IOException {
    GeoSpatialArea first = area("duplicate");
    GeoSpatialArea second = area("duplicate");

    GeoSpatialAreaRegistry.Builder builder = GeoSpatialAreaRegistry.builder().add(first);

    assertThrows(IllegalArgumentException.class, () -> builder.add(second));
  }

  @Test
  void rejectsUnknownAreaName() throws IOException {
    GeoSpatialAreaRegistry registry = GeoSpatialAreaRegistry.builder().add(area("known")).build();

    assertThrows(IllegalArgumentException.class, () -> registry.require("missing"));
  }

  private static GeoSpatialArea area(String name) throws IOException {
    String geoJson =
        """
        {
          "type":"Polygon",
          "coordinates":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]
        }
        """;
    GeoSpatialBoundary boundary =
        GeoJsonBoundaryLoader.load(
            name + "-inside",
            new ByteArrayInputStream(geoJson.getBytes(StandardCharsets.UTF_8)),
            name + ".geojson",
            GeoSpatialBoundaryType.INSIDE);
    return GeoSpatialArea.builder(name).add(boundary).build();
  }
}
