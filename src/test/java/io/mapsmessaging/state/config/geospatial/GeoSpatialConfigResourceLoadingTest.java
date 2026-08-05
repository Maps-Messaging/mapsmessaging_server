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

package io.mapsmessaging.state.config.geospatial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.geospatial.GeoSpatialAreaRegistry;
import io.mapsmessaging.geospatial.GeoSpatialBoundaryType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class GeoSpatialConfigResourceLoadingTest {

  @Test
  void loader_resolvesRelativePathAgainstConfigurationDirectory()
      throws IOException, URISyntaxException {
    GeoSpatialBoundaryConfigDTO boundary = new GeoSpatialBoundaryConfigDTO();
    boundary.setName("offshore");
    boundary.setPath("geospatial/offshore.geojson");
    boundary.setType(GeoSpatialBoundaryType.INSIDE);

    GeoSpatialAreaConfigDTO area = new GeoSpatialAreaConfigDTO();
    area.setName("sesimbra-usv");
    area.setBoundaries(List.of(boundary));

    GeoSpatialConfigDTO config = new GeoSpatialConfigDTO();
    config.setAreas(List.of(area));

    Path offshorePath =
        Path.of(
            Objects.requireNonNull(
                    getClass().getResource("/geospatial/offshore.geojson"),
                    "Missing offshore test resource")
                .toURI());
    Path configurationDirectory = offshorePath.getParent().getParent();

    GeoSpatialAreaRegistry registry =
        GeoSpatialConfigLoader.load(config, List.of(), configurationDirectory);

    assertEquals(1, registry.areas().size());
    assertEquals(1, registry.require("sesimbra-usv").boundaries().size());
  }
}
