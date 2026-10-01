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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class SchemaResolver {

  private static final String ALL_OF_KEY = "allOf";
  private static final String ANY_OF_KEY = "anyOf";
  private static final String ONE_OF_KEY = "oneOf";
  private static final String PROPERTIES_KEY = "properties";
  private static final String REQUIRED_KEY = "required";


  public JsonElement resolve(JsonElement schemaElement, JsonObject schemaRoot) {
    if (schemaElement == null || schemaElement.isJsonNull()) {
      return schemaElement;
    }

    if (!schemaElement.isJsonObject()) {
      return schemaElement;
    }

    JsonObject schemaObject = schemaElement.getAsJsonObject();

    if (schemaObject.has("$ref")) {
      String reference = schemaObject.get("$ref").getAsString();
      JsonElement resolved = resolveRef(schemaRoot, reference);
      if (resolved != null) {
        return resolve(resolved, schemaRoot);
      }
    }

    if (schemaObject.has(ALL_OF_KEY) && schemaObject.get(ALL_OF_KEY).isJsonArray()) {
      return mergeAllOf(schemaObject, schemaRoot);
    }

    if (schemaObject.has(ONE_OF_KEY) && schemaObject.get(ONE_OF_KEY).isJsonArray()) {
      JsonArray oneOfArray = schemaObject.getAsJsonArray(ONE_OF_KEY);
      if (!oneOfArray.isEmpty()) {
        return resolve(oneOfArray.get(0), schemaRoot);
      }
    }

    if (schemaObject.has(ANY_OF_KEY) && schemaObject.get(ANY_OF_KEY).isJsonArray()) {
      JsonArray anyOfArray = schemaObject.getAsJsonArray(ANY_OF_KEY);
      if (!anyOfArray.isEmpty()) {
        return resolve(anyOfArray.get(0), schemaRoot);
      }
    }

    return schemaObject;
  }

  public JsonObject coerceToObjectSchema(JsonObject schema, JsonObject schemaRoot) {
    JsonObject resolvedSchema = schema;
    String resolvedType = getType(resolvedSchema);

    if ("object".equals(resolvedType) || (resolvedSchema != null && resolvedSchema.has(PROPERTIES_KEY))) {
      return resolvedSchema;
    }

    JsonObject objectSchema = new JsonObject();
    objectSchema.addProperty("type", "object");
    objectSchema.add(PROPERTIES_KEY, new JsonObject());
    return objectSchema;
  }

  public String getType(JsonObject schema) {
    if (schema == null) {
      return null;
    }
    if (!schema.has("type")) {
      return null;
    }
    JsonElement typeElement = schema.get("type");
    if (typeElement == null || typeElement.isJsonNull()) {
      return null;
    }
    if (!typeElement.isJsonPrimitive()) {
      return null;
    }
    return typeElement.getAsString();
  }

  private JsonObject mergeAllOf(JsonObject schemaObject, JsonObject schemaRoot) {
    JsonObject mergedSchema = new JsonObject();

    for (Map.Entry<String, JsonElement> entry : schemaObject.entrySet()) {
      if (!entry.getKey().equals(ALL_OF_KEY)) {
        mergedSchema.add(entry.getKey(), entry.getValue());
      }
    }

    JsonArray allOfArray = schemaObject.getAsJsonArray(ALL_OF_KEY);
    for (JsonElement element : allOfArray) {
      JsonElement resolvedElement = resolve(element, schemaRoot);
      if (resolvedElement != null && resolvedElement.isJsonObject()) {
        shallowMerge(mergedSchema, resolvedElement.getAsJsonObject());
      }
    }

    return mergedSchema;
  }

  private void shallowMerge(JsonObject target, JsonObject source) {
    mergeProperties(target, source);
    mergeRequired(target, source);
    mergeMissingSimpleKeys(target, source);
  }

  private void mergeProperties(JsonObject target, JsonObject source) {
    JsonObject targetProperties = target.has(PROPERTIES_KEY) && target.get(PROPERTIES_KEY).isJsonObject()
        ? target.getAsJsonObject(PROPERTIES_KEY)
        : null;

    JsonObject sourceProperties = source.has(PROPERTIES_KEY) && source.get(PROPERTIES_KEY).isJsonObject()
        ? source.getAsJsonObject(PROPERTIES_KEY)
        : null;

    if (sourceProperties == null) {
      return;
    }

    if (targetProperties == null) {
      target.add(PROPERTIES_KEY, sourceProperties);
      return;
    }

    for (Map.Entry<String, JsonElement> entry : sourceProperties.entrySet()) {
      if (!targetProperties.has(entry.getKey())) {
        targetProperties.add(entry.getKey(), entry.getValue());
      }
    }
  }

  private void mergeRequired(JsonObject target, JsonObject source) {
    JsonArray sourceRequired = source.has(REQUIRED_KEY) && source.get(REQUIRED_KEY).isJsonArray()
        ? source.getAsJsonArray(REQUIRED_KEY)
        : null;

    if (sourceRequired == null) {
      return;
    }

    Set<String> combined = new LinkedHashSet<>();

    if (target.has(REQUIRED_KEY) && target.get(REQUIRED_KEY).isJsonArray()) {
      JsonArray targetRequired = target.getAsJsonArray(REQUIRED_KEY);
      for (JsonElement element : targetRequired) {
        if (element.isJsonPrimitive()) {
          combined.add(element.getAsString());
        }
      }
    }

    for (JsonElement element : sourceRequired) {
      if (element.isJsonPrimitive()) {
        combined.add(element.getAsString());
      }
    }

    JsonArray mergedRequired = new JsonArray();
    for (String value : combined) {
      mergedRequired.add(value);
    }

    target.add(REQUIRED_KEY, mergedRequired);
  }

  private void mergeMissingSimpleKeys(JsonObject target, JsonObject source) {
    for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
      String key = entry.getKey();
      if (key.equals(PROPERTIES_KEY) || key.equals(REQUIRED_KEY)) {
        continue;
      }
      if (!target.has(key)) {
        target.add(key, entry.getValue());
      }
    }
  }

  private JsonElement resolveRef(JsonObject schemaRoot, String reference) {
    if (reference == null) {
      return null;
    }
    if (!reference.startsWith("#/")) {
      return null;
    }

    String pointer = reference.substring(2);
    String[] parts = pointer.split("/");

    JsonElement current = schemaRoot;
    for (String part : parts) {
      if (current == null || !current.isJsonObject()) {
        return null;
      }
      JsonObject currentObject = current.getAsJsonObject();
      if (!currentObject.has(part)) {
        return null;
      }
      current = currentObject.get(part);
    }
    return current;
  }
}