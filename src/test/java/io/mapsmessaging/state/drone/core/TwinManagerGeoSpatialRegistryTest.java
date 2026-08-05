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

package io.mapsmessaging.state.drone.core;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.mapsmessaging.geospatial.GeoJsonBoundaryLoader;
import io.mapsmessaging.geospatial.GeoSpatialArea;
import io.mapsmessaging.geospatial.GeoSpatialAreaRegistry;
import io.mapsmessaging.geospatial.GeoSpatialBoundary;
import io.mapsmessaging.geospatial.GeoSpatialBoundaryType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class TwinManagerGeoSpatialRegistryTest {

  @Test
  void constructor_retainsSharedGeoSpatialAreaRegistry()
      throws IOException, URISyntaxException {
    Path boundaryPath =
        Path.of(
            Objects.requireNonNull(
                    getClass().getResource("/geospatial/offshore.geojson"),
                    "Missing offshore test resource")
                .toURI());
    GeoSpatialBoundary boundary =
        GeoJsonBoundaryLoader.load(
            "offshore", boundaryPath, GeoSpatialBoundaryType.INSIDE);
    GeoSpatialArea area =
        GeoSpatialArea.builder("sesimbra-usv").add(boundary).build();
    GeoSpatialAreaRegistry registry =
        GeoSpatialAreaRegistry.builder().add(area).build();

    TwinManager manager =
        new TwinManager(true, 10000L, 5000L, 120000L, null, registry);

    assertSame(registry, manager.getGeoSpatialAreaRegistry());
  }

  @Test
  void existingConstructorRemainsCompatibleWithoutGeoSpatialConfiguration() {
    TwinManager manager =
        new TwinManager(true, 10000L, 5000L, 120000L, null);

    assertNull(manager.getGeoSpatialAreaRegistry());
  }
}
