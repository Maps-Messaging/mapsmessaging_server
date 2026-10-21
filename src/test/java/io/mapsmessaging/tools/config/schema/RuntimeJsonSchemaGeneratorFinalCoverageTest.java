package io.mapsmessaging.tools.config.schema;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeJsonSchemaGeneratorFinalCoverageTest {

  @Test
  void generatedDocumentCarriesRootMetadataReferenceAndDefinitions() {
    String json = new RuntimeJsonSchemaGenerator().generateSchema("Example", ExampleDto.class);
    JsonObject root = JsonParser.parseString(json).getAsJsonObject();

    assertEquals(
        "https://json-schema.org/draft/2020-12/schema",
        root.get("$schema").getAsString());
    assertEquals("urn:mapsmessaging:config-schema:Example", root.get("$id").getAsString());
    assertEquals("Example", root.get("title").getAsString());
    assertEquals("Example DTO", root.get("description").getAsString());
    assertTrue(root.get("$ref").getAsString().startsWith("#/$defs/"));
    assertTrue(root.getAsJsonObject("$defs").size() >= 1);
  }

  @Test
  void requiredNullableAndNumericConstraintsAreEmittedWithCorrectTypes() {
    JsonObject definition = definitionFor(
        new RuntimeJsonSchemaGenerator().generateSchema("Example", ExampleDto.class));
    JsonObject properties = definition.getAsJsonObject("properties");

    assertTrue(definition.getAsJsonArray("required").toString().contains("count"));

    JsonObject count = properties.getAsJsonObject("count");
    assertEquals("integer", count.get("type").getAsString());
    assertEquals(1, count.get("minimum").getAsInt());
    assertEquals(10, count.get("maximum").getAsInt());
    assertEquals(3, count.get("default").getAsInt());

    JsonObject label = properties.getAsJsonObject("label");
    assertTrue(label.has("anyOf"));
    assertEquals(2, label.getAsJsonArray("anyOf").size());
  }

  @Test
  void enumFieldsProduceStringEnumsAndExamples() {
    JsonObject definition = definitionFor(
        new RuntimeJsonSchemaGenerator().generateSchema("Example", ExampleDto.class));
    JsonObject mode = definition.getAsJsonObject("properties").getAsJsonObject("mode");

    assertEquals("string", mode.get("type").getAsString());
    assertTrue(mode.getAsJsonArray("enum").toString().contains("FAST"));
    assertTrue(mode.getAsJsonArray("enum").toString().contains("SLOW"));
  }

  @Test
  void strictModeRejectsUnsupportedTypeWhileRelaxedModeCanEmitWarningPlaceholder() {
    RuntimeJsonSchemaGenerator strict =
        new RuntimeJsonSchemaGenerator(SchemaGenerationMode.STRICT, true);
    assertThrows(
        RuntimeException.class,
        () -> strict.generateSchema("Unsupported", UnsupportedDto.class));

    RuntimeJsonSchemaGenerator relaxed =
        new RuntimeJsonSchemaGenerator(SchemaGenerationMode.RELAXED, true);
    JsonObject root = JsonParser.parseString(
        relaxed.generateSchema("Unsupported", UnsupportedDto.class)).getAsJsonObject();

    assertTrue(root.has("x-warnings"));
    assertFalse(root.getAsJsonArray("x-warnings").isEmpty());
  }

  private static JsonObject definitionFor(String json) {
    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
    String ref = root.get("$ref").getAsString();
    String key = ref.substring(ref.lastIndexOf('/') + 1);
    return root.getAsJsonObject("$defs").getAsJsonObject(key);
  }

  @Schema(description = "Example DTO")
  static class ExampleDto extends BaseConfigDTO {
    @Schema(
        description = "Count",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minimum = "1",
        maximum = "10",
        defaultValue = "3",
        example = "4")
    int count;

    @Schema(description = "Optional label", nullable = true)
    String label;

    @Schema(description = "Mode")
    Mode mode;
  }

  static class UnsupportedDto extends BaseConfigDTO {
    @Schema(description = "Unsupported timestamp")
    Instant timestamp;
  }

  enum Mode {
    FAST,
    SLOW
  }
}