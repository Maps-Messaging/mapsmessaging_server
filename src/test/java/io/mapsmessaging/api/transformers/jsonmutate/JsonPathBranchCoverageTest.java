package io.mapsmessaging.api.transformers.jsonmutate;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonPathBranchCoverageTest {

  @Test
  void malformedArraySyntaxIsTreatedAsLiteralObjectKey() {
    JsonObject root = new JsonObject();
    root.addProperty("items[not-an-index]", "literal");
    root.addProperty("unterminated[2", "also-literal");

    assertEquals("literal", JsonPath.get(root, "items[not-an-index]").getAsString());
    assertEquals("also-literal", JsonPath.get(root, "unterminated[2").getAsString());
  }

  @Test
  void setDoesNotOverwriteScalarWhenTraversalRequiresObjectOrArray() {
    JsonObject root = new JsonObject();
    root.addProperty("scalar", 1);
    JsonObject nested = new JsonObject();
    nested.addProperty("list", "not-an-array");
    root.add("nested", nested);

    JsonPath.set(root, "scalar.child", new JsonPrimitive(2));
    JsonPath.set(root, "nested.list[0].value", new JsonPrimitive(3));

    assertEquals(1, root.get("scalar").getAsInt());
    assertEquals("not-an-array", root.getAsJsonObject("nested").get("list").getAsString());
  }

  @Test
  void removeRejectsInvalidTraversalAndOutOfRangeIndexesWithoutMutation() {
    JsonObject root = new JsonObject();
    JsonArray array = new JsonArray();
    array.add(new JsonPrimitive("a"));
    root.add("items", array);

    JsonPath.remove(root, "items[9]");
    JsonPath.remove(root, "items[0].missing.child");

    assertEquals(1, array.size());
    assertEquals("a", array.get(0).getAsString());
  }

  @Test
  void settingSparseFinalIndexPadsWithJsonNull() {
    JsonObject root = new JsonObject();

    JsonPath.set(root, "items[3]", new JsonPrimitive("x"));

    JsonArray array = root.getAsJsonArray("items");
    assertEquals(4, array.size());
    assertSame(JsonNull.INSTANCE, array.get(0));
    assertEquals("x", array.get(3).getAsString());
  }
}