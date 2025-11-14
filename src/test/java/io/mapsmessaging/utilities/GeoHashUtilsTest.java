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
 *
 */

package io.mapsmessaging.utilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class GeoHashUtilsTest {

  private static final double TEST_LATITUDE = -33.8568;   // Sydney Opera House
  private static final double TEST_LONGITUDE = 151.2153;


  @Test
  @DisplayName("toGeoHash produces expected base32 at precision 6")
  void toGeoHash_knownPoint_precision6() {
    String geohash = GeoHashUtils.toGeoHash(TEST_LATITUDE, TEST_LONGITUDE, 6);
    assertEquals("r3gx2u", geohash);
  }

  @Test
  @DisplayName("toGeoHashes splits into single-character array")
  void toGeoHashes_splitsCorrectly() {
    String[] parts = GeoHashUtils.toGeoHashes(TEST_LATITUDE, TEST_LONGITUDE, 6);
    assertArrayEquals(new String[] {"r","3","g","x","2","u"}, parts);
  }

  @Test
  @DisplayName("toTopicNameGeoHash builds '/r/3/g/x/2/u' style path")
  void toTopicNameGeoHash_buildsPath() {
    String topicPath = GeoHashUtils.toTopicNameGeoHash(TEST_LATITUDE, TEST_LONGITUDE, 6);
    assertEquals("/r/3/g/x/2/u", topicPath);
  }

  @Test
  @DisplayName("fromGeoHash(String) returns lat/long near cell center")
  void fromGeoHash_string_roundTripWithinTolerance() {
    String geohash = GeoHashUtils.toGeoHash(TEST_LATITUDE, TEST_LONGITUDE, 6);
    double[] center = GeoHashUtils.fromGeoHash(geohash);

    // Precision 6 cell size is about 1.2 km; 0.01° is ~1.1 km in latitude.
    double toleranceDegrees = 0.015; // be generous to avoid false negatives
    assertEquals(TEST_LATITUDE, center[0], toleranceDegrees);
    assertEquals(TEST_LONGITUDE, center[1], toleranceDegrees);
  }

  @Test
  @DisplayName("fromGeoHash(String[]) reconstructs same as String")
  void fromGeoHash_array_matchesStringVersion() {
    String geohash = GeoHashUtils.toGeoHash(TEST_LATITUDE, TEST_LONGITUDE, 6);
    String[] parts = geohash.split("");
    double[] fromString = GeoHashUtils.fromGeoHash(geohash);
    double[] fromArray = GeoHashUtils.fromGeoHash(parts);

    assertEquals(fromString[0], fromArray[0], 1e-9);
    assertEquals(fromString[1], fromArray[1], 1e-9);
  }

  @ParameterizedTest(name = "round-trip geohash preserves hash at precision {0}")
  @ValueSource(ints = {1, 5, 6, 8, 12})
  void roundTrip_preservesHashAtPrecisions(int precision) {
    String geohash = GeoHashUtils.toGeoHash(TEST_LATITUDE, TEST_LONGITUDE, precision);
    double[] center = GeoHashUtils.fromGeoHash(geohash);
    String rehashed = GeoHashUtils.toGeoHash(center[0], center[1], precision);
    assertEquals(geohash, rehashed);
  }

  @Test
  @DisplayName("topic path length equals precision and starts with slash")
  void topicPath_shape() {
    int precision = 7;
    String topicPath = GeoHashUtils.toTopicNameGeoHash(TEST_LATITUDE, TEST_LONGITUDE, precision);
    assertTrue(topicPath.startsWith("/"));
    assertEquals(precision, topicPath.chars().filter(c -> c == '/').count()); // one slash per char
    assertEquals(precision * 2, topicPath.length()); // '/' + char repeated
  }

  @Nested
  class HappyPath {

    @ParameterizedTest(name = "precision {0}: hash length matches")
    @ValueSource(ints = {1, 2, 3, 5, 6, 8, 12})
    void toGeoHash_length_matches_precision(int precision) {
      String geohash = GeoHashUtils.toGeoHash(TEST_LATITUDE, TEST_LONGITUDE, precision);
      assertEquals(precision, geohash.length());
    }

    @Test
    @DisplayName("split to single-char parts and rebuild topic path")
    void toGeoHashes_and_topic_path_shape() {
      int precision = 6;

      String[] parts = GeoHashUtils.toGeoHashes(TEST_LATITUDE, TEST_LONGITUDE, precision);
      assertEquals(precision, parts.length);
      for (String part : parts) {
        assertNotNull(part);
        assertEquals(1, part.length());
      }

      String topicPath = GeoHashUtils.toTopicNameGeoHash(TEST_LATITUDE, TEST_LONGITUDE, precision);
      assertTrue(topicPath.startsWith("/"));
      long slashCount = topicPath.chars().filter(c -> c == '/').count();
      assertEquals(precision, slashCount);
      assertEquals(precision * 2, topicPath.length());
    }

    @Test
    @DisplayName("round-trip: center re-hashes to identical geohash at same precision")
    void roundTrip_center_rehash_equal() {
      int precision = 6;
      String geohash = GeoHashUtils.toGeoHash(TEST_LATITUDE, TEST_LONGITUDE, precision);
      double[] center = GeoHashUtils.fromGeoHash(geohash);
      String rehashed = GeoHashUtils.toGeoHash(center[0], center[1], precision);
      assertEquals(geohash, rehashed);
    }

    @Test
    @DisplayName("fromGeoHash(String[]) equals fromGeoHash(String)")
    void array_and_string_decoding_match() {
      int precision = 7;
      String geohash = GeoHashUtils.toGeoHash(TEST_LATITUDE, TEST_LONGITUDE, precision);
      String[] parts = geohash.split("");

      double[] fromString = GeoHashUtils.fromGeoHash(geohash);
      double[] fromArray = GeoHashUtils.fromGeoHash(parts);

      assertEquals(fromString[0], fromArray[0], 1e-12);
      assertEquals(fromString[1], fromArray[1], 1e-12);
    }

    @Test
    @DisplayName("boundary tolerance: allow adjacent cell for known boundary point")
    void boundary_tolerance_membership() {
      int precision = 6;
      String geohash = GeoHashUtils.toGeoHash(TEST_LATITUDE, TEST_LONGITUDE, precision);
      assertTrue(geohash.equals("r3gx2u") || geohash.equals("r3gx2v"));
    }
  }

  @Nested
  class ValidationFailures {

    @ParameterizedTest(name = "precision {0} should be rejected")
    @ValueSource(ints = {0, -1, 13, 100})
    void invalid_precision(int precision) {
      assertThrows(IllegalArgumentException.class,
          () -> GeoHashUtils.toGeoHash(TEST_LATITUDE, TEST_LONGITUDE, precision));
      assertThrows(IllegalArgumentException.class,
          () -> GeoHashUtils.toTopicNameGeoHash(TEST_LATITUDE, TEST_LONGITUDE, precision));
      assertThrows(IllegalArgumentException.class,
          () -> GeoHashUtils.toGeoHashes(TEST_LATITUDE, TEST_LONGITUDE, precision));
    }

    @ParameterizedTest(name = "lat={0}, lon={1} should be rejected")
    @CsvSource({
        "91,0",
        "-91,0",
        "0,181",
        "0,-181"
    })
    void out_of_range_lat_lon(double latitude, double longitude) {
      assertThrows(IllegalArgumentException.class,
          () -> GeoHashUtils.toGeoHash(latitude, longitude, 5));
      assertThrows(IllegalArgumentException.class,
          () -> GeoHashUtils.toTopicNameGeoHash(latitude, longitude, 5));
      assertThrows(IllegalArgumentException.class,
          () -> GeoHashUtils.toGeoHashes(latitude, longitude, 5));
    }

    @Test
    @DisplayName("non-finite lat/lon are rejected")
    void non_finite_lat_lon() {
      assertThrows(IllegalArgumentException.class,
          () -> GeoHashUtils.toGeoHash(Double.NaN, TEST_LONGITUDE, 5));
      assertThrows(IllegalArgumentException.class,
          () -> GeoHashUtils.toGeoHash(TEST_LATITUDE, Double.POSITIVE_INFINITY, 5));
      assertThrows(IllegalArgumentException.class,
          () -> GeoHashUtils.toGeoHash(Double.NEGATIVE_INFINITY, Double.NaN, 5));
    }

    @Test
    @DisplayName("null/empty/whitespace geohash string rejected")
    void invalid_geohash_string_null_empty() {
      assertThrows(IllegalArgumentException.class, () -> GeoHashUtils.fromGeoHash((String) null));
      assertThrows(IllegalArgumentException.class, () -> GeoHashUtils.fromGeoHash(""));
      assertThrows(IllegalArgumentException.class, () -> GeoHashUtils.fromGeoHash("   "));
    }

    @ParameterizedTest(name = "invalid char in hash \"{0}\" rejected")
    @ValueSource(strings = {"r3gx2I", "R3GX2V!", "zzzzzz~", "abcdeo"})
    void invalid_geohash_string_characters(String input) {
      assertThrows(IllegalArgumentException.class, () -> GeoHashUtils.fromGeoHash(input));
    }

    @Test
    @DisplayName("mixed case normalized then validated")
    void mixed_case_normalization() {
      int precision = 6;
      String lower = GeoHashUtils.toGeoHash(TEST_LATITUDE, TEST_LONGITUDE, precision);
      String upper = lower.toUpperCase(Locale.ROOT);

      double[] fromLower = GeoHashUtils.fromGeoHash(lower);
      double[] fromUpper = GeoHashUtils.fromGeoHash(upper);

      assertEquals(fromLower[0], fromUpper[0], 1e-12);
      assertEquals(fromLower[1], fromUpper[1], 1e-12);
    }

    @Test
    @DisplayName("invalid array inputs rejected")
    void invalid_array_inputs() {
      assertThrows(IllegalArgumentException.class, () -> GeoHashUtils.fromGeoHash((String[]) null));
      assertThrows(IllegalArgumentException.class, () -> GeoHashUtils.fromGeoHash(new String[0]));
      assertThrows(IllegalArgumentException.class, () -> GeoHashUtils.fromGeoHash(new String[]{null}));
      assertThrows(IllegalArgumentException.class, () -> GeoHashUtils.fromGeoHash(new String[]{"r", "3", "gx"}));
      assertThrows(IllegalArgumentException.class, () -> GeoHashUtils.fromGeoHash(new String[]{"r", "3", "I"}));
    }
  }

  @Nested
  class Concurrency {

    @RepeatedTest(3)
    @Timeout(2)
    @DisplayName("parallel encode/decode smoke-test without exceptions")
    void parallel_smoke_test() {
      int threads = Runtime.getRuntime().availableProcessors();
      int samplesPerThread = 1000;

      assertDoesNotThrow(() -> IntStream.range(0, threads).parallel().forEach(t -> {
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < samplesPerThread; i++) {
          double latitude = random.nextDouble(-90.0, 90.0);
          double longitude = random.nextDouble(-180.0, 180.0);
          int precision = random.nextInt(1, 13);

          String geohash = GeoHashUtils.toGeoHash(latitude, longitude, precision);
          assertEquals(precision, geohash.length());

          double[] center = GeoHashUtils.fromGeoHash(geohash);
          String rehashed = GeoHashUtils.toGeoHash(center[0], center[1], precision);
          assertEquals(geohash, rehashed);

          String topicPath = GeoHashUtils.toTopicNameGeoHash(latitude, longitude, precision);
          long slashCount = topicPath.chars().filter(c -> c == '/').count();
          assertEquals(precision, slashCount);
        }
      }));
    }
  }
}
