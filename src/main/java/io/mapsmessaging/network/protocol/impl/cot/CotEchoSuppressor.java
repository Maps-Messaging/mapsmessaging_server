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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
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
  static final String HOP_ATTRIBUTE = "hop";
  private static final String MARKER_PREFIX = "maps";

  private CotEchoSuppressor() {
  }

  static byte[] mark(byte[] xml, String origin) throws IOException {
    return mark(xml, origin, 64);
  }

  static byte[] mark(byte[] xml, String origin, int maximumDepth) throws IOException {
    requireOrigin(origin);
    CotEventInfo info = inspect(xml, maximumDepth);
    if (info.origins().contains(origin)) {
      return xml;
    }
    try (ByteArrayInputStream input = new ByteArrayInputStream(xml);
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      XMLStreamReader reader = newInputFactory()
          .createXMLStreamReader(input, StandardCharsets.UTF_8.name());
      XMLStreamWriter writer = XMLOutputFactory.newFactory()
          .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
      try {
        copyWithMarker(reader, writer, origin, info.hopCount() + 1, maximumDepth);
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
    try {
      return inspect(xml, 64).origins().contains(requireOrigin(origin));
    } catch (IOException exception) {
      return false;
    }
  }

  static CotEventInfo inspect(byte[] xml, int maximumDepth) throws IOException {
    if (xml == null || xml.length == 0) {
      throw new IOException("CoT XML cannot be empty");
    }
    if (maximumDepth < 4) {
      throw new IllegalArgumentException("maximumDepth must be at least 4");
    }
    try (ByteArrayInputStream input = new ByteArrayInputStream(xml)) {
      XMLStreamReader reader = newInputFactory()
          .createXMLStreamReader(input, StandardCharsets.UTF_8.name());
      try {
        return inspect(reader, maximumDepth);
      } finally {
        reader.close();
      }
    } catch (XMLStreamException exception) {
      throw new IOException("Unable to parse CoT XML", exception);
    }
  }

  private static CotEventInfo inspect(XMLStreamReader reader, int maximumDepth)
      throws XMLStreamException, IOException {
    MessageDigest digest = newDigest();
    String uid = null;
    String type = null;
    Instant time = null;
    Instant start = null;
    Instant stale = null;
    List<String> origins = new ArrayList<>();
    int hopCount = 0;
    int depth = 0;
    int markerDepth = -1;
    boolean eventSeen = false;
    while (reader.hasNext()) {
      int token = reader.next();
      if (token == XMLStreamConstants.DTD || token == XMLStreamConstants.ENTITY_REFERENCE) {
        throw new IOException("DTD and entity references are not permitted in CoT XML");
      }
      if (token == XMLStreamConstants.START_ELEMENT) {
        depth++;
        if (depth > maximumDepth) {
          throw new IOException("CoT XML exceeds maximum nesting depth " + maximumDepth);
        }
        if (isMarker(reader)) {
          String markerOrigin = reader.getAttributeValue(null, MARKER_ATTRIBUTE);
          if (markerOrigin != null && !markerOrigin.isBlank()) {
            origins.add(markerOrigin);
          }
          hopCount = Math.max(hopCount, parseHop(reader, origins.size()));
          markerDepth = depth;
          continue;
        }
        if (markerDepth < 0) {
          updateDigestForStart(reader, digest);
        }
        if (depth == 1) {
          if (!"event".equals(reader.getLocalName())) {
            throw new IOException("CoT document root must be event");
          }
          eventSeen = true;
          uid = requiredAttribute(reader, "uid");
          type = requiredAttribute(reader, "type");
          time = parseTime(reader, "time");
          start = parseTime(reader, "start");
          stale = parseTime(reader, "stale");
        }
      } else if (token == XMLStreamConstants.END_ELEMENT) {
        if (markerDepth < 0) {
          updateDigest(digest, "E", reader.getNamespaceURI(), reader.getLocalName());
        }
        if (depth == markerDepth) {
          markerDepth = -1;
        }
        depth--;
      } else if ((token == XMLStreamConstants.CHARACTERS || token == XMLStreamConstants.CDATA)
          && markerDepth < 0 && !reader.isWhiteSpace()) {
        updateDigest(digest, "T", reader.getText());
      }
    }
    if (!eventSeen || depth != 0) {
      throw new IOException("Incomplete CoT event");
    }
    return new CotEventInfo(
        uid,
        type,
        time,
        start,
        stale,
        HexFormat.of().formatHex(digest.digest()),
        List.copyOf(origins),
        Math.max(hopCount, origins.size()),
        classify(type));
  }

  private static void copyWithMarker(
      XMLStreamReader reader,
      XMLStreamWriter writer,
      String origin,
      int hop,
      int maximumDepth) throws XMLStreamException, IOException {
    int depth = 0;
    boolean detailFound = false;
    while (reader.hasNext()) {
      int event = reader.next();
      switch (event) {
        case XMLStreamConstants.START_ELEMENT -> {
          depth++;
          if (depth > maximumDepth) {
            throw new IOException("CoT XML exceeds maximum nesting depth " + maximumDepth);
          }
          writeStartElement(reader, writer);
          if (depth == 2 && "detail".equals(reader.getLocalName())) {
            detailFound = true;
          }
        }
        case XMLStreamConstants.END_ELEMENT -> {
          if (depth == 2 && "detail".equals(reader.getLocalName())) {
            writeMarker(writer, origin, hop);
          } else if (depth == 1 && "event".equals(reader.getLocalName()) && !detailFound) {
            writer.writeStartElement("detail");
            writeMarker(writer, origin, hop);
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
        case XMLStreamConstants.DTD, XMLStreamConstants.ENTITY_REFERENCE ->
            throw new IOException("DTD and entity references are not permitted in CoT XML");
        default -> {
          // The stream header is supplied by CotStreamEncoder.
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

  private static void writeMarker(XMLStreamWriter writer, String origin, int hop)
      throws XMLStreamException {
    writer.writeEmptyElement(MARKER_PREFIX, MARKER_ELEMENT, MARKER_NAMESPACE);
    writer.writeNamespace(MARKER_PREFIX, MARKER_NAMESPACE);
    writer.writeAttribute(MARKER_ATTRIBUTE, origin);
    writer.writeAttribute(HOP_ATTRIBUTE, Integer.toString(hop));
  }

  private static void updateDigestForStart(XMLStreamReader reader, MessageDigest digest) {
    updateDigest(digest, "S", reader.getNamespaceURI(), reader.getLocalName());
    List<String> attributes = new ArrayList<>();
    for (int index = 0; index < reader.getAttributeCount(); index++) {
      attributes.add(value(reader.getAttributeNamespace(index))
          + ':' + reader.getAttributeLocalName(index)
          + '=' + reader.getAttributeValue(index));
    }
    attributes.sort(Comparator.naturalOrder());
    for (String attribute : attributes) {
      updateDigest(digest, "A", attribute);
    }
  }

  private static void updateDigest(MessageDigest digest, String... values) {
    for (String item : values) {
      byte[] bytes = value(item).getBytes(StandardCharsets.UTF_8);
      digest.update((byte) (bytes.length >>> 24));
      digest.update((byte) (bytes.length >>> 16));
      digest.update((byte) (bytes.length >>> 8));
      digest.update((byte) bytes.length);
      digest.update(bytes);
    }
  }

  private static MessageDigest newDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private static int parseHop(XMLStreamReader reader, int fallback) throws IOException {
    String value = reader.getAttributeValue(null, HOP_ATTRIBUTE);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      int hop = Integer.parseInt(value);
      if (hop < 1) {
        throw new IOException("CoT origin hop must be positive");
      }
      return hop;
    } catch (NumberFormatException exception) {
      throw new IOException("Invalid CoT origin hop", exception);
    }
  }

  private static String requiredAttribute(XMLStreamReader reader, String name)
      throws IOException {
    String value = reader.getAttributeValue(null, name);
    if (value == null || value.isBlank()) {
      throw new IOException("Missing CoT attribute " + name);
    }
    return value;
  }

  private static Instant parseTime(XMLStreamReader reader, String name) throws IOException {
    String value = requiredAttribute(reader, name);
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (DateTimeParseException exception) {
      throw new IOException("Invalid CoT date-time attribute " + name, exception);
    }
  }

  private static CotEventClass classify(String type) {
    if (type.startsWith("a-")) {
      return CotEventClass.STATE;
    }
    if (type.startsWith("b-t-f")) {
      return CotEventClass.CHAT;
    }
    if (type.startsWith("b-a") || type.contains("emergency")) {
      return CotEventClass.ALERT;
    }
    if (type.startsWith("t-x-d-d")) {
      return CotEventClass.DELETE;
    }
    if (type.startsWith("t-x") || type.startsWith("b-f-t")) {
      return CotEventClass.CONTROL;
    }
    if (type.startsWith("b-m-p") || type.startsWith("u-")) {
      return CotEventClass.OBJECT;
    }
    return CotEventClass.UNSUPPORTED;
  }

  private static boolean isMarker(XMLStreamReader reader) {
    return MARKER_ELEMENT.equals(reader.getLocalName())
        && MARKER_NAMESPACE.equals(reader.getNamespaceURI());
  }

  private static XMLInputFactory newInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
    factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
    factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
      throw new XMLStreamException("External XML resources are not permitted");
    });
    return factory;
  }

  private static String requireOrigin(String origin) {
    if (origin == null || origin.isBlank()) {
      throw new IllegalArgumentException("CoT echo-suppression origin must not be blank");
    }
    return origin;
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }

  enum CotEventClass {
    STATE,
    OBJECT,
    CHAT,
    ALERT,
    CONTROL,
    DELETE,
    UNSUPPORTED
  }

  record CotEventInfo(
      String uid,
      String type,
      Instant time,
      Instant start,
      Instant stale,
      String fingerprint,
      List<String> origins,
      int hopCount,
      CotEventClass eventClass) {

    boolean coalescible() {
      return eventClass == CotEventClass.STATE || eventClass == CotEventClass.OBJECT;
    }

    boolean highValue() {
      return eventClass == CotEventClass.ALERT
          || eventClass == CotEventClass.CHAT
          || eventClass == CotEventClass.CONTROL
          || eventClass == CotEventClass.DELETE;
    }
  }
}
