package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransformationConfigFactory {

  public static List<TransformationConfigDTO> loadChain(Object value) {
    if (value == null) {
      return List.of();
    }
    if (value instanceof List<?> list) {
      List<TransformationConfigDTO> results = new ArrayList<>();
      for (Object entry : list) {
        results.add(loadSingle(entry));
      }
      return results;
    }
    return List.of(loadSingle(value));
  }

  public static TransformationConfigDTO loadSingle(Object value) {
    if (value == null) {
      throw new IllegalArgumentException("Transformer config entry is null");
    }

    ConfigurationProperties props = coerceToConfigurationProperties(value);
    String rawType = readTypeToken(props);
    TransformationType type = TransformationType.fromWireName(rawType);

    return switch (type) {
      case JSON_TO_XML -> new JsonToXmlTransformationConfig(props);
      case XML_TO_JSON -> new XmlToJsonTransformationConfig(props);
      case JSON_TO_VALUE -> new JsonToValueTransformationConfig(props);
      case JSON_QUERY -> new JsonQueryTransformationConfig(props);
      case GEOHASH -> new GeoHashResolverTransformationConfig(props);
    };
  }

  private static String readTypeToken(ConfigurationProperties props) {
    String token = props.getProperty("type", null);
    if (token == null) {
      token = props.getProperty("name", null);
    }
    if (token == null) {
      throw new IllegalArgumentException("Transformer missing 'type' (or legacy 'name'): " + props);
    }
    return token.trim().toLowerCase();
  }

  @SuppressWarnings("unchecked")
  private static ConfigurationProperties coerceToConfigurationProperties(Object value) {
    if (value instanceof ConfigurationProperties properties) {
      return properties;
    }
    if (value instanceof Map<?, ?> map) {
      return new ConfigurationProperties((Map<String, Object>) map);
    }
    throw new IllegalArgumentException("Unsupported transformer entry type: " + value.getClass().getName());
  }
}
