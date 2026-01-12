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

    if (schemaObject.has("allOf") && schemaObject.get("allOf").isJsonArray()) {
      return mergeAllOf(schemaObject, schemaRoot);
    }

    if (schemaObject.has("oneOf") && schemaObject.get("oneOf").isJsonArray()) {
      JsonArray oneOfArray = schemaObject.getAsJsonArray("oneOf");
      if (!oneOfArray.isEmpty()) {
        return resolve(oneOfArray.get(0), schemaRoot);
      }
    }

    if (schemaObject.has("anyOf") && schemaObject.get("anyOf").isJsonArray()) {
      JsonArray anyOfArray = schemaObject.getAsJsonArray("anyOf");
      if (!anyOfArray.isEmpty()) {
        return resolve(anyOfArray.get(0), schemaRoot);
      }
    }

    return schemaObject;
  }

  public JsonObject coerceToObjectSchema(JsonObject schema, JsonObject schemaRoot) {
    JsonObject resolvedSchema = schema;
    String resolvedType = getType(resolvedSchema);

    if ("object".equals(resolvedType) || resolvedSchema.has("properties")) {
      return resolvedSchema;
    }

    JsonObject objectSchema = new JsonObject();
    objectSchema.addProperty("type", "object");
    objectSchema.add("properties", new JsonObject());
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
      if (!entry.getKey().equals("allOf")) {
        mergedSchema.add(entry.getKey(), entry.getValue());
      }
    }

    JsonArray allOfArray = schemaObject.getAsJsonArray("allOf");
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
    JsonObject targetProperties = target.has("properties") && target.get("properties").isJsonObject()
        ? target.getAsJsonObject("properties")
        : null;

    JsonObject sourceProperties = source.has("properties") && source.get("properties").isJsonObject()
        ? source.getAsJsonObject("properties")
        : null;

    if (sourceProperties == null) {
      return;
    }

    if (targetProperties == null) {
      target.add("properties", sourceProperties);
      return;
    }

    for (Map.Entry<String, JsonElement> entry : sourceProperties.entrySet()) {
      if (!targetProperties.has(entry.getKey())) {
        targetProperties.add(entry.getKey(), entry.getValue());
      }
    }
  }

  private void mergeRequired(JsonObject target, JsonObject source) {
    JsonArray sourceRequired = source.has("required") && source.get("required").isJsonArray()
        ? source.getAsJsonArray("required")
        : null;

    if (sourceRequired == null) {
      return;
    }

    Set<String> combined = new LinkedHashSet<>();

    if (target.has("required") && target.get("required").isJsonArray()) {
      JsonArray targetRequired = target.getAsJsonArray("required");
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

    target.add("required", mergedRequired);
  }

  private void mergeMissingSimpleKeys(JsonObject target, JsonObject source) {
    for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
      String key = entry.getKey();
      if (key.equals("properties") || key.equals("required")) {
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