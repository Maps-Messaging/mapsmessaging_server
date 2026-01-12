package io.mapsmessaging.tools.config.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class JsonSchemaValueCoercion {

  private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

  private JsonSchemaValueCoercion() {
  }

  public static JsonNode coerceValue(String rawValue, SchemaScalarType schemaScalarType) {
    if (rawValue == null) {
      return NullNode.getInstance();
    }

    String trimmedValue = rawValue.trim();
    if (trimmedValue.isEmpty() || "null".equalsIgnoreCase(trimmedValue)) {
      return NullNode.getInstance();
    }

    switch (schemaScalarType) {
      case INTEGER:
        return NODE_FACTORY.numberNode(parseLong(trimmedValue));
      case NUMBER:
        return NODE_FACTORY.numberNode(parseBigDecimal(trimmedValue));
      case BOOLEAN:
        return NODE_FACTORY.booleanNode(parseBoolean(trimmedValue));
      default:
        return NODE_FACTORY.textNode(rawValue);
    }
  }

  public static List<JsonNode> coerceEnumValues(List<String> enumValues, SchemaScalarType schemaScalarType) {
    if (enumValues == null || enumValues.isEmpty()) {
      return List.of();
    }

    List<JsonNode> coercedValues = new ArrayList<>(enumValues.size());
    for (String enumValue : enumValues) {
      coercedValues.add(coerceValue(enumValue, schemaScalarType));
    }
    return coercedValues;
  }

  private static long parseLong(String rawValue) {
    String normalizedValue = rawValue.replace("_", "");
    if (normalizedValue.startsWith("0x") || normalizedValue.startsWith("0X")) {
      return Long.parseLong(normalizedValue.substring(2), 16);
    }
    return Long.parseLong(normalizedValue);
  }

  private static BigDecimal parseBigDecimal(String rawValue) {
    String normalizedValue = rawValue.replace("_", "");
    // BigDecimal handles 863, 868.0, 1e-3, etc.
    return new BigDecimal(normalizedValue);
  }

  private static boolean parseBoolean(String rawValue) {
    if ("true".equalsIgnoreCase(rawValue) || "false".equalsIgnoreCase(rawValue)) {
      return Boolean.parseBoolean(rawValue);
    }
    throw new IllegalArgumentException("Invalid boolean value: " + rawValue);
  }

  public enum SchemaScalarType {
    STRING,
    INTEGER,
    NUMBER,
    BOOLEAN
  }
}
