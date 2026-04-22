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

package io.mapsmessaging.api.transformers.jsonmapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.dto.rest.config.transformer.jsonmapper.JsonMapFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonMapFunctionsTest {

  @Test
  void apply_nullValue_returnsJsonNull() {
    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.TO_STRING, null);

    assertSame(JsonNull.INSTANCE, result);
  }

  @Test
  void apply_jsonNull_returnsJsonNull() {
    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.TO_STRING, JsonNull.INSTANCE);

    assertSame(JsonNull.INSTANCE, result);
  }

  @Test
  void apply_nullFunction_returnsDeepCopy() {
    JsonObject value = new JsonObject();
    value.addProperty("name", "matthew");

    JsonElement result = JsonMapFunctions.apply(null, value);

    assertTrue(result.isJsonObject());
    assertNotSame(value, result);
    assertEquals("matthew", result.getAsJsonObject().get("name").getAsString());
  }

  @Test
  void apply_noneFunction_returnsDeepCopy() {
    JsonObject value = new JsonObject();
    value.addProperty("name", "matthew");

    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.NONE, value);

    assertTrue(result.isJsonObject());
    assertNotSame(value, result);
    assertEquals("matthew", result.getAsJsonObject().get("name").getAsString());
  }

  @Test
  void apply_toString_onNumber_returnsStringPrimitive() {
    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.TO_STRING, new JsonPrimitive(123));

    assertEquals("123", result.getAsString());
  }

  @Test
  void apply_toString_onObject_returnsJsonString() {
    JsonObject value = new JsonObject();
    value.addProperty("name", "uav-1");

    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.TO_STRING, value);

    assertEquals("{\"name\":\"uav-1\"}", result.getAsString());
  }

  @Test
  void apply_toInt_returnsIntegerPrimitive() {
    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.TO_INT, new JsonPrimitive("123"));

    assertEquals(123, result.getAsInt());
  }

  @Test
  void apply_toLong_returnsLongPrimitive() {
    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.TO_LONG, new JsonPrimitive("123456789"));

    assertEquals(123456789L, result.getAsLong());
  }

  @Test
  void apply_toFloat_returnsFloatPrimitive() {
    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.TO_FLOAT, new JsonPrimitive("12.5"));

    assertEquals(12.5f, result.getAsFloat(), 0.0001f);
  }

  @Test
  void apply_toDouble_returnsDoublePrimitive() {
    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.TO_DOUBLE, new JsonPrimitive("123.456"));

    assertEquals(123.456d, result.getAsDouble(), 0.0000001d);
  }

  @Test
  void apply_toBoolean_returnsBooleanPrimitive() {
    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.TO_BOOLEAN, new JsonPrimitive("true"));

    assertTrue(result.getAsBoolean());
  }

  @Test
  void apply_base64Encode_returnsEncodedString() {
    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.BASE64_ENCODE, new JsonPrimitive("Maps"));

    assertEquals("TWFwcw==", result.getAsString());
  }

  @Test
  void apply_base64Decode_returnsDecodedString() {
    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.BASE64_DECODE, new JsonPrimitive("TWFwcw=="));

    assertEquals("Maps", result.getAsString());
  }

  @Test
  void apply_noneFunction_onArray_returnsDeepCopy() {
    JsonArray value = new JsonArray();
    value.add("one");
    value.add("two");

    JsonElement result = JsonMapFunctions.apply(JsonMapFunction.NONE, value);

    assertTrue(result.isJsonArray());
    assertNotSame(value, result);
    assertEquals(2, result.getAsJsonArray().size());

    value.add("three");

    assertEquals(2, result.getAsJsonArray().size());
  }
}