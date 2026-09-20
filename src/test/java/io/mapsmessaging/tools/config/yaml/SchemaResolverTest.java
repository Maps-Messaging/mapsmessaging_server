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
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaResolverTest {

  private final SchemaResolver resolver = new SchemaResolver();

  @Test
  void localReferencesResolveThroughSchemaRoot() {
    JsonObject root = new JsonObject();
    JsonObject definitions = new JsonObject();
    JsonObject item = objectSchema("name");
    definitions.add("Item", item);
    root.add("$defs", definitions);

    JsonObject reference = new JsonObject();
    reference.addProperty("$ref", "#/$defs/Item");

    assertSame(item, resolver.resolve(reference, root));
  }

  @Test
  void unresolvedOrExternalReferencesRemainOriginalSchema() {
    JsonObject root = new JsonObject();
    JsonObject missing = new JsonObject();
    missing.addProperty("$ref", "#/$defs/Missing");
    JsonObject external = new JsonObject();
    external.addProperty("$ref", "other.json#/Item");

    assertSame(missing, resolver.resolve(missing, root));
    assertSame(external, resolver.resolve(external, root));
  }

  @Test
  void allOfMergesPropertiesRequiredAndSimpleKeysWithoutOverwritingExistingValues() {
    JsonObject root = new JsonObject();
    JsonObject schema = new JsonObject();
    schema.addProperty("title", "local");

    JsonArray allOf = new JsonArray();
    JsonObject first = objectSchema("first");
    JsonArray firstRequired = new JsonArray();
    firstRequired.add("first");
    first.add("required", firstRequired);
    first.addProperty("title", "from-first");

    JsonObject second = objectSchema("second");
    JsonArray secondRequired = new JsonArray();
    secondRequired.add("second");
    secondRequired.add("first");
    second.add("required", secondRequired);
    second.addProperty("description", "merged");

    allOf.add(first);
    allOf.add(second);
    schema.add("allOf", allOf);

    JsonObject merged = resolver.resolve(schema, root).getAsJsonObject();

    assertEquals("local", merged.get("title").getAsString());
    assertEquals("merged", merged.get("description").getAsString());
    assertTrue(merged.getAsJsonObject("properties").has("first"));
    assertTrue(merged.getAsJsonObject("properties").has("second"));
    assertEquals(2, merged.getAsJsonArray("required").size());
  }

  @Test
  void oneOfAndAnyOfUseFirstAlternative() {
    JsonObject root = new JsonObject();

    JsonObject oneOfSchema = new JsonObject();
    JsonArray oneOf = new JsonArray();
    oneOf.add(typeSchema("string"));
    oneOf.add(typeSchema("integer"));
    oneOfSchema.add("oneOf", oneOf);

    JsonObject anyOfSchema = new JsonObject();
    JsonArray anyOf = new JsonArray();
    anyOf.add(typeSchema("boolean"));
    anyOf.add(typeSchema("number"));
    anyOfSchema.add("anyOf", anyOf);

    assertEquals("string", resolver.getType(resolver.resolve(oneOfSchema, root).getAsJsonObject()));
    assertEquals("boolean", resolver.getType(resolver.resolve(anyOfSchema, root).getAsJsonObject()));
  }

  @Test
  void nonObjectValuesAndTypeLookupAreHandledSafely() {
    JsonPrimitive primitive = new JsonPrimitive("value");

    assertSame(primitive, resolver.resolve(primitive, new JsonObject()));
    assertNull(resolver.resolve(null, new JsonObject()));
    assertNull(resolver.getType(null));
    assertNull(resolver.getType(new JsonObject()));

    JsonObject nonPrimitiveType = new JsonObject();
    nonPrimitiveType.add("type", new JsonArray());
    assertNull(resolver.getType(nonPrimitiveType));
  }

  @Test
  void coercionPreservesObjectSchemasAndCreatesEmptyObjectForScalars() {
    JsonObject object = objectSchema("value");
    JsonObject scalar = typeSchema("string");

    assertSame(object, resolver.coerceToObjectSchema(object, object));

    JsonObject coerced = resolver.coerceToObjectSchema(scalar, scalar);
    assertEquals("object", coerced.get("type").getAsString());
    assertTrue(coerced.getAsJsonObject("properties").entrySet().isEmpty());
  }

  private static JsonObject objectSchema(String property) {
    JsonObject schema = typeSchema("object");
    JsonObject properties = new JsonObject();
    properties.add(property, typeSchema("string"));
    schema.add("properties", properties);
    return schema;
  }

  private static JsonObject typeSchema(String type) {
    JsonObject schema = new JsonObject();
    schema.addProperty("type", type);
    return schema;
  }
}
