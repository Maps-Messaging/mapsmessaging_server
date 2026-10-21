package io.mapsmessaging.tools.config.yaml;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlNodeRendererBranchCoverageTest {

  private final YamlNodeRenderer renderer = new YamlNodeRenderer(
      new SchemaResolver(),
      new SchemaIntrospector(),
      new YamlCommentEmitter(),
      new YamlValueFormatter());

  @Test
  void iterableArrayValuesAreRenderedEvenWhenTheyAreNotLists() {
    JsonObject schema = objectWith("values", arrayOf(type("string")));

    String yaml = renderer.renderRoot(
        "root",
        Map.of("values", new LinkedHashSet<>(List.of("a", "b"))),
        schema,
        RenderMode.MINIMAL);

    assertTrue(yaml.contains("- a"));
    assertTrue(yaml.contains("- b"));
  }

  @Test
  void scalarSchemaReceivingEmptyMapRendersEmptyObject() {
    JsonObject schema = objectWith("value", type("string"));

    String yaml = renderer.renderRoot(
        "root",
        Map.of("value", Map.of()),
        schema,
        RenderMode.FULL);

    assertTrue(yaml.contains("value: {}"));
  }

  @Test
  void missingOrNonObjectPropertiesProduceOnlyRootHeader() {
    JsonObject missing = type("object");
    JsonObject invalid = type("object");
    invalid.addProperty("properties", "wrong");

    assertEquals("root:\n", renderer.renderRoot("root", Map.of(), missing, RenderMode.FULL));
    assertEquals("root:\n", renderer.renderRoot("root", Map.of(), invalid, RenderMode.FULL));
  }

  @Test
  void malformedRequiredMetadataIsIgnored() {
    JsonObject schema = objectWith("name", type("string"));
    schema.addProperty("required", "name");

    String yaml = renderer.renderRoot("root", Map.of(), schema, RenderMode.MINIMAL);

    assertEquals("root:\n", yaml);
  }

  private static JsonObject objectWith(String name, JsonObject property) {
    JsonObject root = type("object");
    JsonObject properties = new JsonObject();
    properties.add(name, property);
    root.add("properties", properties);
    return root;
  }

  private static JsonObject arrayOf(JsonObject item) {
    JsonObject array = type("array");
    array.add("items", item);
    return array;
  }

  private static JsonObject type(String type) {
    JsonObject schema = new JsonObject();
    schema.addProperty("type", type);
    return schema;
  }
}