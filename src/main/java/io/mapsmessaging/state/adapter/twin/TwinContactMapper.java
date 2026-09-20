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

package io.mapsmessaging.state.adapter.twin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.model.DetectionEventType;
import io.mapsmessaging.state.drone.model.GeoPosition;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns a relayed twin document into a detection on the twin that saw it.
 *
 * <p>A node that owns a vehicle publishes what it detects to {@code /state/twins/<id>/contacts},
 * and those documents arrive verbatim at an aggregator. The aggregator is the node that holds the
 * TAK connection, so it -- not the owning node -- has to raise the detection for it to be drawn.
 *
 * <p>The detection is attached to the twin the aggregator ALREADY shows, found by the document's
 * asset uuid. Registering a twin of its own would put a second vehicle on the picture beside the
 * one built from the CATL, which is why an unknown asset is skipped rather than created.
 */
public class TwinContactMapper {

  /** The attribute the TAK mapper reads for a detection's playable feeds. */
  static final String VIDEO_URLS_ATTRIBUTE = "tak.videoUrls";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final TwinManager twinManager;

  public TwinContactMapper(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  /**
   * @return true when the document carried a contact that was raised as a detection
   */
  public boolean ingest(String document, TwinUpdateContext context) {
    if (document == null || document.isBlank()) {
      return false;
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(document);
    } catch (Exception malformed) {
      // a subscription must survive whatever arrives on it
      return false;
    }
    if (root == null || !root.isObject()) {
      return false;
    }
    JsonNode contact = root.get("contact");
    if (contact == null || !contact.isObject()) {
      return false;
    }
    Optional<DroneTwin> source = resolveSource(root);
    if (source.isEmpty()) {
      return false;
    }
    Optional<DetectionEvent> event = toDetection(contact, root);
    if (event.isEmpty()) {
      return false;
    }
    twinManager.notifyDetectionEvent(source.get(), event.get(), context);
    return true;
  }

  /** The twin this aggregator already holds for the asset, by uuid; by twin id as a fallback. */
  private Optional<DroneTwin> resolveSource(JsonNode root) {
    UUID uuid = readUuid(root.get("uuid"));
    if (uuid != null) {
      Optional<EntityTwin> byKey = twinManager.getTwin(uuid.toString());
      if (byKey.isPresent() && byKey.get() instanceof DroneTwin twin) {
        return Optional.of(twin);
      }
      for (EntityTwin twin : twinManager.listTwins()) {
        if (uuid.equals(twin.getUuid()) && twin instanceof DroneTwin drone) {
          return Optional.of(drone);
        }
      }
    }
    String twinId = text(root.get("twinId"));
    if (twinId != null) {
      Optional<EntityTwin> byId = twinManager.getTwin(twinId);
      if (byId.isPresent() && byId.get() instanceof DroneTwin twin) {
        return Optional.of(twin);
      }
    }
    return Optional.empty();
  }

  private Optional<DetectionEvent> toDetection(JsonNode contact, JsonNode root) {
    UUID contactId = readUuid(contact.get("id"));
    GeoPosition position = readPosition(contact.get("position"));
    Long ttlMillis = readLong(contact.get("ttlMillis"));
    if (contactId == null || position == null || ttlMillis == null || ttlMillis <= 0) {
      return Optional.empty();
    }

    Long created = readLong(contact.get("createdTimeMs"));
    Long updated = readLong(contact.get("updatedTimeMs"));
    DetectionEventType type =
        created != null && updated != null && updated > created
            ? DetectionEventType.UPDATED
            : DetectionEventType.DETECTED;

    DetectionEvent event = new DetectionEvent(contactId, text(contact.get("description")), type);
    event.setPosition(position);
    event.setTtlMillis(ttlMillis);
    if (updated != null) {
      event.setTimestamp(Instant.ofEpochMilli(updated));
    }
    List<String> videoUrls = readVideoUrls(root.get("dataProducts"));
    if (!videoUrls.isEmpty()) {
      event.addAttribute(VIDEO_URLS_ATTRIBUTE, videoUrls);
    }
    return Optional.of(event);
  }

  /**
   * The asset's streams, for the detection to carry. Only the schemes a client can play: a page it
   * cannot open would put a video icon on the marker that does nothing. The TAK mapper filters
   * again on render, so this is a narrowing, not the contract.
   */
  private List<String> readVideoUrls(JsonNode dataProducts) {
    List<String> urls = new ArrayList<>();
    if (dataProducts == null || !dataProducts.isArray()) {
      return urls;
    }
    for (JsonNode product : dataProducts) {
      String uri = text(product.get("uri"));
      if (uri == null) {
        continue;
      }
      String scheme = uri.toLowerCase(Locale.ROOT);
      boolean playable = scheme.startsWith("rtsp://") || scheme.startsWith("rtsps://")
          || scheme.startsWith("rtmp://") || scheme.startsWith("rtmps://")
          || ((scheme.startsWith("http://") || scheme.startsWith("https://"))
              && (scheme.endsWith(".m3u8") || scheme.endsWith(".ts")));
      if (playable) {
        urls.add(uri);
      }
    }
    return urls;
  }

  private static GeoPosition readPosition(JsonNode position) {
    if (position == null || !position.isObject()) {
      return null;
    }
    Double latitude = readDouble(position.get("latitude"));
    Double longitude = readDouble(position.get("longitude"));
    if (latitude == null || longitude == null) {
      return null;
    }
    return new GeoPosition(latitude, longitude, readDouble(position.get("altitudeMslMeters")),
        readDouble(position.get("altitudeAglMeters")), readDouble(position.get("altitudeRelativeMeters")));
  }

  private static UUID readUuid(JsonNode node) {
    String value = text(node);
    if (value == null) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException notAUuid) {
      return null;
    }
  }

  private static String text(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    String value = node.asText(null);
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static Long readLong(JsonNode node) {
    return node == null || !node.isNumber() ? null : node.asLong();
  }

  private static Double readDouble(JsonNode node) {
    return node == null || !node.isNumber() ? null : node.asDouble();
  }
}
