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
package io.mapsmessaging.state.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CotConfigSupport {

  private CotConfigSupport() {}

  public static List<CotConfigDTO> parse(Object value) {
    List<CotConfigDTO> configs = new ArrayList<>();
    if (value instanceof ConfigurationProperties properties) {
      configs.add(parseEntry(properties));
    } else if (value instanceof List<?> entries) {
      for (Object entry : entries) {
        if (entry instanceof ConfigurationProperties properties) {
          configs.add(parseEntry(properties));
        }
      }
    }
    return configs;
  }

  public static List<ConfigurationProperties> toConfigurationProperties(List<CotConfigDTO> configs) {
    List<ConfigurationProperties> values = new ArrayList<>();
    if (configs == null) {
      return values;
    }

    for (CotConfigDTO config : configs) {
      if (config == null) {
        continue;
      }
      ConfigurationProperties properties = new ConfigurationProperties();
      properties.put("namespacePath", config.getNamespacePath());
      CotAffiliation affiliation =
          config.getAffiliation() == null ? CotAffiliation.SOURCE : config.getAffiliation();
      properties.put("affiliation", affiliation.name());
      properties.put("how", config.getHow());
      properties.put("staleTimeoutMillis", config.getStaleTimeoutMillis());
      if (config.getUidPrefix() != null) {
        properties.put("uidPrefix", config.getUidPrefix());
      }
      if (config.getDefaultCircularErrorMeters() != null) {
        properties.put("defaultCircularErrorMeters", config.getDefaultCircularErrorMeters());
      }
      if (config.getDefaultLinearErrorMeters() != null) {
        properties.put("defaultLinearErrorMeters", config.getDefaultLinearErrorMeters());
      }
      properties.put("altitudeSource", config.getAltitudeSource());
      values.add(properties);
    }
    return values;
  }

  private static CotConfigDTO parseEntry(ConfigurationProperties properties) {
    CotConfigDTO config = new CotConfigDTO();
    config.setNamespacePath(properties.getProperty("namespacePath", null));
    config.setAffiliation(parseAffiliation(properties.getProperty("affiliation", null), config.getAffiliation()));
    config.setHow(properties.getProperty("how", config.getHow()));
    config.setStaleTimeoutMillis(properties.getLongProperty("staleTimeoutMillis", config.getStaleTimeoutMillis()));
    config.setUidPrefix(properties.getProperty("uidPrefix", null));
    if (properties.containsKey("defaultCircularErrorMeters")) {
      config.setDefaultCircularErrorMeters(properties.getDoubleProperty("defaultCircularErrorMeters", config.getDefaultCircularErrorMeters()));
    }
    if (properties.containsKey("defaultLinearErrorMeters")) {
      config.setDefaultLinearErrorMeters(properties.getDoubleProperty("defaultLinearErrorMeters", config.getDefaultLinearErrorMeters()));
    }
    config.setAltitudeSource(properties.getProperty("altitudeSource", config.getAltitudeSource()));
    validate(config);
    return config;
  }

  private static CotAffiliation parseAffiliation(String value, CotAffiliation defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    String normalised = value.trim().toUpperCase(Locale.ROOT);
    if ("ENEMY".equals(normalised)) {
      return CotAffiliation.HOSTILE;
    }
    return CotAffiliation.valueOf(normalised);
  }

  private static void validate(CotConfigDTO config) {
    if (config.getNamespacePath() == null || config.getNamespacePath().isBlank()) {
      throw new IllegalArgumentException("CoT namespacePath must not be blank");
    }
    if (config.getStaleTimeoutMillis() < 1) {
      throw new IllegalArgumentException("CoT staleTimeoutMillis must be positive");
    }
    validateError(config.getDefaultCircularErrorMeters(), "defaultCircularErrorMeters");
    validateError(config.getDefaultLinearErrorMeters(), "defaultLinearErrorMeters");
  }

  private static void validateError(Double value, String name) {
    if (value != null && (!Double.isFinite(value) || value < 0.0d)) {
      throw new IllegalArgumentException("CoT " + name + " must be finite and non-negative");
    }
  }
}
