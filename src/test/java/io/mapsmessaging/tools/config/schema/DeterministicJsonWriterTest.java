package io.mapsmessaging.tools.config.schema;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicJsonWriterTest {

  @Test
  void knownSchemaKeysAreOrderedBeforeUnknownKeysAndNullsAreOmitted() {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("zeta", true);
    value.put("properties", Map.of());
    value.put("title", "Example");
    value.put("$schema", "schema-uri");
    value.put("alpha", 1);
    value.put("description", null);

    String json = DeterministicJsonWriter.write(value);

    assertTrue(json.indexOf(""$schema"") < json.indexOf(""title""));
    assertTrue(json.indexOf(""title"") < json.indexOf(""properties""));
    assertTrue(json.indexOf(""properties"") < json.indexOf(""alpha""));
    assertTrue(json.indexOf(""alpha"") < json.indexOf(""zeta""));
    assertFalse(json.contains("description"));
  }

  @Test
  void numericSchemaStringsAreCoercedOnlyInNumericContexts() {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("minimum", "1.500");
    value.put("default", "+2");
    value.put("enum", List.of("3", "text"));
    value.put("title", "4");

    String json = DeterministicJsonWriter.write(value);

    assertTrue(json.contains(""minimum": 1.5"));
    assertTrue(json.contains(""default": "+2""));
    assertTrue(json.contains("3,"));
    assertTrue(json.contains(""text""));
    assertTrue(json.contains(""title": "4""));
  }

  @Test
  void stringsEscapeJsonControlCharacters() {
    String json = DeterministicJsonWriter.write(Map.of("title", "line1\n\"quoted\"\\tail\t"));

    assertTrue(json.contains("line1\\n\\\"quoted\\\"\\\\tail\\t"));
  }

  @Test
  void unsupportedValueTypesAreRejected() {
    assertThrows(IllegalStateException.class, () -> DeterministicJsonWriter.write(Map.of("title", new Object())));
  }
}