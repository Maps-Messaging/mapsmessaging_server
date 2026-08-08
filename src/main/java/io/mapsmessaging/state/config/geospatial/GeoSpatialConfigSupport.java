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

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.geospatial.GeoSpatialBoundaryType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GeoSpatialConfigSupport {

  private static final String AREAS_KEY = "areas";
  private static final String BOUNDARIES_KEY = "boundaries";

  private GeoSpatialConfigSupport() {}

  public static GeoSpatialConfigDTO parse(Object value) {
    GeoSpatialConfigDTO config = new GeoSpatialConfigDTO();
    ConfigurationProperties properties = asConfigurationProperties(value);
    if (properties != null) {
      config.setAreas(parseAreas(properties.get(AREAS_KEY)));
    }
    return config;
  }

  public static ConfigurationProperties toConfigurationProperties(GeoSpatialConfigDTO config) {
    ConfigurationProperties properties = new ConfigurationProperties();
    if (config == null || config.getAreas() == null || config.getAreas().isEmpty()) {
      return properties;
    }

    List<ConfigurationProperties> areas = new ArrayList<>();
    for (GeoSpatialAreaConfigDTO area : config.getAreas()) {
      ConfigurationProperties areaProperties = new ConfigurationProperties();
      areaProperties.put("name", area.getName());

      List<ConfigurationProperties> boundaries = new ArrayList<>();
      if (area.getBoundaries() != null) {
        for (GeoSpatialBoundaryConfigDTO boundary : area.getBoundaries()) {
          ConfigurationProperties boundaryProperties = new ConfigurationProperties();
          boundaryProperties.put("name", boundary.getName());
          boundaryProperties.put("path", boundary.getPath());
          if (boundary.getType() != null) {
            boundaryProperties.put("type", boundary.getType().name());
          }
          boundaries.add(boundaryProperties);
        }
      }
      areaProperties.put(BOUNDARIES_KEY, boundaries);
      areas.add(areaProperties);
    }
    properties.put(AREAS_KEY, areas);
    return properties;
  }

  private static List<GeoSpatialAreaConfigDTO> parseAreas(Object value) {
    List<GeoSpatialAreaConfigDTO> areas = new ArrayList<>();
    ConfigurationProperties singleArea = asConfigurationProperties(value);
    if (singleArea != null) {
      areas.add(parseArea(singleArea));
      return areas;
    }
    if (value instanceof List<?> entries) {
      for (Object entry : entries) {
        ConfigurationProperties properties = asConfigurationProperties(entry);
        if (properties != null) {
          areas.add(parseArea(properties));
        }
      }
    }
    return areas;
  }

  private static GeoSpatialAreaConfigDTO parseArea(ConfigurationProperties properties) {
    GeoSpatialAreaConfigDTO area = new GeoSpatialAreaConfigDTO();
    area.setName(properties.getProperty("name", null));
    area.setBoundaries(parseBoundaries(properties.get(BOUNDARIES_KEY)));
    return area;
  }

  private static List<GeoSpatialBoundaryConfigDTO> parseBoundaries(Object value) {
    List<GeoSpatialBoundaryConfigDTO> boundaries = new ArrayList<>();
    ConfigurationProperties singleBoundary = asConfigurationProperties(value);
    if (singleBoundary != null) {
      boundaries.add(parseBoundary(singleBoundary));
      return boundaries;
    }
    if (value instanceof List<?> entries) {
      for (Object entry : entries) {
        ConfigurationProperties properties = asConfigurationProperties(entry);
        if (properties != null) {
          boundaries.add(parseBoundary(properties));
        }
      }
    }
    return boundaries;
  }

  private static GeoSpatialBoundaryConfigDTO parseBoundary(ConfigurationProperties properties) {
    GeoSpatialBoundaryConfigDTO boundary = new GeoSpatialBoundaryConfigDTO();
    boundary.setName(properties.getProperty("name", null));
    boundary.setPath(properties.getProperty("path", null));

    String configuredType = properties.getProperty("type", null);
    if (configuredType != null && !configuredType.isBlank()) {
      boundary.setType(GeoSpatialBoundaryType.valueOf(configuredType.trim().toUpperCase(Locale.ROOT)));
    }
    return boundary;
  }

  private static ConfigurationProperties asConfigurationProperties(Object value) {
    if (value instanceof ConfigurationProperties properties) {
      return properties;
    }
    if (!(value instanceof Map<?, ?> map)) {
      return null;
    }

    ConfigurationProperties properties = new ConfigurationProperties();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (entry.getKey() instanceof String key) {
        properties.put(key, entry.getValue());
      }
    }
    return properties;
  }
}
