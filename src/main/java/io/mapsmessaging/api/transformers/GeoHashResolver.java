/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.api.transformers;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.utilities.GeoHashUtils;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.api.message.Filter.getTopicResolver;

public class GeoHashResolver implements InterServerTransformation {

  private String prefix;
  private String latKey;
  private String lonKey;
  private int precision;
  private boolean splitHash;

  // New, extensible configuration
  private List<String> latKeys;
  private List<String> lonKeys;
  private String units;            // "deg" | "rad" | "e7" | "micros"
  private String layout;           // "chars-per-segment" | "two-per-segment" | "raw"
  private String onMissing;        // "drop" | "skip" | "defaultTo"
  private Double defaultLatitude;  // used only when onMissing == "defaultTo"
  private Double defaultLongitude; // used only when onMissing == "defaultTo"

  public GeoHashResolver() {
  }

  public GeoHashResolver(String prefix, String latKey, String lonKey, int precision, boolean splitHash) {
    this.prefix = prefix != null ? prefix : "";
    this.latKey = latKey != null ? latKey : "latitude";
    this.lonKey = lonKey != null ? lonKey : "longitude";
    this.precision = precision;
    this.splitHash = splitHash;

    this.latKeys = new ArrayList<>();
    this.lonKeys = new ArrayList<>();
    this.units = "deg";
    this.layout = "chars-per-segment";
    this.onMissing = "skip";
    this.defaultLatitude = null;
    this.defaultLongitude = null;
  }

  public Protocol.ParsedMessage transform(String source, Protocol.ParsedMessage message) {
    IdentifierResolver identifierResolver = getTopicResolver(source, message.getMessage());
    if (identifierResolver == null) {
      return handleMissing(message);
    }

    Object latitudeObject = identifierResolver.get(latKey);
    Object longitudeObject = identifierResolver.get(lonKey);

    if (latitudeObject == null || longitudeObject == null) {
      latitudeObject = findFirst(identifierResolver, latKeys);
      longitudeObject = findFirst(identifierResolver, lonKeys);
    }

    if (latitudeObject == null || longitudeObject == null) {
      return handleMissing(message);
    }

    Double latitude = safeToDouble(latitudeObject);
    Double longitude = safeToDouble(longitudeObject);

    if (latitude == null || longitude == null) {
      return handleMissing(message);
    }

    double[] normalized = normalizeUnits(latitude, longitude, units);
    double normalizedLatitude = normalized[0];
    double normalizedLongitude = normalized[1];

    String geohash = GeoHashUtils.toGeoHash(normalizedLatitude, normalizedLongitude, precision);
    String topicSuffix;

    String effectiveLayout = resolveLayoutPreference();

    if ("raw".equalsIgnoreCase(effectiveLayout)) {
      topicSuffix = "/" + geohash;
    } else if ("two-per-segment".equalsIgnoreCase(effectiveLayout)) {
      topicSuffix = toTwoPerSegmentPath(geohash);
    } else {
      topicSuffix = GeoHashUtils.toTopicNameGeoHash(normalizedLatitude, normalizedLongitude, precision);
    }

    String effectivePrefix = prefix != null ? prefix : "";
    message.setDestinationName(effectivePrefix + topicSuffix);
    return message;
  }

  @SuppressWarnings("unchecked")
  public InterServerTransformation build(ConfigurationProperties properties){
    ConfigurationProperties map = ((ConfigurationProperties) properties.get("parameters"));
    String localPrefix = map.getProperty("prefix", "");
    String localLatKey = map.getProperty( "latKey", "latitude");
    String localLonKey =  map.getProperty("lonKey", "longitude");
    int localPrecision = map.getIntProperty( "precision", 5);
    boolean localSplitHash = map.getBooleanProperty("splitHash", true);

    GeoHashResolver resolver = new GeoHashResolver(localPrefix, localLatKey, localLonKey, localPrecision, localSplitHash);

    resolver.latKeys = safeStringList(map.getProperty("latKeys", ""));
    resolver.lonKeys = safeStringList(map.getProperty("lonKeys"));
    resolver.units = map.getProperty("units", "deg");
    resolver.layout = map.getProperty( "layout", "chars-per-segment");
    resolver.onMissing = map.getProperty( "onMissing", "skip");

    // defaultTo expects "lat,lon" (e.g., "0,0")
    if ("defaultTo".equalsIgnoreCase(resolver.onMissing)) {
      String defaultTo = map.getProperty( "defaultTo", null);
      if (defaultTo != null) {
        double[] pair = parseLatLonPair(defaultTo);
        resolver.defaultLatitude = pair != null ? pair[0] : null;
        resolver.defaultLongitude = pair != null ? pair[1] : null;
      }
    }
    return resolver;
  }

  private Protocol.ParsedMessage handleMissing(Protocol.ParsedMessage message) {
    if ("drop".equalsIgnoreCase(onMissing)) {
      return null; // convention: null indicates drop
    }
    if ("defaultTo".equalsIgnoreCase(onMissing) && defaultLatitude != null && defaultLongitude != null) {
      String geohash = GeoHashUtils.toGeoHash(defaultLatitude, defaultLongitude, precision);
      String suffix;
      String effectiveLayout = resolveLayoutPreference();
      if ("raw".equalsIgnoreCase(effectiveLayout)) {
        suffix = "/" + geohash;
      } else if ("two-per-segment".equalsIgnoreCase(effectiveLayout)) {
        suffix = toTwoPerSegmentPath(geohash);
      } else {
        suffix = GeoHashUtils.toTopicNameGeoHash(defaultLatitude, defaultLongitude, precision);
      }
      String effectivePrefix = prefix != null ? prefix : "";
      message.setDestinationName(effectivePrefix + suffix);
      return message;
    }
    return message;
  }

  private static Object findFirst(IdentifierResolver identifierResolver, List<String> candidateKeys) {
    if (candidateKeys == null || candidateKeys.isEmpty()) {
      return null;
    }
    for (String key : candidateKeys) {
      Object value = identifierResolver.get(key);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private static Double safeToDouble(Object value) {
    if (value instanceof Number num) {
      return num.doubleValue();
    }
    if (value instanceof String str) {
      try {
        return Double.parseDouble(str);
      } catch (NumberFormatException ignore) {
        return null;
      }
    }
    return null;
  }

  private static double[] normalizeUnits(double latitude, double longitude, String units) {
    if (units == null || "deg".equalsIgnoreCase(units)) {
      return new double[]{latitude, longitude};
    }
    if ("rad".equalsIgnoreCase(units)) {
      return new double[]{Math.toDegrees(latitude), Math.toDegrees(longitude)};
    }
    if ("e7".equalsIgnoreCase(units)) {
      return new double[]{latitude / 1e7d, longitude / 1e7d};
    }
    if ("micros".equalsIgnoreCase(units)) {
      return new double[]{latitude / 1e6d, longitude / 1e6d};
    }
    return new double[]{latitude, longitude};
  }

  private String resolveLayoutPreference() {
    if (layout != null && !layout.isEmpty()) {
      return layout;
    }
    return splitHash ? "chars-per-segment" : "raw";
  }

  private static String toTwoPerSegmentPath(String geohash) {
    StringBuilder builder = new StringBuilder();
    builder.append('/');
    for (int i = 0; i < geohash.length(); i += 2) {
      int end = Math.min(i + 2, geohash.length());
      builder.append(geohash, i, end);
      if (end < geohash.length()) {
        builder.append('/');
      }
    }
    return builder.toString();
  }

  @SuppressWarnings("unchecked")
  private static List<String> safeStringList( String value) {
    List<String> result = new ArrayList<>();
    String[] parts = value.split(",");
    for (String part : parts) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        result.add(trimmed);
      }
    }
    return result;
  }

  private static double[] parseLatLonPair(String text) {
    if (text == null) {
      return null;
    }
    String[] parts = text.split(",");
    if (parts.length != 2) {
      return null;
    }
    try {
      double lat = Double.parseDouble(parts[0].trim());
      double lon = Double.parseDouble(parts[1].trim());
      return new double[]{lat, lon};
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  public String getName() {
    return "GeoHash";
  }

  @Override
  public String getDescription() {
    return "Looks up data in the message and resolves topic name to the geohash of the source";
  }
}
