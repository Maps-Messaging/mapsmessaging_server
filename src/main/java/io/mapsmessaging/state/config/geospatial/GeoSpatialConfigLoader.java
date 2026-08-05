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

import io.mapsmessaging.MapsEnvironment;
import io.mapsmessaging.geospatial.GeoJsonBoundaryLoader;
import io.mapsmessaging.geospatial.GeoSpatialArea;
import io.mapsmessaging.geospatial.GeoSpatialAreaRegistry;
import io.mapsmessaging.geospatial.GeoSpatialBoundary;
import io.mapsmessaging.geospatial.GeoSpatialBoundaryType;
import io.mapsmessaging.state.config.DroneInfoDTO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class GeoSpatialConfigLoader {

  private GeoSpatialConfigLoader() {}

  public static GeoSpatialAreaRegistry load(
      GeoSpatialConfigDTO config, Collection<DroneInfoDTO> droneInfos) throws IOException {
    return load(
        config,
        droneInfos,
        Path.of(MapsEnvironment.getMapsHome(), "conf"));
  }

  public static GeoSpatialAreaRegistry load(
      GeoSpatialConfigDTO config,
      Collection<DroneInfoDTO> droneInfos,
      Path configurationDirectory)
      throws IOException {
    Objects.requireNonNull(config, "config must not be null");
    Objects.requireNonNull(configurationDirectory, "configurationDirectory must not be null");
    List<GeoSpatialAreaConfigDTO> configuredAreas = config.getAreas();
    if (configuredAreas == null || configuredAreas.isEmpty()) {
      throw new IllegalArgumentException("geospatial configuration must contain at least one area");
    }

    GeoSpatialAreaRegistry.Builder registryBuilder = GeoSpatialAreaRegistry.builder();
    for (GeoSpatialAreaConfigDTO configuredArea : configuredAreas) {
      registryBuilder.add(loadArea(configuredArea, configurationDirectory));
    }

    GeoSpatialAreaRegistry registry = registryBuilder.build();
    validateDroneAssignments(registry, droneInfos);
    return registry;
  }

  private static GeoSpatialArea loadArea(
      GeoSpatialAreaConfigDTO configuredArea, Path configurationDirectory)
      throws IOException {
    Objects.requireNonNull(configuredArea, "configured area must not be null");
    String areaName = requireText(configuredArea.getName(), "geospatial area name");
    List<GeoSpatialBoundaryConfigDTO> configuredBoundaries = configuredArea.getBoundaries();
    if (configuredBoundaries == null || configuredBoundaries.isEmpty()) {
      throw new IllegalArgumentException(
          "geospatial area '" + areaName + "' must contain at least one boundary");
    }

    GeoSpatialArea.Builder areaBuilder = GeoSpatialArea.builder(areaName);
    for (GeoSpatialBoundaryConfigDTO configuredBoundary : configuredBoundaries) {
      areaBuilder.add(loadBoundary(areaName, configuredBoundary, configurationDirectory));
    }
    return areaBuilder.build();
  }

  private static GeoSpatialBoundary loadBoundary(
      String areaName,
      GeoSpatialBoundaryConfigDTO configuredBoundary,
      Path configurationDirectory)
      throws IOException {
    Objects.requireNonNull(configuredBoundary, "configured boundary must not be null");
    String pathValue =
        requireText(configuredBoundary.getPath(), "boundary path for area '" + areaName + "'");
    GeoSpatialBoundaryType type =
        configuredBoundary.getType() == null
            ? GeoSpatialBoundaryType.INSIDE
            : configuredBoundary.getType();

    Path configuredPath = Path.of(pathValue);
    Path resolvedPath =
        configuredPath.isAbsolute()
            ? configuredPath.normalize()
            : configurationDirectory.resolve(configuredPath).normalize();
    if (!Files.isRegularFile(resolvedPath)) {
      throw new NoSuchFileException(resolvedPath.toString());
    }

    String configuredName = configuredBoundary.getName();
    if (configuredName == null || configuredName.isBlank()) {
      return GeoJsonBoundaryLoader.load(resolvedPath, type);
    }
    return GeoJsonBoundaryLoader.load(configuredName.trim(), resolvedPath, type);
  }

  private static void validateDroneAssignments(
      GeoSpatialAreaRegistry registry, Collection<DroneInfoDTO> droneInfos) {
    if (droneInfos == null) {
      return;
    }

    for (DroneInfoDTO droneInfo : droneInfos) {
      if (droneInfo == null
          || droneInfo.getGeospatialArea() == null
          || droneInfo.getGeospatialArea().isBlank()) {
        continue;
      }

      String areaName = droneInfo.getGeospatialArea().trim();
      if (registry.find(areaName).isEmpty()) {
        throw new IllegalArgumentException(
            "Drone '"
                + droneInfo.getName()
                + "' references unknown geospatial area '"
                + areaName
                + "'");
      }
      droneInfo.setGeospatialArea(areaName);
    }
  }

  private static String requireText(String value, String description) {
    Objects.requireNonNull(value, description + " must not be null");
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(description + " must not be blank");
    }
    return trimmed;
  }
}
