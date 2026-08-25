/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class DataProductConfigLoader {

  private DataProductConfigLoader() {}

  static List<DataProductConfig> load(String droneName, UUID droneUuid) {
    ConfigurationProperties root =
        ConfigurationManager.getInstance().getProperties("TwinManager");
    if (root == null) {
      return List.of();
    }
    Object droneInfo = root.get("droneInfo");
    if (droneInfo instanceof List<?> entries) {
      for (Object entry : entries) {
        if (entry instanceof ConfigurationProperties properties
            && matches(properties, droneName, droneUuid)) {
          return parseProducts(properties.get("data_products"));
        }
      }
    } else if (droneInfo instanceof ConfigurationProperties properties
        && matches(properties, droneName, droneUuid)) {
      return parseProducts(properties.get("data_products"));
    }
    return List.of();
  }

  private static boolean matches(
      ConfigurationProperties properties, String droneName, UUID droneUuid) {
    String configuredName = properties.getProperty("name", null);
    String configuredUuid = properties.getProperty("uuid", null);
    return (droneName != null && Objects.equals(droneName, configuredName))
        || (droneUuid != null && Objects.equals(droneUuid.toString(), configuredUuid));
  }

  static List<DataProductConfig> parseProducts(Object value) {
    if (value == null) {
      return List.of();
    }
    List<DataProductConfig> products = new ArrayList<>();
    if (value instanceof List<?> entries) {
      for (Object entry : entries) {
        if (entry instanceof ConfigurationProperties properties) {
          products.add(parseProduct(properties));
        }
      }
    } else if (value instanceof ConfigurationProperties properties) {
      products.add(parseProduct(properties));
    }
    return List.copyOf(products);
  }

  private static DataProductConfig parseProduct(ConfigurationProperties properties) {
    DataProductConfig product = new DataProductConfig();
    product.setIdentifier(properties.getProperty("identifier", null));
    product.setDescription(properties.getProperty("description", null));
    product.setUri(properties.getProperty("uri", null));
    product.setProductType(parseMap(properties.get("product_type")));
    product.setConformsTo(parseMap(properties.get("conforms_to")));
    return product;
  }

  private static Map<String, Object> parseMap(Object value) {
    if (value instanceof ConfigurationProperties properties) {
      return new LinkedHashMap<>(properties.getMap());
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (entry.getKey() instanceof String key) {
          result.put(key, entry.getValue());
        }
      }
      return result;
    }
    return Map.of();
  }
}
