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

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
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

    return new MessageBuilder()
        .setOpaqueData(payload)
        .setMeta(meta)
        .setContentType("application/cot+xml")
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .build();
  }

  @Override
  public byte[] encode(Message message) throws IOException {
    byte[] opaque = message.getOpaqueData();
    if (opaque != null && opaque.length > 0) {
      String candidate = new String(opaque, StandardCharsets.UTF_8).trim();
      if (candidate.startsWith("<event")) {
        return opaque;
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
}
