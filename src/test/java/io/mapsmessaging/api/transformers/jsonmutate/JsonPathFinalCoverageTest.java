package io.mapsmessaging.api.transformers.jsonmutate;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonPathFinalCoverageTest {

  @Test
  void getRejectsTraversalThroughScalarAndNonArrayValues() {
    JsonObject root = new JsonObject();
    root.addProperty("scalar", 1);
    root.addProperty("items", "not-array");

    assertNull(JsonPath.get(root, "scalar.child"));
    assertNull(JsonPath.get(root, "items[0]"));
    assertNull(JsonPath.get(root, "missing.value"));
  }

  @Test
  void setCreatesMissingNestedObjectsThroughNullValues() {
    JsonObject root = new JsonObject();
    root.add("vehicle", JsonNull.INSTANCE);

    JsonPath.set(root, "vehicle.position.lat", new JsonPrimitive(38.4));

    assertEquals(
        38.4,
        JsonPath.get(root, "vehicle.position.lat").getAsDouble(),
        0.0);
  }

  @Test
  void setCreatesIntermediateArrayAndObjectForIndexedTraversal() {
    JsonObject root = new JsonObject();

    JsonPath.set(root, "sensors[1].reading.value", new JsonPrimitive(7));

    JsonArray sensors = root.getAsJsonArray("sensors");
    assertEquals(2, sensors.size());
    assertSame(JsonNull.INSTANCE, sensors.get(0));
    assertEquals(
        7,
        JsonPath.get(root, "sensors[1].reading.value").getAsInt());
  }

  @Test
  void parserSkipsBlankDotSegmentsAndTreatsBlankArrayKeyAsLiteral() {
    JsonObject root = new JsonObject();
    JsonPath.set(root, "a..b", new JsonPrimitive("value"));
    root.addProperty("[0]", "literal");

    assertEquals("value", JsonPath.get(root, "a..b").getAsString());
    assertEquals("literal", JsonPath.get(root, "[0]").getAsString());
  }
}