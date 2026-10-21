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

package io.mapsmessaging.tools.config.schema;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaDefaultsTest {

  @Test
  void nullAndUntypedDefaultsArePreservedSafely() {
    SchemaObject untyped = new SchemaObject();

    assertNull(JsonSchemaDefaults.coerceDefaultValue(null, untyped));
    assertEquals("  original  ", JsonSchemaDefaults.coerceDefaultValue("  original  ", untyped));
  }

  @Test
  void booleansAreCoercedOnlyForValidBooleanText() {
    SchemaObject schema = schema("boolean");

    assertEquals(true, JsonSchemaDefaults.coerceDefaultValue(" true ", schema));
    assertEquals(false, JsonSchemaDefaults.coerceDefaultValue("FALSE", schema));
    assertNull(JsonSchemaDefaults.coerceDefaultValue("yes", schema));
  }

  @Test
  void integerAndNumberDefaultsAllowNumericSeparators() {
    assertEquals(123456L, JsonSchemaDefaults.coerceDefaultValue("123_456", schema("integer")));
    assertEquals(1234.5, (Double) JsonSchemaDefaults.coerceDefaultValue("1_234.5", schema("number")), 0.0);
    assertNull(JsonSchemaDefaults.coerceDefaultValue("not-a-number", schema("integer")));
  }

  @Test
  void arraysAndObjectsAreParsedFromJson() {
    Object array = JsonSchemaDefaults.coerceDefaultValue("[1,2,3]", schema("array"));
    Object object = JsonSchemaDefaults.coerceDefaultValue("{\"name\":\"maps\",\"enabled\":true}", schema("object"));

    assertInstanceOf(List.class, array);
    assertEquals(List.of(1, 2, 3), array);

    Map<?, ?> map = assertInstanceOf(Map.class, object);
    assertEquals("maps", map.get("name"));
    assertEquals(true, map.get("enabled"));
  }

  @Test
  void malformedStructuredDefaultsReturnNull() {
    assertNull(JsonSchemaDefaults.coerceDefaultValue("not-array", schema("array")));
    assertNull(JsonSchemaDefaults.coerceDefaultValue("[broken", schema("array")));
    assertNull(JsonSchemaDefaults.coerceDefaultValue("not-object", schema("object")));
    assertNull(JsonSchemaDefaults.coerceDefaultValue("{broken", schema("object")));
  }

  @Test
  void stringsAndUnknownTypesRemainStrings() {
    assertEquals(" keep spaces ", JsonSchemaDefaults.coerceDefaultValue(" keep spaces ", schema("string")));
    assertEquals("value", JsonSchemaDefaults.coerceDefaultValue("value", schema("custom")));
  }

  private static SchemaObject schema(String type) {
    SchemaObject schema = new SchemaObject();
    schema.put("type", type);
    return schema;
  }
}
