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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.mapsmessaging.tools.config.schema.JsonSchemaValueCoercion.SchemaScalarType.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaValueCoercionTest {

  @Test
  void coerceValue_nullAndBlankValues_returnNullNode() {
    assertTrue(JsonSchemaValueCoercion.coerceValue(null, STRING).isNull());
    assertTrue(JsonSchemaValueCoercion.coerceValue("   ", INTEGER).isNull());
    assertTrue(JsonSchemaValueCoercion.coerceValue(" NULL ", BOOLEAN).isNull());
  }

  @Test
  void coerceValue_string_preservesOriginalWhitespace() {
    JsonNode result = JsonSchemaValueCoercion.coerceValue("  value  ", STRING);

    assertTrue(result.isTextual());
    assertEquals("  value  ", result.textValue());
  }

  @Test
  void coerceValue_integer_supportsSeparatorsAndHexadecimal() {
    assertEquals(1234567L, JsonSchemaValueCoercion.coerceValue("1_234_567", INTEGER).longValue());
    assertEquals(255L, JsonSchemaValueCoercion.coerceValue(" 0xFF ", INTEGER).longValue());
  }

  @Test
  void coerceValue_number_preservesDecimalPrecision() {
    JsonNode result = JsonSchemaValueCoercion.coerceValue("1_234.567890123456789", NUMBER);

    assertEquals(new BigDecimal("1234.567890123456789"), result.decimalValue());
  }

  @Test
  void coerceValue_boolean_isCaseInsensitiveAndRejectsMalformedValue() {
    assertTrue(JsonSchemaValueCoercion.coerceValue(" TRUE ", BOOLEAN).booleanValue());
    assertFalse(JsonSchemaValueCoercion.coerceValue("false", BOOLEAN).booleanValue());
    assertThrows(IllegalArgumentException.class,
        () -> JsonSchemaValueCoercion.coerceValue("yes", BOOLEAN));
  }

  @Test
  void coerceEnumValues_preservesOrderAndHandlesEmptyInputs() {
    List<JsonNode> result = JsonSchemaValueCoercion.coerceEnumValues(
        List.of("0x10", "20", "null"), INTEGER);

    assertEquals(3, result.size());
    assertEquals(16L, result.get(0).longValue());
    assertEquals(20L, result.get(1).longValue());
    assertTrue(result.get(2).isNull());
    assertEquals(List.of(), JsonSchemaValueCoercion.coerceEnumValues(null, STRING));
    assertEquals(List.of(), JsonSchemaValueCoercion.coerceEnumValues(List.of(), STRING));
  }
}
