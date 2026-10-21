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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaIntrospectorTest {

  @Test
  void extractsDocumentationConstraintsAndEnumValues() {
    JsonObject schema = new JsonObject();
    schema.addProperty("title", "Port");
    schema.addProperty("description", "TCP port");
    schema.addProperty("format", "int32");
    schema.addProperty("pattern", "[0-9]+");
    schema.addProperty("default", 1883);
    schema.addProperty("minimum", 1);
    schema.addProperty("maximum", 65535);
    schema.addProperty("exclusiveMinimum", 0);
    schema.addProperty("exclusiveMaximum", 65536);
    schema.addProperty("minLength", 1);
    schema.addProperty("maxLength", 5);
    schema.addProperty("multipleOf", 1);

    JsonArray values = new JsonArray();
    values.add("mqtt");
    values.add(1883);
    values.add((String) null);
    JsonObject complex = new JsonObject();
    complex.addProperty("name", "custom");
    values.add(complex);
    schema.add("enum", values);

    SchemaDoc doc = new SchemaIntrospector().extract(schema);

    assertEquals("Port", doc.getTitle());
    assertEquals("TCP port", doc.getDescription());
    assertEquals("int32", doc.getFormat());
    assertEquals("[0-9]+", doc.getPattern());
    assertEquals("1883", doc.getDefaultValue());
    assertEquals("1", doc.getMinimum());
    assertEquals("65535", doc.getMaximum());
    assertEquals("0", doc.getExclusiveMinimum());
    assertEquals("65536", doc.getExclusiveMaximum());
    assertEquals("1", doc.getMinLength());
    assertEquals("5", doc.getMaxLength());
    assertEquals("1", doc.getMultipleOf());
    assertEquals(List.of("mqtt", "1883", "{\"name\":\"custom\"}"), doc.getAllowedValues());
  }

  @Test
  void nullOrMissingValuesProduceEmptyDocumentation() {
    SchemaIntrospector introspector = new SchemaIntrospector();

    SchemaDoc fromNull = introspector.extract(null);
    SchemaDoc fromEmpty = introspector.extract(new JsonObject());

    assertNull(fromNull.getTitle());
    assertNull(fromNull.getAllowedValues());
    assertNull(fromEmpty.getDescription());
    assertNull(fromEmpty.getDefaultValue());
  }

  @Test
  void nonPrimitiveTextFieldsAndNonArrayEnumsAreRenderedSafely() {
    JsonObject schema = new JsonObject();
    JsonObject title = new JsonObject();
    title.addProperty("nested", "value");
    schema.add("title", title);
    schema.addProperty("enum", "not-an-array");

    SchemaDoc doc = new SchemaIntrospector().extract(schema);

    assertEquals("{\"nested\":\"value\"}", doc.getTitle());
    assertNull(doc.getAllowedValues());
  }

  @Test
  void emptyEnumIsTreatedAsNoRestriction() {
    JsonObject schema = new JsonObject();
    schema.add("enum", new JsonArray());

    assertNull(new SchemaIntrospector().extract(schema).getAllowedValues());
  }
}
