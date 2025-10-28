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

public class GeoHashUtils {

  public static String toGeoHash(double latitude, double longitude, int precision) {
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
    String[] geoHashes = toGeoHashes(latitude, longitude, precision);
    StringBuilder topic = new StringBuilder();

    for (String geoHash : geoHashes) {
      topic.append('/').append(geoHash);
    }

    return topic.toString();
  }

  public static double[] fromGeoHash(String hash) {
    WGS84Point point = GeoHash.fromGeohashString(hash).getOriginatingPoint();
    return new double[] { point.getLatitude(), point.getLongitude() };
  }

  public static double[] fromGeoHash(String[] hash) {
    StringBuilder sb = new StringBuilder();
    for (String s : hash) {
      sb.append(s);
    }
    WGS84Point point = GeoHash.fromGeohashString(sb.toString()).getOriginatingPoint();
    return new double[] { point.getLatitude(), point.getLongitude() };
  }

  public static void main(String[] args) {
    double lat = -33.8688;   // Sydney
    double lon = 151.2093;
    int precision = 7;

    String hash = GeoHashUtils.toGeoHash(lat, lon, precision);
    System.out.println("GeoHash: " + hash);

    double[] decoded = GeoHashUtils.fromGeoHash(hash);
    System.out.printf("Decoded: lat=%.6f, lon=%.6f%n", decoded[0], decoded[1]);

    String[] hashParts = GeoHashUtils.toGeoHashes(lat, lon, precision);
    System.out.print("Split: ");
    for (String s : hashParts) System.out.print(s + "/");
    System.out.println();

    double[] decodedFromParts = GeoHashUtils.fromGeoHash(hashParts);
    System.out.printf("Decoded from parts: lat=%.6f, lon=%.6f%n", decodedFromParts[0], decodedFromParts[1]);

    System.out.println("Topic name geohash: "+toTopicNameGeoHash(lat, lon, precision));
  }
}
