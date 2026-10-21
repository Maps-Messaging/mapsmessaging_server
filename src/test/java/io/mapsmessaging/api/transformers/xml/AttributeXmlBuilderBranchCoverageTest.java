package io.mapsmessaging.api.transformers.xml;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AttributeXmlBuilderBranchCoverageTest {

  @Test
  void rejectsNullEmptyMultipleRootsAndPrimitiveRoot() {
    assertThrows(IOException.class, () -> AttributeXmlBuilder.buildXml(null));
    assertThrows(IOException.class, () -> AttributeXmlBuilder.buildXml(new JsonObject()));

    JsonObject multiple = new JsonObject();
    multiple.add("a", new JsonObject());
    multiple.add("b", new JsonObject());
    assertThrows(IOException.class, () -> AttributeXmlBuilder.buildXml(multiple));

    JsonObject primitive = new JsonObject();
    primitive.addProperty("root", "value");
    assertThrows(IOException.class, () -> AttributeXmlBuilder.buildXml(primitive));
  }

  @Test
  void rendersAttributesTextNestedObjectsAndMixedArrays() throws Exception {
    JsonObject body = new JsonObject();
    body.addProperty("@id", 7);
    body.addProperty("text", "hello");
    body.addProperty("enabled", true);
    body.add("ignored", JsonNull.INSTANCE);

    JsonObject child = new JsonObject();
    child.addProperty("@kind", "nested");
    child.addProperty("value", 3.5);
    body.add("child", child);

    JsonArray values = new JsonArray();
    values.add("one");
    JsonObject objectValue = new JsonObject();
    objectValue.addProperty("@code", "two");
    values.add(objectValue);
    JsonArray nested = new JsonArray();
    nested.add(1);
    nested.add(false);
    values.add(nested);
    values.add(JsonNull.INSTANCE);
    body.add("value", values);

    JsonObject root = new JsonObject();
    root.add("root", body);

    String xml = AttributeXmlBuilder.buildXmlString(root);

    assertTrue(xml.contains("<root id=\"7\">"));
    assertTrue(xml.contains("hello"));
    assertTrue(xml.contains("<enabled>true</enabled>"));
    assertTrue(xml.contains("<child kind=\"nested\"><value>3.5</value></child>"));
    assertTrue(xml.contains("<value>one</value>"));
    assertTrue(xml.contains("code=\"two\""));
    assertTrue(xml.contains("<item>1</item>"));
    assertTrue(xml.contains("<item>false</item>"));
  }
}
