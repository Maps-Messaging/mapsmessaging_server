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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class CotEchoSuppressorTest {

  private static final String EVENT = "<event version=\"2.0\" uid=\"target\" type=\"a-f-G\" how=\"m-g\" time=\"2026-09-08T15:30:00Z\" start=\"2026-09-08T15:30:00Z\" stale=\"2026-09-08T15:32:00Z\"><point lat=\"1\" lon=\"2\" hae=\"3\" ce=\"4\" le=\"5\"/><detail><contact callsign=\"Target\"/></detail></event>";

  @Test
  void marks_event_and_identifies_only_its_own_echo() throws Exception {
    byte[] marked = CotEchoSuppressor.mark(EVENT.getBytes(StandardCharsets.UTF_8), "maps-gateway");
    Document document = parse(marked);
    Element marker = (Element) document
        .getElementsByTagNameNS(CotEchoSuppressor.MARKER_NAMESPACE, CotEchoSuppressor.MARKER_ELEMENT)
        .item(0);

    assertEquals("maps-gateway", marker.getAttribute(CotEchoSuppressor.MARKER_ATTRIBUTE));
    assertEquals("1", marker.getAttribute(CotEchoSuppressor.HOP_ATTRIBUTE));
    assertTrue(CotEchoSuppressor.isEcho(marked, "maps-gateway"));
    assertFalse(CotEchoSuppressor.isEcho(marked, "another-gateway"));
  }

  @Test
  void creates_detail_when_event_has_none() throws Exception {
    byte[] event = EVENT.replace("<detail><contact callsign=\"Target\"/></detail>", "")
        .getBytes(StandardCharsets.UTF_8);
    Document document = parse(CotEchoSuppressor.mark(event, "maps-gateway"));

    assertEquals(1, document.getElementsByTagName("detail").getLength());
    assertEquals(
        1,
        document.getElementsByTagNameNS(
            CotEchoSuppressor.MARKER_NAMESPACE,
            CotEchoSuppressor.MARKER_ELEMENT).getLength());
  }

  @Test
  void does_not_add_duplicate_origin_marker() throws Exception {
    byte[] marked = CotEchoSuppressor.mark(EVENT.getBytes(StandardCharsets.UTF_8), "maps-gateway");
    byte[] markedAgain = CotEchoSuppressor.mark(marked, "maps-gateway");
    Document document = parse(markedAgain);

    assertEquals(
        1,
        document.getElementsByTagNameNS(
            CotEchoSuppressor.MARKER_NAMESPACE,
            CotEchoSuppressor.MARKER_ELEMENT).getLength());
  }

  @Test
  void preserves_existing_namespaces_text_and_attributes() throws Exception {
    byte[] event = ("<event xmlns:x=\"urn:test\" version=\"2.0\" uid=\"target\" type=\"a-f-G\" how=\"m-g\" time=\"2026-09-08T15:30:00Z\" start=\"2026-09-08T15:30:00Z\" stale=\"2026-09-08T15:32:00Z\"><point lat=\"1\" lon=\"2\" hae=\"3\" ce=\"4\" le=\"5\"/><detail>before"
        + "<x:item x:value=\"a &amp; b\">payload</x:item>after</detail></event>")
        .getBytes(StandardCharsets.UTF_8);
    Document document = parse(CotEchoSuppressor.mark(event, "maps&amp;gateway"));
    Element item = (Element) document.getElementsByTagNameNS("urn:test", "item").item(0);

    assertEquals("a & b", item.getAttributeNS("urn:test", "value"));
    assertEquals("payload", item.getTextContent());
    assertTrue(document.getDocumentElement().getTextContent().contains("before"));
    assertTrue(CotEchoSuppressor.isEcho(
        CotEchoSuppressor.mark(event, "maps&gateway"),
        "maps&gateway"));
  }

  @Test
  void rejects_blank_origin_and_malformed_xml() {
    byte[] event = EVENT.getBytes(StandardCharsets.UTF_8);
    assertThrows(IllegalArgumentException.class, () -> CotEchoSuppressor.mark(event, " "));
    assertThrows(IOException.class, () -> CotEchoSuppressor.mark("<event>".getBytes(StandardCharsets.UTF_8), "maps"));
    assertFalse(CotEchoSuppressor.isEcho("<event>".getBytes(StandardCharsets.UTF_8), "maps"));
  }

  @Test
  void preserves_each_bridge_hop_and_excludes_markers_from_the_fingerprint() throws Exception {
    byte[] first = CotEchoSuppressor.mark(EVENT.getBytes(StandardCharsets.UTF_8), "maps-a");
    byte[] second = CotEchoSuppressor.mark(first, "maps-b");
    CotEchoSuppressor.CotEventInfo original =
        CotEchoSuppressor.inspect(EVENT.getBytes(StandardCharsets.UTF_8), 64);
    CotEchoSuppressor.CotEventInfo routed = CotEchoSuppressor.inspect(second, 64);

    assertEquals(List.of("maps-a", "maps-b"), routed.origins());
    assertEquals(2, routed.hopCount());
    assertEquals(original.fingerprint(), routed.fingerprint());
  }

  @Test
  void semantic_fingerprint_ignores_attribute_order_and_formatting_whitespace() throws Exception {
    byte[] reformatted = ("<event stale=\"2026-09-08T15:32:00Z\" start=\"2026-09-08T15:30:00Z\""
        + " time=\"2026-09-08T15:30:00Z\" how=\"m-g\" type=\"a-f-G\" uid=\"target\" version=\"2.0\">\n"
        + "  <point le=\"5\" ce=\"4\" hae=\"3\" lon=\"2\" lat=\"1\"/>\n"
        + "  <detail><contact callsign=\"Target\"/></detail>\n</event>")
        .getBytes(StandardCharsets.UTF_8);

    assertEquals(
        CotEchoSuppressor.inspect(EVENT.getBytes(StandardCharsets.UTF_8), 64).fingerprint(),
        CotEchoSuppressor.inspect(reformatted, 64).fingerprint());
  }

  @Test
  void preserves_unknown_details_and_classifies_unsupported_types() throws Exception {
    byte[] event = EVENT
        .replace("type=\"a-f-G\"", "type=\"z-private-extension\"")
        .replace("<contact callsign=\"Target\"/>", "<vendor:payload xmlns:vendor=\"urn:vendor\">data</vendor:payload>")
        .getBytes(StandardCharsets.UTF_8);

    byte[] marked = CotEchoSuppressor.mark(event, "maps-gateway");
    CotEchoSuppressor.CotEventInfo info = CotEchoSuppressor.inspect(marked, 64);
    Document document = parse(marked);

    assertEquals(CotEchoSuppressor.CotEventClass.UNSUPPORTED, info.eventClass());
    assertEquals("data", document.getElementsByTagNameNS("urn:vendor", "payload").item(0).getTextContent());
  }

  @Test
  void rejects_xxe_and_excessive_nesting() {
    byte[] xxe = ("<!DOCTYPE event [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
        + EVENT.replace("Target", "&xxe;"))
        .getBytes(StandardCharsets.UTF_8);
    byte[] deep = EVENT.replace(
        "<contact callsign=\"Target\"/>",
        "<a><b><c><d><e><f>value</f></e></d></c></b></a>")
        .getBytes(StandardCharsets.UTF_8);

    assertThrows(IOException.class, () -> CotEchoSuppressor.inspect(xxe, 64));
    assertThrows(IOException.class, () -> CotEchoSuppressor.inspect(deep, 6));
  }

  private Document parse(byte[] xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
  }
}
