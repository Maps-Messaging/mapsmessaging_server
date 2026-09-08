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
package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.cot.CotEvent;
import io.mapsmessaging.cot.CotParser;
import io.mapsmessaging.state.drone.tak.model.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Maps a schema-validated CoT event into the TAK model used by the state layer. */
public class CotXmlParser {

  private final CotParser cotParser = new CotParser();

  public TakEvent parse(String xml) throws IOException {
    if (xml == null) {
      throw new IOException("CoT XML cannot be null");
    }
    return parse(xml.getBytes(StandardCharsets.UTF_8));
  }

  public TakEvent parse(byte[] xml) throws IOException {
    return parseEvent(cotParser.parse(xml));
  }

  private TakEvent parseEvent(CotEvent source) throws IOException {
    TakEvent event = new TakEvent();
    event.setVersion(source.version());
    event.setUid(source.uid());
    event.setType(source.type());
    event.setHow(source.how());
    event.setTime(source.time().toString());
    event.setStart(source.start().toString());
    event.setStale(source.stale().toString());
    if (source.point() != null) {
      event.setPoint(parsePoint(source));
    }
    if (source.detail() != null) {
      event.setDetail(parseDetail(source.detail()));
    }
    return event;
  }

  private TakPoint parsePoint(CotEvent source) {
    TakPoint point = new TakPoint();
    point.setLat(source.point().latitude().doubleValue());
    point.setLon(source.point().longitude().doubleValue());
    point.setHae(source.point().heightAboveEllipsoid().doubleValue());
    point.setCe(source.point().circularError().doubleValue());
    point.setLe(source.point().linearError().doubleValue());
    return point;
  }

  private TakDetail parseDetail(Element element) throws IOException {
    TakDetail detail = new TakDetail();
    Element contact = child(element, "contact");
    if (contact != null) {
      TakContact value = new TakContact();
      value.setCallsign(attribute(contact, "callsign"));
      detail.setContact(value);
    }
    Element track = child(element, "track");
    if (track != null) {
      TakTrack value = new TakTrack();
      value.setSpeed(number(track, "speed"));
      value.setCourse(number(track, "course"));
      detail.setTrack(value);
    }
    Element status = child(element, "status");
    if (status != null) {
      TakStatus value = new TakStatus();
      value.setLifecycle(attribute(status, "lifecycle"));
      value.setReason(attribute(status, "reason"));
      detail.setStatus(value);
    }
    Element remarks = child(element, "remarks");
    if (remarks != null) {
      detail.setRemarks(remarks.getTextContent());
    }
    Element precision = child(element, "precisionlocation");
    if (precision != null) {
      TakPrecisionLocation value = new TakPrecisionLocation();
      value.setAltsrc(attribute(precision, "altsrc"));
      detail.setPrecisionLocation(value);
    }
    Element takv = child(element, "takv");
    if (takv != null) {
      TakPlatform value = new TakPlatform();
      value.setDevice(attribute(takv, "device"));
      value.setPlatform(attribute(takv, "platform"));
      value.setOs(attribute(takv, "os"));
      value.setVersion(attribute(takv, "version"));
      detail.setTakv(value);
    }
    Element mapsLink = child(element, "maps-link");
    if (mapsLink != null) {
      detail.setMapsLink(parseLinkState(mapsLink));
    }
    List<TakLink> links = new ArrayList<>();
    for (Element link : children(element, "link")) {
      TakLink value = new TakLink();
      value.setUid(attribute(link, "uid"));
      value.setRelation(attribute(link, "relation"));
      links.add(value);
    }
    detail.setLinks(links);
    return detail;
  }

  private TakLinkState parseLinkState(Element element) throws IOException {
    TakLinkState value = new TakLinkState();
    value.setState(attribute(element, "state"));
    String connected = attribute(element, "connected");
    value.setConnected(connected == null ? null : Boolean.valueOf(connected));
    value.setRssiDbm(integer(element, "rssiDbm"));
    value.setSnrDb(number(element, "snrDb"));
    value.setLatencyMs(number(element, "latencyMs"));
    value.setRxErrorRate(number(element, "rxErrorRate"));
    value.setTxErrorRate(number(element, "txErrorRate"));
    return value;
  }

  private Double number(Element element, String name) throws IOException {
    String raw = attribute(element, name);
    if (raw == null) {
      return null;
    }
    try {
      double value = Double.parseDouble(raw);
      if (!Double.isFinite(value)) {
        throw new NumberFormatException("non-finite");
      }
      return value;
    } catch (NumberFormatException e) {
      throw new IOException("Invalid numeric CoT attribute " + name, e);
    }
  }

  private Integer integer(Element element, String name) throws IOException {
    String raw = attribute(element, name);
    if (raw == null) {
      return null;
    }
    try {
      return Integer.valueOf(raw);
    } catch (NumberFormatException e) {
      throw new IOException("Invalid integer CoT attribute " + name, e);
    }
  }

  private static String attribute(Element element, String name) {
    String value = element.getAttribute(name);
    return value.isBlank() ? null : value;
  }

  private static Element child(Element parent, String name) {
    for (Element element : children(parent, name)) {
      return element;
    }
    return null;
  }

  private static List<Element> children(Element parent, String name) {
    List<Element> matches = new ArrayList<>();
    NodeList nodes = parent.getChildNodes();
    for (int index = 0; index < nodes.getLength(); index++) {
      Node node = nodes.item(index);
      if (node instanceof Element element && name.equals(element.getTagName())) {
        matches.add(element);
      }
    }
    return matches;
  }
}
