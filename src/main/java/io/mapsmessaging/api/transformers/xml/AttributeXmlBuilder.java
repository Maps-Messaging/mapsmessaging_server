package io.mapsmessaging.api.transformers.xml;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class AttributeXmlBuilder {

  private AttributeXmlBuilder() {
  }

  public static byte[] buildXml(JsonObject jsonObject) throws IOException {
    if (jsonObject == null || jsonObject.isEmpty()) {
      throw new IOException("JsonObject must not be null or empty");
    }

    if (jsonObject.entrySet().size() != 1) {
      throw new IOException("JsonObject must contain exactly one root element");
    }

    Map.Entry<String, JsonElement> rootEntry = jsonObject.entrySet().iterator().next();
    if (!rootEntry.getValue().isJsonObject()) {
      throw new IOException("Root element '" + rootEntry.getKey() + "' must be a JsonObject");
    }

    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document document = documentBuilder.newDocument();

      Element rootElement = buildElement(document, rootEntry.getKey(), rootEntry.getValue().getAsJsonObject());
      document.appendChild(rootElement);

      return toBytes(document);
    } catch (ParserConfigurationException | TransformerException e) {
      throw new IOException("Failed to build XML from JsonObject", e);
    }
  }

  private static Element buildElement(Document document, String elementName, JsonObject jsonObject) {
    Element element = document.createElement(elementName);

    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
      String fieldName = entry.getKey();
      JsonElement value = entry.getValue();

      if (value == null || value.isJsonNull()) {
        continue;
      }

      if ("text".equals(fieldName) && value.isJsonPrimitive()) {
        element.setTextContent(asString(value.getAsJsonPrimitive()));
      } else if (value.isJsonPrimitive()) {
        element.setAttribute(fieldName, asString(value.getAsJsonPrimitive()));
      } else if (value.isJsonObject()) {
        Element childElement = buildElement(document, fieldName, value.getAsJsonObject());
        element.appendChild(childElement);
      } else if (value.isJsonArray()) {
        appendArray(document, element, fieldName, value.getAsJsonArray());
      }
    }

    return element;
  }

  private static void appendArray(Document document, Element parentElement, String fieldName, JsonArray jsonArray) {
    for (JsonElement arrayEntry : jsonArray) {
      if (arrayEntry == null || arrayEntry.isJsonNull()) {
        continue;
      }

      if (arrayEntry.isJsonObject()) {
        Element childElement = buildElement(document, fieldName, arrayEntry.getAsJsonObject());
        parentElement.appendChild(childElement);
      } else if (arrayEntry.isJsonPrimitive()) {
        Element childElement = document.createElement(fieldName);
        childElement.setTextContent(asString(arrayEntry.getAsJsonPrimitive()));
        parentElement.appendChild(childElement);
      } else if (arrayEntry.isJsonArray()) {
        Element childElement = document.createElement(fieldName);
        appendNestedArray(document, childElement, "item", arrayEntry.getAsJsonArray());
        parentElement.appendChild(childElement);
      }
    }
  }

  private static void appendNestedArray(Document document, Element parentElement, String itemName, JsonArray jsonArray) {
    for (JsonElement arrayEntry : jsonArray) {
      if (arrayEntry == null || arrayEntry.isJsonNull()) {
        continue;
      }

      if (arrayEntry.isJsonObject()) {
        Element childElement = buildElement(document, itemName, arrayEntry.getAsJsonObject());
        parentElement.appendChild(childElement);
      } else if (arrayEntry.isJsonPrimitive()) {
        Element childElement = document.createElement(itemName);
        childElement.setTextContent(asString(arrayEntry.getAsJsonPrimitive()));
        parentElement.appendChild(childElement);
      } else if (arrayEntry.isJsonArray()) {
        Element childElement = document.createElement(itemName);
        appendNestedArray(document, childElement, "item", arrayEntry.getAsJsonArray());
        parentElement.appendChild(childElement);
      }
    }
  }

  private static String asString(JsonPrimitive jsonPrimitive) {
    if (jsonPrimitive.isBoolean()) {
      return Boolean.toString(jsonPrimitive.getAsBoolean());
    }

    if (jsonPrimitive.isNumber()) {
      return jsonPrimitive.getAsNumber().toString();
    }

    return jsonPrimitive.getAsString();
  }

  private static byte[] toBytes(Document document) throws TransformerException {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    transformer.setOutputProperty(OutputKeys.INDENT, "no");

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    transformer.transform(new DOMSource(document), new StreamResult(byteArrayOutputStream));
    return byteArrayOutputStream.toByteArray();
  }

  public static String buildXmlString(JsonObject jsonObject) throws IOException {
    return new String(buildXml(jsonObject), StandardCharsets.UTF_8);
  }
}