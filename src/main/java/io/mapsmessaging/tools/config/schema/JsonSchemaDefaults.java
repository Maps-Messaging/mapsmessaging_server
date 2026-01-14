package io.mapsmessaging.tools.config.schema;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Map;

public final class JsonSchemaDefaults {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JsonSchemaDefaults() {
  }

  public static Object coerceDefaultValue(String rawDefault, SchemaObject propertySchema) {
    if (rawDefault == null) {
      return null;
    }

    String raw = rawDefault.trim();

    Object typeObj = propertySchema.get("type");
    if (typeObj == null) {
      return rawDefault;
    }

    if (typeObj instanceof String) {
      return coerceBySingleType(raw, (String) typeObj, rawDefault);
    }

    // If you ever emit type as List (e.g. ["string","null"]) handle that later.
    // For now: keep as string to avoid producing invalid schema.
    return rawDefault;
  }

  private static Object coerceBySingleType(String raw, String schemaType, String rawDefault) {
    String type = schemaType.toLowerCase(Locale.ROOT);

    try {
      switch (type) {
        case "boolean":
          if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) {
            return Boolean.parseBoolean(raw);
          }
          return null;

        case "integer":
          return Long.parseLong(raw.replace("_", ""));

        case "number":
          return Double.parseDouble(raw.replace("_", ""));

        case "array":
          if (raw.startsWith("[")) {
            return OBJECT_MAPPER.readValue(raw, new TypeReference<Object>() {});
          }
          return null;

        case "object":
          if (raw.startsWith("{")) {
            return OBJECT_MAPPER.readValue(raw, new TypeReference<Map<String, Object>>() {});
          }
          return null;

        case "string":
        default:
          return rawDefault;
      }
    }
    catch (Exception e) {
      return null;
    }
  }
}
