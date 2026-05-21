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

package io.mapsmessaging.tools.config.yaml;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

public final class YamlNodeRenderer {
  private static final String LIST_SEPARATOR = "# ----------------------------------------------------------------------";
  private static final String VERSION_FIELD = "schemaLoadingVersion";

  private final SchemaResolver schemaResolver;
  private final SchemaIntrospector schemaIntrospector;
  private final YamlCommentEmitter yamlCommentEmitter;
  private final YamlValueFormatter yamlValueFormatter;

  public YamlNodeRenderer(
      SchemaResolver schemaResolver,
      SchemaIntrospector schemaIntrospector,
      YamlCommentEmitter yamlCommentEmitter,
      YamlValueFormatter yamlValueFormatter
  ) {
    this.schemaResolver = schemaResolver;
    this.schemaIntrospector = schemaIntrospector;
    this.yamlCommentEmitter = yamlCommentEmitter;
    this.yamlValueFormatter = yamlValueFormatter;
  }

  public String renderRoot(String rootKey, Map<String, Object> rootValue, JsonObject rootSchema, RenderMode renderMode) {
    StringBuilder builder = new StringBuilder(8192);
    builder.append(rootKey).append(":").append("\n");

    JsonElement resolved = schemaResolver.resolve(rootSchema, rootSchema);
    JsonObject resolvedObjectSchema = schemaResolver.coerceToObjectSchema(resolved.getAsJsonObject(), rootSchema);

    renderObject(builder, 1, resolvedObjectSchema, rootValue, rootSchema, renderMode, true);
    return builder.toString();
  }

  private void renderObject(
      StringBuilder builder,
      int indentLevel,
      JsonObject objectSchema,
      Map<String, Object> objectValue,
      JsonObject schemaRoot,
      RenderMode renderMode,
      boolean isRootObject
  ) {
    JsonObject propertiesObject = getObject(objectSchema, "properties");
    Set<String> requiredFields = getStringSet(objectSchema, "required");

    if (propertiesObject == null) {
      return;
    }

    boolean firstEmitted = true;

    for (Map.Entry<String, JsonElement> entry : propertiesObject.entrySet()) {
      String propertyName = entry.getKey();

      if (!isRootObject && VERSION_FIELD.equals(propertyName)) {
        continue;
      }

      JsonElement resolvedPropertySchemaElement = schemaResolver.resolve(entry.getValue(), schemaRoot);
      if (resolvedPropertySchemaElement == null || !resolvedPropertySchemaElement.isJsonObject()) {
        continue;
      }

      JsonObject propertySchema = resolvedPropertySchemaElement.getAsJsonObject();

      boolean isRequired = requiredFields.contains(propertyName);
      Object rawValue = objectValue != null ? objectValue.get(propertyName) : null;
      boolean hasMeaningfulValue = isMeaningfulValue(rawValue);
      boolean hasDefault = propertySchema.has("default");

      if (renderMode == RenderMode.MINIMAL && !hasMeaningfulValue && !isRequired && !hasDefault) {
        continue;
      }

      Object value = rawValue;
      if (!hasMeaningfulValue && hasDefault) {
        value = JsonElementConverter.toJava(propertySchema.get("default"));
      }

      // Optional: blank line between sibling keys at top level of an object
      if (!firstEmitted) {
        builder.append("\n");
      }
      firstEmitted = false;

      writeCommentBlock(builder, indentLevel, propertySchema);

      String indent = indent(indentLevel);
      builder.append(indent).append(propertyName).append(":");

      String type = schemaResolver.getType(propertySchema);

      if (isObjectType(type, propertySchema)) {
        Map<String, Object> childMap = castToMap(value);

        if (renderMode == RenderMode.MINIMAL && (childMap == null || childMap.isEmpty()) && !isRequired && !hasDefault) {
          builder.append(" {}\n");
          continue;
        }

        builder.append("\n");

        if (childMap == null) {
          childMap = new LinkedHashMap<>();
        }

        JsonObject childSchema = schemaResolver.coerceToObjectSchema(propertySchema, schemaRoot);
        renderObject(builder, indentLevel + 1, childSchema, childMap, schemaRoot, renderMode, false);
        continue;
      }

      if ("array".equals(type)) {
        renderArray(builder, indentLevel, propertySchema, value, schemaRoot, renderMode);
        continue;
      }

      if (value instanceof Map<?, ?>) {
        Map<String, Object> mapValue = castToMap(value);
        if (mapValue == null || mapValue.isEmpty()) {
          builder.append(" {}\n");
          continue;
        }
      }

      builder.append(" ").append(yamlValueFormatter.formatScalar(value)).append("\n");
    }
  }

  private void renderArray(
      StringBuilder builder,
      int indentLevel,
      JsonObject arraySchema,
      Object arrayValue,
      JsonObject schemaRoot,
      RenderMode renderMode
  ) {
    List<Object> list = castToList(arrayValue);

    if (list == null || list.isEmpty()) {
      if (renderMode == RenderMode.FULL && itemsIsObject(arraySchema)) {
        builder.append("\n");
        builder.append(indent(indentLevel + 1)).append(LIST_SEPARATOR).append("\n");
        builder.append(indent(indentLevel + 1)).append("-").append("\n");

        JsonObject itemSchema = schemaResolver.resolve(arraySchema.get("items"), schemaRoot).getAsJsonObject();
        JsonObject itemObjectSchema = schemaResolver.coerceToObjectSchema(itemSchema, schemaRoot);

        renderObject(builder, indentLevel + 2, itemObjectSchema, new LinkedHashMap<>(), schemaRoot, renderMode, false);
        return;
      }

      builder.append(" []\n");
      return;
    }

    builder.append("\n");

    JsonObject itemsSchema = null;
    if (arraySchema.has("items")) {
      JsonElement resolvedItems = schemaResolver.resolve(arraySchema.get("items"), schemaRoot);
      if (resolvedItems != null && resolvedItems.isJsonObject()) {
        itemsSchema = resolvedItems.getAsJsonObject();
      }
    }

    String itemsType = schemaResolver.getType(itemsSchema);
    boolean firstItem = true;
    for (Object item : list) {
      if (!firstItem) {
        builder.append("\n");
      }
      firstItem = false;
      builder.append(indent(indentLevel + 1)).append(LIST_SEPARATOR).append("\n");
      builder.append(indent(indentLevel + 1)).append("-");

      if (itemsSchema != null && isObjectType(itemsType, itemsSchema)) {
        builder.append("\n");
        Map<String, Object> itemMap = castToMap(item);
        if (itemMap == null) {
          itemMap = new LinkedHashMap<>();
        }

        JsonObject itemObjectSchema = schemaResolver.coerceToObjectSchema(itemsSchema, schemaRoot);
        renderObject(builder, indentLevel + 2, itemObjectSchema, itemMap, schemaRoot, renderMode, false);
      } else {
        builder.append(" ").append(yamlValueFormatter.formatScalar(item)).append("\n");
      }
    }
  }

  private void writeCommentBlock(StringBuilder builder, int indentLevel, JsonObject propertySchema) {
    SchemaDoc schemaDoc = schemaIntrospector.extract(propertySchema);
    List<String> commentLines = yamlCommentEmitter.buildCommentLines(schemaDoc);

    if (commentLines.isEmpty()) {
      return;
    }

    String indent = indent(indentLevel);
    for (String line : commentLines) {
      builder.append(indent).append("# ").append(line).append("\n");
    }
  }

  private boolean itemsIsObject(JsonObject arraySchema) {
    if (arraySchema == null) {
      return false;
    }
    if (!arraySchema.has("items")) {
      return false;
    }
    JsonElement itemsElement = arraySchema.get("items");
    if (itemsElement == null || !itemsElement.isJsonObject()) {
      return false;
    }
    JsonObject itemsObject = itemsElement.getAsJsonObject();
    String type = schemaResolver.getType(itemsObject);
    return isObjectType(type, itemsObject);
  }

  private boolean isObjectType(String type, JsonObject schema) {
    if ("object".equals(type)) {
      return true;
    }
    return type == null && schema != null && schema.has("properties");
  }

  private boolean isMeaningfulValue(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof String stringValue) {
      return !stringValue.isBlank();
    }
    if (value instanceof Map<?, ?> rawMap) {
      return !rawMap.isEmpty();
    }
    if (value instanceof Collection<?> collectionValue) {
      return !collectionValue.isEmpty();
    }
    return true;
  }

  private String indent(int indentLevel) {
    return "  ".repeat(Math.max(0, indentLevel));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> castToMap(Object value) {
    if (value instanceof Map<?, ?> rawMap) {
      Map<String, Object> map = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
        map.put(String.valueOf(entry.getKey()), entry.getValue());
      }
      return map;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private List<Object> castToList(Object value) {
    if (value instanceof List<?> rawList) {
      return (List<Object>) rawList;
    }
    if (value instanceof Iterable<?> iterable) {
      List<Object> list = new ArrayList<>();
      for (Object item : iterable) {
        list.add(item);
      }
      return list;
    }
    return null;
  }

  private JsonObject getObject(JsonObject parent, String key) {
    if (parent == null || !parent.has(key)) {
      return null;
    }
    JsonElement element = parent.get(key);
    if (element == null || !element.isJsonObject()) {
      return null;
    }
    return element.getAsJsonObject();
  }

  private Set<String> getStringSet(JsonObject parent, String key) {
    if (parent == null || !parent.has(key)) {
      return Collections.emptySet();
    }
    JsonElement element = parent.get(key);
    if (element == null || !element.isJsonArray()) {
      return Collections.emptySet();
    }

    Set<String> values = new LinkedHashSet<>();
    JsonArray array = element.getAsJsonArray();
    for (JsonElement item : array) {
      if (item != null && item.isJsonPrimitive()) {
        values.add(item.getAsString());
      }
    }
    return values;
  }
}
