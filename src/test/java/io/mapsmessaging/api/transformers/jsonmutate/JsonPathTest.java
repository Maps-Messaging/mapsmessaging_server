package io.mapsmessaging.api.transformers.jsonmutate;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonPathTest {

  @Test
  void setCreatesNestedObjectsAndArraysThenGetResolvesValue() {
    JsonObject root = new JsonObject();

    JsonPath.set(root, "vehicle.sensors[2].name", new JsonPrimitive("thermal"));

    assertEquals(
        "thermal",
        JsonPath.get(root, "vehicle.sensors[2].name").getAsString());
    assertTrue(JsonPath.get(root, "vehicle.sensors[0]").isJsonNull());
    assertNull(JsonPath.get(root, "vehicle.sensors[9].name"));
  }

  @Test
  void removeObjectPropertyAndArrayEntryAreSafe() {
    JsonObject root = new JsonObject();
    JsonPath.set(root, "a.b", new JsonPrimitive(1));
    JsonPath.set(root, "a.list[1]", new JsonPrimitive("x"));

    JsonPath.remove(root, "a.b");
    JsonPath.remove(root, "a.list[1]");

    assertNull(JsonPath.get(root, "a.b"));
    assertSame(JsonNull.INSTANCE, JsonPath.get(root, "a.list[1]"));
  }

  @Test
  void malformedAndMissingPathsDoNotDamageDocument() {
    JsonObject root = new JsonObject();
    root.addProperty("existing", "value");

    JsonPath.set(root, "", new JsonPrimitive("x"));
    JsonPath.remove(root, "missing[99]");
    assertNull(JsonPath.get(root, null));
    assertEquals("value", root.get("existing").getAsString());
  }
}
