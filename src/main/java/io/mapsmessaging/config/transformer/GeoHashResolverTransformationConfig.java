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
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.mapsmessaging.dto.rest.config.transformer.impl.GeoHashResolverTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashLayout;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashOnMissingPolicy;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashUnits;

import java.util.ArrayList;
import java.util.List;

public class GeoHashResolverTransformationConfig extends GeoHashResolverTransformationDTO {

  public GeoHashResolverTransformationConfig(ConfigurationProperties props) {
    setType(TransformationType.GEOHASH);

    ConfigurationProperties parameters = unwrapParameters(props);

    setPrefix(parameters.getProperty("prefix", ""));
    setLatKey(parameters.getProperty("latKey", "latitude"));
    setLonKey(parameters.getProperty("lonKey", "longitude"));
    setPrecision(parameters.getIntProperty("precision", 5));

    List<String> latKeys = parseCsv(parameters.getProperty("latKeys", ""));
    List<String> lonKeys = parseCsv(parameters.getProperty("lonKeys", ""));
    setLatKeys(latKeys);
    setLonKeys(lonKeys);

    setUnits(parseUnits(parameters.getProperty("units", "deg")));
    setLayout(parseLayout(parameters.getProperty("layout", "chars-per-segment")));
    setOnMissing(parseOnMissing(parameters.getProperty("onMissing", "skip")));

    if (getOnMissing() == GeoHashOnMissingPolicy.DEFAULT_TO) {
      // legacy supported "defaultTo" = "lat,lon"
      String defaultTo = parameters.getProperty("defaultTo", null);
      if (defaultTo != null) {
        double[] pair = parseLatLonPair(defaultTo);
        if (pair != null) {
          setDefaultLatitude(pair[0]);
          setDefaultLongitude(pair[1]);
        }
      } else {
        // new style fields (if present)
        if (parameters.containsKey("defaultLatitude")) {
          setDefaultLatitude(parameters.getDoubleProperty("defaultLatitude", 0.0));
        }
        if (parameters.containsKey("defaultLongitude")) {
          setDefaultLongitude(parameters.getDoubleProperty("defaultLongitude", 0.0));
        }
      }
    }
  }

  private ConfigurationProperties unwrapParameters(ConfigurationProperties legacy) {
    Object raw = legacy.get("parameters");
    if (raw instanceof ConfigurationProperties properties) {
      return properties;
    }
    return legacy;
  }

  private List<String> parseCsv(String value) {
    if (value == null || value.isBlank()) {
      return new ArrayList<>();
    }
    List<String> list = new ArrayList<>();
    String[] parts = value.split(",");
    for (String part : parts) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        list.add(trimmed);
      }
    }
    return list;
  }

  private GeoHashUnits parseUnits(String value) {
    if (value == null) {
      return GeoHashUnits.DEG;
    }
    String normalized = value.trim().toLowerCase();
    return switch (normalized) {
      case "deg" -> GeoHashUnits.DEG;
      case "rad" -> GeoHashUnits.RAD;
      case "e7" -> GeoHashUnits.E7;
      case "micros" -> GeoHashUnits.MICROS;
      default -> GeoHashUnits.DEG;
    };
  }

  private GeoHashLayout parseLayout(String value) {
    if (value == null) {
      return GeoHashLayout.CHARS_PER_SEGMENT;
    }
    String normalized = value.trim().toLowerCase();
    return switch (normalized) {
      case "raw" -> GeoHashLayout.RAW;
      case "two-per-segment" -> GeoHashLayout.TWO_PER_SEGMENT;
      case "chars-per-segment" -> GeoHashLayout.CHARS_PER_SEGMENT;
      default -> GeoHashLayout.CHARS_PER_SEGMENT;
    };
  }

  private GeoHashOnMissingPolicy parseOnMissing(String value) {
    if (value == null) {
      return GeoHashOnMissingPolicy.SKIP;
    }
    String normalized = value.trim().toLowerCase();
    return switch (normalized) {
      case "drop" -> GeoHashOnMissingPolicy.DROP;
      case "skip" -> GeoHashOnMissingPolicy.SKIP;
      case "defaultto", "default-to", "default_to" -> GeoHashOnMissingPolicy.DEFAULT_TO;
      default -> GeoHashOnMissingPolicy.SKIP;
    };
  }

  private double[] parseLatLonPair(String text) {
    String[] parts = text.split(",");
    if (parts.length != 2) {
      return null;
    }
    try {
      double lat = Double.parseDouble(parts[0].trim());
      double lon = Double.parseDouble(parts[1].trim());
      return new double[] {lat, lon};
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
