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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.geospatial.GeoSpatialAreaRegistry;
import io.mapsmessaging.geospatial.GeoSpatialBoundaryType;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.TwinManagerConfig;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class GeoSpatialTwinManagerConfigTest {

  @Test
  void twinManagerConfig_readsAndWritesTopLevelGeospatialAreas()
      throws ReflectiveOperationException {
    ConfigurationProperties root = new ConfigurationProperties();
    ConfigurationProperties geospatial = new ConfigurationProperties();
    ConfigurationProperties area = new ConfigurationProperties();
    ConfigurationProperties inside = new ConfigurationProperties();
    ConfigurationProperties excluded = new ConfigurationProperties();

    inside.put("name", "offshore");
    inside.put("path", "/opt/maps/conf/geospatial/offshore.geojson");
    inside.put("type", "inside");
    excluded.put("name", "restricted");
    excluded.put("path", "/opt/maps/conf/geospatial/restricted.geojson");
    excluded.put("type", "DO_NOT_ENTER");
    area.put("name", "sesimbra-usv");
    area.put("boundaries", List.of(inside, excluded));
    geospatial.put("areas", List.of(area));
    root.put("geospatial", geospatial);

    ConfigurationProperties drone = new ConfigurationProperties();
    drone.put("name", "stickleback");
    drone.put("geospatialArea", "sesimbra-usv");
    root.put("droneInfo", List.of(drone));

    TwinManagerConfig config = newTwinManagerConfig(root);

    assertEquals(1, config.getGeospatial().getAreas().size());
    assertEquals("sesimbra-usv", config.getGeospatial().getAreas().get(0).getName());
    assertEquals(2, config.getGeospatial().getAreas().get(0).getBoundaries().size());
    assertEquals(
        GeoSpatialBoundaryType.INSIDE,
        config.getGeospatial().getAreas().get(0).getBoundaries().get(0).getType());
    assertEquals(
        GeoSpatialBoundaryType.DO_NOT_ENTER,
        config.getGeospatial().getAreas().get(0).getBoundaries().get(1).getType());
    assertEquals("sesimbra-usv", config.getDroneInfo().get(0).getGeospatialArea());

    ConfigurationProperties saved = config.toConfigurationProperties();
    ConfigurationProperties savedGeospatial =
        assertInstanceOf(ConfigurationProperties.class, saved.get("geospatial"));
    List<?> savedAreas = assertInstanceOf(List.class, savedGeospatial.get("areas"));
    ConfigurationProperties savedArea =
        assertInstanceOf(ConfigurationProperties.class, savedAreas.get(0));
    List<?> savedBoundaries = assertInstanceOf(List.class, savedArea.get("boundaries"));
    ConfigurationProperties savedBoundary =
        assertInstanceOf(ConfigurationProperties.class, savedBoundaries.get(1));
    assertEquals("DO_NOT_ENTER", savedBoundary.getProperty("type"));

    List<?> savedDrones = assertInstanceOf(List.class, saved.get("droneInfo"));
    ConfigurationProperties savedDrone =
        assertInstanceOf(ConfigurationProperties.class, savedDrones.get(0));
    assertEquals("sesimbra-usv", savedDrone.getProperty("geospatialArea"));
  }

  @Test
  void loader_loadsOneNamedAreaSharedByMultipleDrones()
      throws IOException, URISyntaxException {
    GeoSpatialConfigDTO config = portugalConfig();
    DroneInfoDTO first = drone("stickleback-one", "sesimbra-usv");
    DroneInfoDTO second = drone("stickleback-two", "sesimbra-usv");

    GeoSpatialAreaRegistry registry =
        GeoSpatialConfigLoader.load(config, List.of(first, second));

    assertEquals(1, registry.areas().size());
    assertEquals(3, registry.require("sesimbra-usv").boundaries().size());
    assertEquals("sesimbra-usv", first.getGeospatialArea());
    assertEquals("sesimbra-usv", second.getGeospatialArea());
  }

  @Test
  void loader_rejectsDroneReferencingUnknownArea()
      throws URISyntaxException {
    GeoSpatialConfigDTO config = portugalConfig();
    DroneInfoDTO drone = drone("lost-boat", "sesimbra-typo");

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> GeoSpatialConfigLoader.load(config, List.of(drone)));

    assertEquals(
        "Drone 'lost-boat' references unknown geospatial area 'sesimbra-typo'",
        exception.getMessage());
  }

  @Test
  void loader_rejectsMissingBoundaryFile() {
    GeoSpatialBoundaryConfigDTO boundary = new GeoSpatialBoundaryConfigDTO();
    boundary.setName("missing");
    boundary.setPath("target/does-not-exist/missing.geojson");
    boundary.setType(GeoSpatialBoundaryType.INSIDE);

    GeoSpatialAreaConfigDTO area = new GeoSpatialAreaConfigDTO();
    area.setName("missing-area");
    area.setBoundaries(List.of(boundary));

    GeoSpatialConfigDTO config = new GeoSpatialConfigDTO();
    config.setAreas(List.of(area));

    assertThrows(IOException.class, () -> GeoSpatialConfigLoader.load(config, List.of()));
  }

  private GeoSpatialConfigDTO portugalConfig() throws URISyntaxException {
    GeoSpatialAreaConfigDTO area = new GeoSpatialAreaConfigDTO();
    area.setName("sesimbra-usv");
    area.setBoundaries(
        List.of(
            boundary("offshore", "offshore.geojson"),
            boundary("rio-1", "rio_1.geojson"),
            boundary("rio-2", "rio_2.geojson")));

    GeoSpatialConfigDTO config = new GeoSpatialConfigDTO();
    config.setAreas(List.of(area));
    return config;
  }

  private GeoSpatialBoundaryConfigDTO boundary(String name, String resourceName)
      throws URISyntaxException {
    GeoSpatialBoundaryConfigDTO boundary = new GeoSpatialBoundaryConfigDTO();
    boundary.setName(name);
    boundary.setPath(resourcePath(resourceName).toString());
    boundary.setType(GeoSpatialBoundaryType.INSIDE);
    return boundary;
  }

  private Path resourcePath(String name) throws URISyntaxException {
    return Path.of(
        Objects.requireNonNull(
                getClass().getResource("/geospatial/" + name),
                "Missing test resource: " + name)
            .toURI());
  }

  private DroneInfoDTO drone(String name, String areaName) {
    DroneInfoDTO drone = new DroneInfoDTO();
    drone.setName(name);
    drone.setGeospatialArea(areaName);
    return drone;
  }

  private TwinManagerConfig newTwinManagerConfig(ConfigurationProperties properties)
      throws ReflectiveOperationException {
    Constructor<TwinManagerConfig> constructor =
        TwinManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(properties);
  }
}
