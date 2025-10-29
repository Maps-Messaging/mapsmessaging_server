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

package io.mapsmessaging.utilities;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;

import java.util.Locale;

public class GeoHashUtils {

  private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
  private static final boolean[] VALID = new boolean[128];

  static {
    for (int i = 0; i < BASE32.length(); i++) {
      char c = BASE32.charAt(i);
      VALID[c] = true;
    }
  }

  public static String toGeoHash(double latitude, double longitude, int precision) {
    checkPrecision(precision);
    checkLatLon(latitude, longitude);
    return GeoHash.withCharacterPrecision(latitude, longitude, precision).toBase32();
  }

  public static String[] toGeoHashes(double latitude, double longitude, int precision) {
    String geoHash = toGeoHash(latitude, longitude, precision);
    String[] geoHashes = new String[geoHash.length()];
    for (int i = 0; i < geoHash.length(); i++) {
      geoHashes[i] = geoHash.substring(i, i + 1);
    }
    return geoHashes;
  }

  public static String toTopicNameGeoHash(double latitude, double longitude, int precision) {
    checkPrecision(precision);
    checkLatLon(latitude, longitude);

    String[] geoHashes = toGeoHashes(latitude, longitude, precision);
    StringBuilder topic = new StringBuilder();

    for (String geoHash : geoHashes) {
      topic.append('/').append(geoHash);
    }

    return topic.toString();
  }

  public static double[] fromGeoHash(String hash) {
    String normalized = normalizeAndValidateHash(hash);
    WGS84Point point = GeoHash.fromGeohashString(normalized).getOriginatingPoint();
    double latitude = point.getLatitude();
    double longitude = point.getLongitude();
    return new double[] { latitude, longitude };
  }

  public static double[] fromGeoHash(String[] hash) {
    validateParts(hash);
    StringBuilder builder = new StringBuilder(hash.length);
    for (String part : hash) {
      builder.append(part.toLowerCase(Locale.ROOT));
    }
    String normalized = normalizeAndValidateHash(builder.toString());
    WGS84Point point = GeoHash.fromGeohashString(normalized).getOriginatingPoint();
    double latitude = point.getLatitude();
    double longitude = point.getLongitude();
    return new double[] { latitude, longitude };
  }

  private static void checkPrecision(int precision) {
    if (precision < 1 || precision > 12) {
      throw new IllegalArgumentException("precision must be between 1 and 12");
    }
  }

  private static void checkLatLon(double latitude, double longitude) {
    if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
      throw new IllegalArgumentException("latitude/longitude must be finite");
    }
    if (latitude < -90.0 || latitude > 90.0) {
      throw new IllegalArgumentException("latitude out of range [-90, 90]");
    }
    if (longitude < -180.0 || longitude > 180.0) {
      throw new IllegalArgumentException("longitude out of range [-180, 180]");
    }
  }

  private static String normalizeAndValidateHash(String hash) {
    if (hash == null) {
      throw new IllegalArgumentException("geohash must not be null");
    }
    String trimmed = hash.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("geohash must not be empty");
    }
    String lower = trimmed.toLowerCase(Locale.ROOT);
    for (int i = 0; i < lower.length(); i++) {
      char c = lower.charAt(i);
      if (c >= VALID.length || !VALID[c]) {
        throw new IllegalArgumentException("invalid geohash character: '" + c + '\'');
      }
    }
    return lower;
  }

  private static void validateParts(String[] parts) {
    if (parts == null) {
      throw new IllegalArgumentException("geohash parts must not be null");
    }
    if (parts.length == 0) {
      throw new IllegalArgumentException("geohash parts must not be empty");
    }
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      if (part == null) {
        throw new IllegalArgumentException("geohash part at index " + i + " is null");
      }
      if (part.length() != 1) {
        throw new IllegalArgumentException("geohash part at index " + i + " must be a single character");
      }
      char c = Character.toLowerCase(part.charAt(0));
      if (c >= VALID.length || !VALID[c]) {
        throw new IllegalArgumentException("invalid geohash character at index " + i + ": '" + c + '\'');
      }
    }
  }
}
