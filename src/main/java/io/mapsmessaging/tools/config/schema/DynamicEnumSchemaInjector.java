package io.mapsmessaging.tools.config.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DynamicEnumSchemaInjector {

  private DynamicEnumSchemaInjector() {
  }

  public static boolean injectProtocolTransformationEnumIfApplicable(
      String fieldName,
      Class<?> fieldType,
      Map<String, Object> propertySchemaNode
  ) {
    if (fieldName == null) {
      return false;
    }
    if (fieldType == null) {
      return false;
    }
    if (propertySchemaNode == null) {
      return false;
    }

    boolean isStringField = String.class.equals(fieldType);
    if (!isStringField) {
      return false;
    }

    if (!"linkTransformation".equals(fieldName)) {
      return false;
    }

    List<String> allowedNames = ProtocolTransformationNameRegistry.getAllowedNames(true);

    // Force a tight schema: string + enum (+ default if you want it here too)
    propertySchemaNode.put("type", "string");
    propertySchemaNode.put("enum", allowedNames);

    if (!propertySchemaNode.containsKey("default")) {
      propertySchemaNode.put("default", "");
    }

    // If your pipeline supports OpenAPI-style nullable, you can keep it,
    // but for config sanity I'd recommend nullable=false when "" is allowed.
    // propertySchemaNode.put("nullable", false);

    return true;
  }

  public static Map<String, Object> newEmptyPropertyNode() {
    return new LinkedHashMap<>();
  }
}
