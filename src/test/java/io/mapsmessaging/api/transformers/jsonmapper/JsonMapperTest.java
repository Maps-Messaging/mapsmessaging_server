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
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.api.transformers.jsonmapper;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.api.transformers.jsonmutate.JsonPath;
import io.mapsmessaging.dto.rest.config.transformer.jsonmapper.JsonMapFunction;
import io.mapsmessaging.dto.rest.config.transformer.jsonmapper.JsonMapOpDTO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonMapperTest {

  @Test
  void apply_nullSource_returnsEmptyTarget() {
    JsonMapper jsonMapper = new JsonMapper(List.of(operation("source.value", "target.value")));

    JsonObject result = jsonMapper.apply(null);

    assertNotNull(result);
    assertTrue(result.entrySet().isEmpty());
  }

  @Test
  void apply_nullOperations_returnsEmptyTarget() {
    JsonMapper jsonMapper = new JsonMapper(null);
    JsonObject source = new JsonObject();
    source.addProperty("name", "matthew");

    JsonObject result = jsonMapper.apply(source);

    assertNotNull(result);
    assertTrue(result.entrySet().isEmpty());
  }

  @Test
  void apply_emptyOperations_returnsEmptyTarget() {
    JsonMapper jsonMapper = new JsonMapper(List.of());
    JsonObject source = new JsonObject();
    source.addProperty("name", "matthew");

    JsonObject result = jsonMapper.apply(source);

    assertNotNull(result);
    assertTrue(result.entrySet().isEmpty());
  }

  @Test
  void apply_simpleMapping_copiesValueToTargetPath() {
    JsonObject source = new JsonObject();
    JsonObject position = new JsonObject();
    position.addProperty("lat", 47.3979981d);
    source.add("position", position);

    JsonMapper jsonMapper = new JsonMapper(
        List.of(operation("position.lat", "stanag.location.lat"))
    );

    JsonObject result = jsonMapper.apply(source);

    assertEquals(47.3979981d, JsonPath.get(result, "stanag.location.lat").getAsDouble(), 0.0000001d);
  }

  @Test
  void apply_multipleMappings_buildsNestedTargetObject() {
    JsonObject source = new JsonObject();

    JsonObject position = new JsonObject();
    position.addProperty("lat", 47.3979981d);
    position.addProperty("lon", 8.5461638d);
    source.add("position", position);

    JsonObject track = new JsonObject();
    track.addProperty("heading", 96.72d);
    source.add("track", track);

    JsonMapper jsonMapper = new JsonMapper(
        List.of(
            operation("position.lat", "stanag.location.lat"),
            operation("position.lon", "stanag.location.lon"),
            operation("track.heading", "stanag.orientation.heading")
        )
    );

    JsonObject result = jsonMapper.apply(source);

    assertEquals(47.3979981d, JsonPath.get(result, "stanag.location.lat").getAsDouble(), 0.0000001d);
    assertEquals(8.5461638d, JsonPath.get(result, "stanag.location.lon").getAsDouble(), 0.0000001d);
    assertEquals(96.72d, JsonPath.get(result, "stanag.orientation.heading").getAsDouble(), 0.0000001d);
  }

  @Test
  void apply_toStringFunction_convertsNumericValue() {
    JsonObject source = new JsonObject();
    source.addProperty("systemId", 12);

    JsonMapOpDTO operation = operation("systemId", "identity.systemId");
    operation.setFunction(JsonMapFunction.TO_STRING);

    JsonMapper jsonMapper = new JsonMapper(List.of(operation));

    JsonObject result = jsonMapper.apply(source);

    assertEquals("12", JsonPath.get(result, "identity.systemId").getAsString());
  }

  @Test
  void apply_toIntFunction_convertsStringValue() {
    JsonObject source = new JsonObject();
    source.addProperty("systemId", "42");

    JsonMapOpDTO operation = operation("systemId", "identity.systemId");
    operation.setFunction(JsonMapFunction.TO_INT);

    JsonMapper jsonMapper = new JsonMapper(List.of(operation));

    JsonObject result = jsonMapper.apply(source);

    assertEquals(42, JsonPath.get(result, "identity.systemId").getAsInt());
  }

  @Test
  void apply_toDoubleFunction_convertsStringValue() {
    JsonObject source = new JsonObject();
    source.addProperty("altitude", "123.456");

    JsonMapOpDTO operation = operation("altitude", "stanag.location.altitude");
    operation.setFunction(JsonMapFunction.TO_DOUBLE);

    JsonMapper jsonMapper = new JsonMapper(List.of(operation));

    JsonObject result = jsonMapper.apply(source);

    assertEquals(123.456d, JsonPath.get(result, "stanag.location.altitude").getAsDouble(), 0.0000001d);
  }

  @Test
  void apply_toBooleanFunction_convertsStringValue() {
    JsonObject source = new JsonObject();
    source.addProperty("active", "true");

    JsonMapOpDTO operation = operation("active", "status.active");
    operation.setFunction(JsonMapFunction.TO_BOOLEAN);

    JsonMapper jsonMapper = new JsonMapper(List.of(operation));

    JsonObject result = jsonMapper.apply(source);

    assertTrue(JsonPath.get(result, "status.active").getAsBoolean());
  }

  @Test
  void apply_base64EncodeFunction_encodesString() {
    JsonObject source = new JsonObject();
    source.addProperty("payload", "Maps");

    JsonMapOpDTO operation = operation("payload", "encoded.payload");
    operation.setFunction(JsonMapFunction.BASE64_ENCODE);

    JsonMapper jsonMapper = new JsonMapper(List.of(operation));

    JsonObject result = jsonMapper.apply(source);

    assertEquals("TWFwcw==", JsonPath.get(result, "encoded.payload").getAsString());
  }

  @Test
  void apply_base64DecodeFunction_decodesString() {
    JsonObject source = new JsonObject();
    source.addProperty("payload", "TWFwcw==");

    JsonMapOpDTO operation = operation("payload", "decoded.payload");
    operation.setFunction(JsonMapFunction.BASE64_DECODE);

    JsonMapper jsonMapper = new JsonMapper(List.of(operation));

    JsonObject result = jsonMapper.apply(source);

    assertEquals("Maps", JsonPath.get(result, "decoded.payload").getAsString());
  }

  @Test
  void apply_missingSourceValue_usesDefaultValue() {
    JsonObject source = new JsonObject();

    JsonMapOpDTO operation = operation("missing.path", "target.value");
    operation.setDefaultValue(new JsonPrimitive("default-text"));
    operation.setIgnoreMissing(false);

    JsonMapper jsonMapper = new JsonMapper(List.of(operation));

    JsonObject result = jsonMapper.apply(source);

    assertEquals("default-text", JsonPath.get(result, "target.value").getAsString());
  }

  @Test
  void apply_missingSourceValue_andIgnoreMissingTrue_skipsMapping() {
    JsonObject source = new JsonObject();

    JsonMapOpDTO operation = operation("missing.path", "target.value");
    operation.setIgnoreMissing(true);

    JsonMapper jsonMapper = new JsonMapper(List.of(operation));

    JsonObject result = jsonMapper.apply(source);

    assertNull(JsonPath.get(result, "target.value"));
  }

  @Test
  void apply_missingSourceValue_andIgnoreMissingFalse_withNoDefault_writesJsonNull() {
    JsonObject source = new JsonObject();

    JsonMapOpDTO operation = operation("missing.path", "target.value");
    operation.setIgnoreMissing(false);

    JsonMapper jsonMapper = new JsonMapper(List.of(operation));

    JsonObject result = jsonMapper.apply(source);

    assertNotNull(JsonPath.get(result, "target.value"));
    assertTrue(JsonPath.get(result, "target.value").isJsonNull());
  }

  @Test
  void apply_nullOperation_skipsEntry() {
    JsonObject source = new JsonObject();
    source.addProperty("name", "drone-1");

    JsonMapper jsonMapper = new JsonMapper(
        Arrays.asList(
            null,
            operation("name", "identity.name")
        )
    );

    JsonObject result = jsonMapper.apply(source);

    assertEquals("drone-1", JsonPath.get(result, "identity.name").getAsString());
  }

  @Test
  void apply_blankFrom_skipsEntry() {
    JsonObject source = new JsonObject();
    source.addProperty("name", "drone-1");

    JsonMapOpDTO operation = operation("", "identity.name");

    JsonMapper jsonMapper = new JsonMapper(List.of(operation));

    JsonObject result = jsonMapper.apply(source);

    assertNull(JsonPath.get(result, "identity.name"));
  }

  @Test
  void apply_blankTo_skipsEntry() {
    JsonObject source = new JsonObject();
    source.addProperty("name", "drone-1");

    JsonMapOpDTO operation = operation("name", "");

    JsonMapper jsonMapper = new JsonMapper(List.of(operation));

    JsonObject result = jsonMapper.apply(source);

    assertTrue(result.entrySet().isEmpty());
  }

  @Test
  void apply_existingTarget_appendsValues() {
    JsonObject source = new JsonObject();
    source.addProperty("name", "drone-1");

    JsonObject target = new JsonObject();
    target.addProperty("existing", "value");

    JsonMapper jsonMapper = new JsonMapper(
        List.of(operation("name", "identity.name"))
    );

    JsonObject result = jsonMapper.apply(source, target);

    assertSame(target, result);
    assertEquals("value", result.get("existing").getAsString());
    assertEquals("drone-1", JsonPath.get(result, "identity.name").getAsString());
  }

  @Test
  void apply_existingTargetNull_createsNewTarget() {
    JsonObject source = new JsonObject();
    source.addProperty("name", "drone-1");

    JsonMapper jsonMapper = new JsonMapper(
        List.of(operation("name", "identity.name"))
    );

    JsonObject result = jsonMapper.apply(source, null);

    assertNotNull(result);
    assertEquals("drone-1", JsonPath.get(result, "identity.name").getAsString());
  }

  @Test
  void apply_objectValue_deepCopiesObject() {
    JsonObject source = new JsonObject();

    JsonObject platform = new JsonObject();
    platform.addProperty("name", "uav-1");
    platform.addProperty("type", "UAV");
    source.add("platform", platform);

    JsonMapper jsonMapper = new JsonMapper(
        List.of(operation("platform", "stanag.platform"))
    );

    JsonObject result = jsonMapper.apply(source);

    JsonObject mappedPlatform = JsonPath.get(result, "stanag.platform").getAsJsonObject();
    assertEquals("uav-1", mappedPlatform.get("name").getAsString());
    assertEquals("UAV", mappedPlatform.get("type").getAsString());

    platform.addProperty("name", "changed");

    assertEquals("uav-1", JsonPath.get(result, "stanag.platform.name").getAsString());
  }

  @Test
  void apply_defaultValueJsonNull_writesJsonNull() {
    JsonObject source = new JsonObject();

    JsonMapOpDTO operation = operation("missing.path", "target.value");
    operation.setDefaultValue(JsonNull.INSTANCE);
    operation.setIgnoreMissing(false);

    JsonMapper jsonMapper = new JsonMapper(List.of(operation));

    JsonObject result = jsonMapper.apply(source);

    assertTrue(JsonPath.get(result, "target.value").isJsonNull());
  }

  private JsonMapOpDTO operation(String from, String to) {
    JsonMapOpDTO operation = new JsonMapOpDTO();
    operation.setFrom(from);
    operation.setTo(to);
    operation.setFunction(JsonMapFunction.NONE);
    operation.setIgnoreMissing(true);
    return operation;
  }
}