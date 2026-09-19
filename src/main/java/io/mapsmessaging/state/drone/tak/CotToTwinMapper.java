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
package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.Instant;

/**
 * Inverse of {@code TakEventMapper}: parses one inbound CoT {@code <event>} document into an
 * {@link DroneTwin} so it can go through {@code TwinManager} like any mavlink/N2K-sourced twin.
 * Shared by every CoT ingest path that wants a twin onto the shared MTI-aware pipeline instead of
 * forwarding raw CoT bytes unmodified - today that's {@code CotProtocol} (raw TLS stream ingest)
 * and {@code CotIngestAdapter} (MAPS-to-MAPS topic bridge ingest, e.g. an edge node's own
 * composed CoT replicated in via NetworkConnectionManager).
 *
 * <p>Deliberately minimal - only the fields a twin actually needs to render back out through
 * {@code TakEventMapper}/{@code CotEventPolicy}: position, a display name, {@code <track>}
 * speed/course (without this a re-derived twin's heading indicator silently drops out - see
 * {@link #applyTrack}), and the original type for the no-MTI-match fallback. Anything else in
 * {@code <detail>} is not parsed; this is about getting the twin onto the shared MTI-aware
 * pipeline, not round-tripping every CoT extension losslessly.
 */
public final class CotToTwinMapper {

  /** The twin attribute {@code CotEventPolicy}/{@code TakEventMapper} fall back to when no MTI
   *  status applies - the type this event arrived with, so a CoT-ingested twin with no MTI match
   *  renders with the type it was already given, not MAPS' own vehicle-class guess. */
  public static final String ORIGINAL_COT_TYPE_ATTRIBUTE = "originalCotType";

  private final DocumentBuilderFactory documentBuilderFactory;

  public CotToTwinMapper() {
    documentBuilderFactory = DocumentBuilderFactory.newInstance();
    // Inbound CoT is untrusted network input - refuse external entities/DTDs rather than resolve
    // them (XXE hardening), not just malformed-XML tolerance.
    documentBuilderFactory.setExpandEntityReferences(false);
    trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    trySetFeature("http://xml.org/sax/features/external-general-entities", false);
    trySetFeature("http://xml.org/sax/features/external-parameter-entities", false);
  }

  private void trySetFeature(String feature, boolean value) {
    try {
      documentBuilderFactory.setFeature(feature, value);
    } catch (Exception ignored) {
      // Feature not supported by this JAXP implementation - the setExpandEntityReferences(false)
      // above already covers the common case.
    }
  }

  /**
   * Parses {@code xml} and, if it carries a usable {@code uid}, registers or updates the
   * corresponding twin in {@code twinManager}.
   *
   * @return {@code true} if the document was mapped and routed, {@code false} if it had no usable
   *     {@code uid} (nothing was routed).
   */
  public boolean routeToTwinManager(TwinManager twinManager, byte[] xml, String updateSource) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setUpdateSource(updateSource);
    context.setReceivedTime(Instant.now());
    return routeToTwinManager(twinManager, xml, context);
  }

  public boolean routeToTwinManager(
      TwinManager twinManager,
      byte[] xml,
      TwinUpdateContext context) {
    DroneTwin parsed = map(xml);
    if (parsed == null) {
      return false;
    }

    if (context.getReceivedTime() == null) {
      context.setReceivedTime(Instant.now());
    }

    if (twinManager.getTwin(parsed.getTwinId()).isPresent()) {
      twinManager.updateTwin(parsed.getTwinId(), existing -> copyOnto(existing, parsed), context);
    } else {
      twinManager.registerTwin(parsed, context);
    }
    return true;
  }

  private void copyOnto(EntityTwin existing, DroneTwin fresh) {
    if (fresh.getGeoPosition() != null) {
      existing.setGeoPosition(fresh.getGeoPosition());
    }
    if (fresh.getDisplayName() != null) {
      existing.setDisplayName(fresh.getDisplayName());
    }
    if (existing instanceof DroneTwin existingDrone) {
      if (fresh.getCallSign() != null) {
        existingDrone.setCallSign(fresh.getCallSign());
      }
      if (fresh.getGroundSpeedMetersPerSecond() != null) {
        existingDrone.setGroundSpeedMetersPerSecond(fresh.getGroundSpeedMetersPerSecond());
      }
      if (fresh.getCourseOverGroundDegrees() != null) {
        existingDrone.setCourseOverGroundDegrees(fresh.getCourseOverGroundDegrees());
      }
    }
    String originalType = fresh.getAttributes().get(ORIGINAL_COT_TYPE_ATTRIBUTE);
    if (originalType != null) {
      existing.getAttributes().put(ORIGINAL_COT_TYPE_ATTRIBUTE, originalType);
    }
  }

  /** @return the mapped twin, or {@code null} if the document has no usable {@code uid}. */
  public DroneTwin map(byte[] xml) {
    Element event = parseEventElement(xml);
    if (event == null) {
      return null;
    }

    String uid = event.getAttribute("uid");
    if (uid == null || uid.isBlank()) {
      return null;
    }

    DroneTwin twin = new DroneTwin(uid);
    twin.setTwinType(TwinType.DRONE);

    String type = event.getAttribute("type");
    if (type != null && !type.isBlank()) {
      twin.getAttributes().put(ORIGINAL_COT_TYPE_ATTRIBUTE, type);
    }

    applyPoint(twin, event);
    applyContact(twin, event);
    applyTrack(twin, event);
    return twin;
  }

  private Element parseEventElement(byte[] xml) {
    try {
      DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
      Document document = builder.parse(new ByteArrayInputStream(xml));
      Element root = document.getDocumentElement();
      return "event".equals(root.getTagName()) ? root : null;
    } catch (Exception e) {
      return null;
    }
  }

  private void applyPoint(DroneTwin twin, Element event) {
    Element point = firstChild(event, "point");
    if (point == null) {
      return;
    }
    Double lat = parseDoubleOrNull(point.getAttribute("lat"));
    Double lon = parseDoubleOrNull(point.getAttribute("lon"));
    if (lat == null || lon == null) {
      return;
    }
    GeoPosition geoPosition = new GeoPosition();
    geoPosition.setLatitude(lat);
    geoPosition.setLongitude(lon);
    geoPosition.setAltitudeMslMeters(parseDoubleOrNull(point.getAttribute("hae")));
    twin.setGeoPosition(geoPosition);
  }

  private void applyContact(DroneTwin twin, Element event) {
    Element detail = firstChild(event, "detail");
    if (detail == null) {
      return;
    }
    Element contact = firstChild(detail, "contact");
    if (contact == null) {
      return;
    }
    String callsign = contact.getAttribute("callsign");
    if (callsign != null && !callsign.isBlank()) {
      twin.setCallSign(callsign);
      twin.setDisplayName(callsign);
    }
  }

  /**
   * Carries the source twin's speed/course through the round-trip - without this, a twin
   * re-derived purely from CoT (no direct mavlink/n2k feed of its own, e.g. on a MAPS-to-MAPS
   * ingest node) has no {@code TakEventMapper.resolveSpeedMetersPerSecond}/
   * {@code resolveCourseDegrees} source, so the recomposed CoT's {@code <track>} silently
   * defaults to 0/0 - TAK clients then drop the heading/predicted-track indicator entirely,
   * even though the position keeps updating correctly.
   */
  private void applyTrack(DroneTwin twin, Element event) {
    Element detail = firstChild(event, "detail");
    if (detail == null) {
      return;
    }
    Element track = firstChild(detail, "track");
    if (track == null) {
      return;
    }
    Double speed = parseDoubleOrNull(track.getAttribute("speed"));
    if (speed != null) {
      twin.setGroundSpeedMetersPerSecond(speed);
    }
    Double course = parseDoubleOrNull(track.getAttribute("course"));
    if (course != null) {
      twin.setCourseOverGroundDegrees(course);
    }
  }

  private Element firstChild(Element parent, String tagName) {
    NodeList children = parent.getElementsByTagName(tagName);
    return children.getLength() > 0 ? (Element) children.item(0) : null;
  }

  private Double parseDoubleOrNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
