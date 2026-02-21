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

package io.mapsmessaging.network.protocol.impl.tak.codec;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.schemas.config.impl.XmlSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.selector.IdentifierResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public class CotXmlCodec implements TakPayloadCodec {

  private final MessageFormatter xmlFormatter;
  private final String schemaId;

  public CotXmlCodec() {
    this(null, SchemaManager.DEFAULT_XML_SCHEMA.toString());
  }

  public CotXmlCodec(MessageFormatter xmlFormatter) {
    this(xmlFormatter, SchemaManager.DEFAULT_XML_SCHEMA.toString());
  }

  public CotXmlCodec(MessageFormatter xmlFormatter, String schemaId) {
    this.xmlFormatter = xmlFormatter;
    this.schemaId = schemaId;
  }

  public static CotXmlCodec withSchemaFormatter(boolean namespaceAware, boolean coalescing, boolean validating, String rootEntry) {
    return withSchemaFormatter(namespaceAware, coalescing, validating, rootEntry, SchemaManager.DEFAULT_XML_SCHEMA.toString());
  }

  public static CotXmlCodec withSchemaFormatter(boolean namespaceAware, boolean coalescing, boolean validating, String rootEntry, String schemaId) {
    return new CotXmlCodec(createFormatter(namespaceAware, coalescing, validating, rootEntry), schemaId);
  }

  @Override
  public Message decode(byte[] payload) throws IOException {
    String xml = new String(payload, StandardCharsets.UTF_8);
    Element event = parseRootEvent(xml);
    Element point = extractPoint(event);

    String uid = required(event, "uid");
    String type = required(event, "type");
    String time = required(event, "time");
    String start = required(event, "start");
    String stale = required(event, "stale");
    String how = required(event, "how");
    String lat = required(point, "lat");
    String lon = required(point, "lon");

    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("tak.uid", uid);
    meta.put("tak.type", type);
    meta.put("tak.time", time);
    meta.put("tak.start", start);
    meta.put("tak.stale", stale);
    meta.put("tak.how", how);
    meta.put("tak.lat", lat);
    meta.put("tak.lon", lon);
    putIfPresent(meta, "tak.hae", point.getAttribute("hae"));
    putIfPresent(meta, "tak.ce", point.getAttribute("ce"));
    putIfPresent(meta, "tak.le", point.getAttribute("le"));
    meta.put("tak.format", "xml");
    meta.put("tak.transport", "stream");
    mergeSchemaParsedMeta(meta, payload);
    addSelectorAliases(meta);

    MessageBuilder builder = new MessageBuilder()
        .setOpaqueData(payload)
        .setMeta(meta)
        .setContentType("application/cot+xml")
        .setQoS(QualityOfService.AT_MOST_ONCE);
    if (schemaId != null && !schemaId.isBlank()) {
      builder.setSchemaId(schemaId);
    }
    return builder.build();
  }

  @Override
  public byte[] encode(Message message) throws IOException {
    byte[] opaque = message.getOpaqueData();
    byte[] cloudEventPayload = TakCloudEventPayloadExtractor.tryExtractPayload(opaque);
    if (cloudEventPayload != null) {
      opaque = cloudEventPayload;
    }
    if (opaque != null && opaque.length > 0) {
      String candidate = new String(opaque, StandardCharsets.UTF_8).trim();
      if (candidate.startsWith("<event")) {
        return opaque;
      }
      byte[] embeddedCot = tryExtractEmbeddedCotFromProtobuf(opaque);
      if (embeddedCot != null) {
        return embeddedCot;
      }
    }
    Map<String, String> meta = message.getMeta() != null ? message.getMeta() : new LinkedHashMap<>();
    String now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    String stale = Instant.now().plus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS).toString();

    String uid = getOrDefault(meta, "tak.uid", "maps-generated");
    String type = getOrDefault(meta, "tak.type", "a-f-G-U-C");
    String time = getOrDefault(meta, "tak.time", now);
    String start = getOrDefault(meta, "tak.start", now);
    String staleTime = getOrDefault(meta, "tak.stale", stale);
    String how = getOrDefault(meta, "tak.how", "m-g");
    String lat = getOrDefault(meta, "tak.lat", "0");
    String lon = getOrDefault(meta, "tak.lon", "0");
    String hae = getOrDefault(meta, "tak.hae", "0");
    String ce = getOrDefault(meta, "tak.ce", "9999999");
    String le = getOrDefault(meta, "tak.le", "9999999");

    String xml = "<event uid=\"" + esc(uid) + "\" type=\"" + esc(type) + "\" time=\"" + esc(time) + "\" start=\"" + esc(start)
        + "\" stale=\"" + esc(staleTime) + "\" how=\"" + esc(how) + "\">"
        + "<point lat=\"" + esc(lat) + "\" lon=\"" + esc(lon) + "\" hae=\"" + esc(hae) + "\" ce=\"" + esc(ce) + "\" le=\"" + esc(le) + "\"/>"
        + "</event>";
    return xml.getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] tryExtractEmbeddedCotFromProtobuf(byte[] payload) {
    try {
      CodedInputStream input = CodedInputStream.newInstance(payload);
      while (!input.isAtEnd()) {
        int tag = input.readTag();
        if (tag == 0) {
          break;
        }
        int wireType = WireFormat.getTagWireType(tag);
        if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
          byte[] candidate = input.readByteArray();
          if (candidate.length > 0) {
            String xmlCandidate = new String(candidate, StandardCharsets.UTF_8).trim();
            if (xmlCandidate.startsWith("<event")) {
              return candidate;
            }
          }
        } else if (!input.skipField(tag)) {
          break;
        }
      }
    } catch (Exception ignored) {
      // non-protobuf payload; fall through to metadata-based reconstruction
    }
    return null;
  }

  private static String getOrDefault(Map<String, String> meta, String key, String def) {
    String value = meta.get(key);
    return value == null || value.isBlank() ? def : value;
  }

  private static void putIfPresent(Map<String, String> meta, String key, String value) {
    if (value != null && !value.isBlank()) {
      meta.put(key, value);
    }
  }

  private static Element parseRootEvent(String xml) throws IOException {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
      dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      dbf.setExpandEntityReferences(false);
      dbf.setXIncludeAware(false);
      dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      DocumentBuilder builder = dbf.newDocumentBuilder();
      Document document = builder.parse(new InputSource(new StringReader(xml)));
      Element root = document.getDocumentElement();
      if (root == null || !"event".equalsIgnoreCase(root.getLocalName() == null ? root.getNodeName() : root.getLocalName())) {
        throw new IOException("Invalid CoT XML payload: root element must be event");
      }
      return root;
    } catch (ParserConfigurationException | SAXException ex) {
      throw new IOException("Invalid CoT XML payload", ex);
    }
  }

  private static Element extractPoint(Element root) throws IOException {
    NodeList byNs = root.getElementsByTagNameNS("*", "point");
    if (byNs.getLength() > 0 && byNs.item(0) instanceof Element element) {
      return element;
    }
    NodeList byName = root.getElementsByTagName("point");
    if (byName.getLength() > 0 && byName.item(0) instanceof Element element) {
      return element;
    }
    throw new IOException("Invalid CoT XML payload: missing point element");
  }

  private static String required(Element element, String name) throws IOException {
    String value = element.getAttribute(name);
    if (value == null || value.isBlank()) {
      throw new IOException("Invalid CoT XML payload: missing " + name);
    }
    return value;
  }

  private static String esc(String value) {
    return value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }

  private void mergeSchemaParsedMeta(Map<String, String> meta, byte[] payload) {
    if (xmlFormatter == null) {
      return;
    }
    try {
      IdentifierResolver parsed = xmlFormatter.parse(payload);
      if (parsed == null) {
        return;
      }
      promoteAny(parsed, meta, "tak.uid", "uid", "event.uid");
      promoteAny(parsed, meta, "tak.type", "type", "event.type");
      promoteAny(parsed, meta, "tak.time", "time", "event.time");
      promoteAny(parsed, meta, "tak.start", "start", "event.start");
      promoteAny(parsed, meta, "tak.stale", "stale", "event.stale");
      promoteAny(parsed, meta, "tak.how", "how", "event.how");
      promoteAny(parsed, meta, "tak.lat", "point.lat", "event.point.lat");
      promoteAny(parsed, meta, "tak.lon", "point.lon", "event.point.lon");
      promoteAny(parsed, meta, "tak.hae", "point.hae", "event.point.hae");
      promoteAny(parsed, meta, "tak.ce", "point.ce", "event.point.ce");
      promoteAny(parsed, meta, "tak.le", "point.le", "event.point.le");
      meta.put("tak.xml_schema_parsed", "true");
    } catch (Exception ignored) {
      // Preserve default CoT metadata extraction if formatter parsing fails.
    }
  }

  private static void promoteAny(IdentifierResolver parsed, Map<String, String> meta, String toKey, String... fromKeys) {
    for (String fromKey : fromKeys) {
      Object val = parsed.get(fromKey);
      if (val != null) {
        meta.put(toKey, val.toString());
        return;
      }
    }
  }

  private static void addSelectorAliases(Map<String, String> meta) {
    alias(meta, "tak.uid", "tak_uid");
    alias(meta, "tak.type", "tak_type");
    alias(meta, "tak.time", "tak_time");
    alias(meta, "tak.start", "tak_start");
    alias(meta, "tak.stale", "tak_stale");
    alias(meta, "tak.how", "tak_how");
    alias(meta, "tak.lat", "tak_lat");
    alias(meta, "tak.lon", "tak_lon");
    alias(meta, "tak.hae", "tak_hae");
    alias(meta, "tak.ce", "tak_ce");
    alias(meta, "tak.le", "tak_le");
    alias(meta, "tak.format", "tak_format");
    alias(meta, "tak.transport", "tak_transport");
  }

  private static void alias(Map<String, String> meta, String sourceKey, String aliasKey) {
    String value = meta.get(sourceKey);
    if (value != null && !value.isBlank()) {
      meta.put(aliasKey, value);
    }
  }

  private static MessageFormatter createFormatter(boolean namespaceAware, boolean coalescing, boolean validating, String rootEntry) {
    try {
      XmlSchemaConfig schemaConfig = new XmlSchemaConfig();
      XmlSchemaConfig.XmlConfig xmlConfig = new XmlSchemaConfig.XmlConfig();
      xmlConfig.setNamespaceAware(namespaceAware);
      xmlConfig.setCoalescing(coalescing);
      xmlConfig.setValidating(validating);
      xmlConfig.setRootEntry(rootEntry == null || rootEntry.isBlank() ? "event" : rootEntry);
      schemaConfig.setConfig(xmlConfig);
      return MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    } catch (Exception ignored) {
      return null;
    }
  }
}
