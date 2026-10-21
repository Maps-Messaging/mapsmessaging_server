package io.mapsmessaging.tools.config.yaml;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaResolverBranchCoverageTest {

  private final SchemaResolver resolver = new SchemaResolver();

  @Test
  void localRefsResolveRecursivelyAndMissingOrExternalRefsFallBackToOriginalObject() {
    JsonObject root = new JsonObject();
    JsonObject defs = new JsonObject();
    JsonObject target = new JsonObject();
    target.addProperty("type", "string");
    defs.add("name", target);
    root.add("$defs", defs);

    JsonObject local = new JsonObject();
    local.addProperty("$ref", "#/$defs/name");
    assertEquals("string", resolver.getType(resolver.resolve(local, root).getAsJsonObject()));

    JsonObject missing = new JsonObject();
    missing.addProperty("$ref", "#/$defs/missing");
    assertSame(missing, resolver.resolve(missing, root));

    JsonObject external = new JsonObject();
    external.addProperty("$ref", "https://example/schema");
    assertSame(external, resolver.resolve(external, root));
  }

  @Test
  void oneOfAndAnyOfUseFirstAlternativeWhenPresent() {
    JsonObject one = new JsonObject();
    JsonArray oneOf = new JsonArray();
    oneOf.add(type("integer"));
    oneOf.add(type("string"));
    one.add("oneOf", oneOf);

    JsonObject any = new JsonObject();
    JsonArray anyOf = new JsonArray();
    anyOf.add(type("boolean"));
    any.add("anyOf", anyOf);

    assertEquals("integer", resolver.getType(resolver.resolve(one, one).getAsJsonObject()));
    assertEquals("boolean", resolver.getType(resolver.resolve(any, any).getAsJsonObject()));
  }

  @Test
  void allOfMergesPropertiesRequiredAndMissingSimpleKeysWithoutOverwritingExistingValues() {
    JsonObject schema = new JsonObject();
    schema.addProperty("title", "outer");
    JsonArray allOf = new JsonArray();

    JsonObject first = type("object");
    JsonObject firstProps = new JsonObject();
    firstProps.add("a", type("string"));
    first.add("properties", firstProps);
    JsonArray firstRequired = new JsonArray();
    firstRequired.add("a");
    first.add("required", firstRequired);
    first.addProperty("title", "inner");
    allOf.add(first);

    JsonObject second = type("object");
    JsonObject secondProps = new JsonObject();
    secondProps.add("b", type("integer"));
    second.add("properties", secondProps);
    JsonArray secondRequired = new JsonArray();
    secondRequired.add("a");
    secondRequired.add("b");
    second.add("required", secondRequired);
    allOf.add(second);

    schema.add("allOf", allOf);

    JsonObject merged = resolver.resolve(schema, schema).getAsJsonObject();

    assertEquals("outer", merged.get("title").getAsString());
    assertTrue(merged.getAsJsonObject("properties").has("a"));
    assertTrue(merged.getAsJsonObject("properties").has("b"));
    assertEquals(2, merged.getAsJsonArray("required").size());
  }

  @Test
  void typeAndObjectCoercionRejectMalformedMetadata() {
    assertNull(resolver.getType(null));

    JsonObject nullType = new JsonObject();
    nullType.add("type", JsonNull.INSTANCE);
    assertNull(resolver.getType(nullType));

    JsonObject objectType = new JsonObject();
    objectType.add("type", new JsonObject());
    assertNull(resolver.getType(objectType));

    JsonObject scalar = type("string");
    JsonObject coerced = resolver.coerceToObjectSchema(scalar, scalar);
    assertEquals("object", resolver.getType(coerced));
    assertTrue(coerced.has("properties"));

    JsonPrimitive primitive = new JsonPrimitive("x");
    assertSame(primitive, resolver.resolve(primitive, new JsonObject()));
  }

  private static JsonObject type(String type) {
    JsonObject object = new JsonObject();
    object.addProperty("type", type);
    return object;
  }
}