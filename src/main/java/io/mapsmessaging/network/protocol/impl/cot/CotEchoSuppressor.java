/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

final class CotEchoSuppressor {

  static final String MARKER_NAMESPACE = "https://mapsmessaging.io/cot";
  static final String MARKER_ELEMENT = "origin";
  static final String MARKER_ATTRIBUTE = "uid";
  private static final String MARKER_PREFIX = "maps";

  private CotEchoSuppressor() {
  }

  static byte[] mark(byte[] xml, String origin) throws IOException {
    requireOrigin(origin);
    XMLInputFactory inputFactory = newInputFactory();
    try (ByteArrayInputStream input = new ByteArrayInputStream(xml);
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      XMLStreamReader reader = inputFactory.createXMLStreamReader(input, StandardCharsets.UTF_8.name());
      XMLStreamWriter writer = XMLOutputFactory.newFactory()
          .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
      try {
        copyWithMarker(reader, writer, origin);
        writer.flush();
        return output.toByteArray();
      } finally {
        reader.close();
        writer.close();
      }
    } catch (XMLStreamException exception) {
      throw new IOException("Unable to add CoT echo-suppression marker", exception);
    }
  }

  static boolean isEcho(byte[] xml, String origin) {
    requireOrigin(origin);
    try (ByteArrayInputStream input = new ByteArrayInputStream(xml)) {
      XMLStreamReader reader = newInputFactory()
          .createXMLStreamReader(input, StandardCharsets.UTF_8.name());
      try {
        while (reader.hasNext()) {
          if (reader.next() == XMLStreamConstants.START_ELEMENT
              && isMarker(reader)
              && origin.equals(reader.getAttributeValue(null, MARKER_ATTRIBUTE))) {
            return true;
          }
        }
        return false;
      } finally {
        reader.close();
      }
    } catch (XMLStreamException exception) {
      return false;
    }
  }

  private static void copyWithMarker(
      XMLStreamReader reader,
      XMLStreamWriter writer,
      String origin) throws XMLStreamException {
    int depth = 0;
    boolean detailFound = false;
    boolean ownMarkerFound = false;
    while (reader.hasNext()) {
      int event = reader.next();
      switch (event) {
        case XMLStreamConstants.START_ELEMENT -> {
          writeStartElement(reader, writer);
          if (depth == 1 && "detail".equals(reader.getLocalName())) {
            detailFound = true;
          }
          if (isMarker(reader)
              && origin.equals(reader.getAttributeValue(null, MARKER_ATTRIBUTE))) {
            ownMarkerFound = true;
          }
          depth++;
        }
        case XMLStreamConstants.END_ELEMENT -> {
          if (depth == 2 && "detail".equals(reader.getLocalName()) && !ownMarkerFound) {
            writeMarker(writer, origin);
            ownMarkerFound = true;
          } else if (depth == 1 && "event".equals(reader.getLocalName()) && !detailFound) {
            writer.writeStartElement("detail");
            writeMarker(writer, origin);
            writer.writeEndElement();
          }
          writer.writeEndElement();
          depth--;
        }
        case XMLStreamConstants.CHARACTERS, XMLStreamConstants.SPACE ->
            writer.writeCharacters(reader.getText());
        case XMLStreamConstants.CDATA -> writer.writeCData(reader.getText());
        case XMLStreamConstants.COMMENT -> writer.writeComment(reader.getText());
        case XMLStreamConstants.PROCESSING_INSTRUCTION ->
            writer.writeProcessingInstruction(reader.getPITarget(), reader.getPIData());
        case XMLStreamConstants.ENTITY_REFERENCE ->
            writer.writeEntityRef(reader.getLocalName());
        default -> {
          // The stream header is supplied by CotStreamEncoder; DTDs are disabled.
        }
      }
    }
  }

  private static void writeStartElement(XMLStreamReader reader, XMLStreamWriter writer)
      throws XMLStreamException {
    String namespace = reader.getNamespaceURI();
    String prefix = reader.getPrefix();
    if (namespace == null || namespace.isEmpty()) {
      writer.writeStartElement(reader.getLocalName());
    } else {
      writer.writeStartElement(prefix == null ? "" : prefix, reader.getLocalName(), namespace);
    }
    for (int i = 0; i < reader.getNamespaceCount(); i++) {
      String namespacePrefix = reader.getNamespacePrefix(i);
      String namespaceUri = reader.getNamespaceURI(i);
      if (namespacePrefix == null) {
        writer.writeDefaultNamespace(namespaceUri);
      } else {
        writer.writeNamespace(namespacePrefix, namespaceUri);
      }
    }
    for (int i = 0; i < reader.getAttributeCount(); i++) {
      String attributeNamespace = reader.getAttributeNamespace(i);
      if (attributeNamespace == null || attributeNamespace.isEmpty()) {
        writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
      } else {
        String attributePrefix = reader.getAttributePrefix(i);
        writer.writeAttribute(
            attributePrefix == null ? "" : attributePrefix,
            attributeNamespace,
            reader.getAttributeLocalName(i),
            reader.getAttributeValue(i));
      }
    }
  }

  private static void writeMarker(XMLStreamWriter writer, String origin)
      throws XMLStreamException {
    writer.writeEmptyElement(MARKER_PREFIX, MARKER_ELEMENT, MARKER_NAMESPACE);
    writer.writeNamespace(MARKER_PREFIX, MARKER_NAMESPACE);
    writer.writeAttribute(MARKER_ATTRIBUTE, origin);
  }

  private static boolean isMarker(XMLStreamReader reader) {
    return MARKER_ELEMENT.equals(reader.getLocalName())
        && MARKER_NAMESPACE.equals(reader.getNamespaceURI());
  }

  private static XMLInputFactory newInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
    return factory;
  }

  private static void requireOrigin(String origin) {
    if (origin == null || origin.isBlank()) {
      throw new IllegalArgumentException("CoT echo-suppression origin must not be blank");
    }
  }
}
