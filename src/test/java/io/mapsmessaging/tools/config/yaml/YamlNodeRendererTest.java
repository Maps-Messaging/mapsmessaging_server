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
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlNodeRendererTest {

  private final YamlNodeRenderer renderer = new YamlNodeRenderer(
      new SchemaResolver(),
      new SchemaIntrospector(),
      new YamlCommentEmitter(),
      new YamlValueFormatter()
  );

  @Test
  void minimalRenderingUsesDefaultsAndOmitsMeaninglessOptionalValues() {
    JsonObject schema = rootSchema();
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("name", "");
    values.put("enabled", null);
    values.put("optional", " ");
    values.put("tags", List.of("one", "two"));

    String yaml = renderer.renderRoot("config", values, schema, RenderMode.MINIMAL);

    assertTrue(yaml.startsWith("config:\n"));
    assertTrue(yaml.contains("  name: default-name"));
    assertTrue(yaml.contains("  enabled: true"));
    assertFalse(yaml.contains("optional:"));
    assertTrue(yaml.contains("  tags:\n"));
    assertTrue(yaml.contains("- one"));
    assertTrue(yaml.contains("- two"));
  }

  @Test
  void fullRenderingIncludesEmptyObjectArrayTemplateAndSuppressesNestedVersionField() {
    JsonObject schema = rootSchema();
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("child", Map.of());
    values.put("objects", List.of());

    String yaml = renderer.renderRoot("config", values, schema, RenderMode.FULL);

    assertTrue(yaml.contains("  child:\n"));
    assertTrue(yaml.contains("    value: null"));
    assertFalse(yaml.contains("schemaLoadingVersion:"));
    assertTrue(yaml.contains("  objects:\n"));
    assertTrue(yaml.contains("# ----------------------------------------------------------------------"));
    assertTrue(yaml.contains("      id: 1"));
  }

  @Test
  void commentsAreEmittedFromPropertySchema() {
    JsonObject schema = rootSchema();

    String yaml = renderer.renderRoot("config", Map.of(), schema, RenderMode.FULL);

    assertTrue(yaml.contains("# Config name (default=\"default-name\")"));
  }

  private static JsonObject rootSchema() {
    JsonObject root = type("object");
    JsonObject properties = new JsonObject();

    JsonObject name = type("string");
    name.addProperty("description", "Config name");
    name.addProperty("default", "default-name");
    properties.add("name", name);

    JsonObject enabled = type("boolean");
    enabled.addProperty("default", true);
    properties.add("enabled", enabled);

    properties.add("optional", type("string"));

    JsonObject tags = type("array");
    tags.add("items", type("string"));
    properties.add("tags", tags);

    JsonObject child = type("object");
    JsonObject childProperties = new JsonObject();
    childProperties.add("schemaLoadingVersion", type("integer"));
    childProperties.add("value", type("string"));
    child.add("properties", childProperties);
    properties.add("child", child);

    JsonObject objects = type("array");
    JsonObject item = type("object");
    JsonObject itemProperties = new JsonObject();
    JsonObject id = type("integer");
    id.addProperty("default", 1);
    itemProperties.add("id", id);
    item.add("properties", itemProperties);
    objects.add("items", item);
    properties.add("objects", objects);

    root.add("properties", properties);

    JsonArray required = new JsonArray();
    required.add("name");
    root.add("required", required);
    return root;
  }

  private static JsonObject type(String type) {
    JsonObject schema = new JsonObject();
    schema.addProperty("type", type);
    return schema;
  }
}
