package io.mapsmessaging.tools.config.yaml;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaResolverFinalCoverageTest {

  private final SchemaResolver resolver = new SchemaResolver();

  @Test
  void nullJsonNullAndPrimitiveValuesResolveWithoutTransformation() {
    assertNull(resolver.resolve(null, new JsonObject()));
    assertSame(JsonNull.INSTANCE, resolver.resolve(JsonNull.INSTANCE, new JsonObject()));

    JsonPrimitive primitive = new JsonPrimitive(3);
    assertSame(primitive, resolver.resolve(primitive, new JsonObject()));
  }

  @Test
  void emptyOneOfAndAnyOfArraysFallBackToOriginalSchemaObject() {
    JsonObject one = new JsonObject();
    one.add("oneOf", new JsonArray());
    JsonObject any = new JsonObject();
    any.add("anyOf", new JsonArray());

    assertSame(one, resolver.resolve(one, one));
    assertSame(any, resolver.resolve(any, any));
  }

  @Test
  void allOfIgnoresNonObjectMembersAndRetainsOuterSimpleKeys() {
    JsonObject schema = new JsonObject();
    schema.addProperty("title", "outer");
    JsonArray allOf = new JsonArray();
    allOf.add("not-an-object");

    JsonObject object = new JsonObject();
    object.addProperty("type", "object");
    JsonObject properties = new JsonObject();
    properties.add("x", type("string"));
    object.add("properties", properties);
    allOf.add(object);
    schema.add("allOf", allOf);

    JsonObject resolved = resolver.resolve(schema, schema).getAsJsonObject();

    assertEquals("outer", resolved.get("title").getAsString());
    assertTrue(resolved.getAsJsonObject("properties").has("x"));
  }

  @Test
  void objectSchemasAreReturnedUnchangedWhileScalarSchemasAreCoerced() {
    JsonObject object = type("object");
    JsonObject properties = new JsonObject();
    object.add("properties", properties);

    assertSame(object, resolver.coerceToObjectSchema(object, object));

    JsonObject implicitObject = new JsonObject();
    implicitObject.add("properties", new JsonObject());
    assertSame(
        implicitObject,
        resolver.coerceToObjectSchema(implicitObject, implicitObject));

    JsonObject coerced = resolver.coerceToObjectSchema(type("integer"), new JsonObject());
    assertEquals("object", coerced.get("type").getAsString());
  }

  private static JsonObject type(String name) {
    JsonObject object = new JsonObject();
    object.addProperty("type", name);
    return object;
  }
}