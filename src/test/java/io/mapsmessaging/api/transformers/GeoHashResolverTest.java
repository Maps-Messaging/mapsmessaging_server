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

import io.mapsmessaging.api.message.Filter;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.utilities.GeoHashUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static io.mapsmessaging.api.transformers.TransformationAssertions.assertDropped;
import static io.mapsmessaging.api.transformers.TransformationAssertions.assertNotDropped;
import static io.mapsmessaging.api.transformers.TransformationTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeoHashResolverTest extends AbstractDroppingTransformationTest {

  @BeforeAll
  static void setup() {
    SchemaManager.getInstance().addSchema(SOURCE, JSON_SCHEMA_CONFIG);
  }

  private static ConfigurationProperties config(String... kvPairs) {
    ConfigurationProperties parameters = new ConfigurationProperties();
    for (int i = 0; i < kvPairs.length; i += 2) {
      parameters.put(kvPairs[i], kvPairs[i + 1]);
    }
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("parameters", parameters);
    return root;
  }

  private static String twoPerSegment(String geohash) {
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

  @Override
  protected InterServerTransformation createTransformer() {
    return new GeoHashResolver().build(config(
        "prefix", "",
        "latKey", "latitude",
        "lonKey", "longitude",
        "precision", "5",
        "splitHash", "true"
    ));
  }

  @Override
  protected byte[] validInputBytes() {
    return utf8Bytes("{}");
  }

  @Test
  void transform_withLatLon_setsDestination_usingDefaultLayout() {
    GeoHashResolver resolver = (GeoHashResolver) new GeoHashResolver().build(config(
        "prefix", "/geo",
        "latKey", "latitude",
        "lonKey", "longitude",
        "precision", "6",
        "layout", "chars-per-segment"
    ));

    IdentifierResolver id = mock(IdentifierResolver.class, withSettings().lenient());
    when(id.get("latitude")).thenReturn(-33.8688);
    when(id.get("longitude")).thenReturn(151.2093);

    Protocol.ParsedMessage parsed = parsedMessage("/dst", mockMessage(utf8Bytes("{}")));

    try (MockedStatic<Filter> filter = mockStatic(Filter.class)) {
      filter.when(() -> Filter.getTopicResolver(anyString(), any())).thenReturn(id);

      Protocol.ParsedMessage result = resolver.transform("/src/topic", parsed);

      assertNotDropped(result);

      String expectedSuffix = GeoHashUtils.toTopicNameGeoHash(-33.8688, 151.2093, 6);
      assertEquals("/geo" + expectedSuffix, result.getDestinationName());
    }
  }

  @Test
  void transform_layoutRaw_usesRawGeohashPath() {
    GeoHashResolver resolver = (GeoHashResolver) new GeoHashResolver().build(config(
        "prefix", "",
        "latKey", "latitude",
        "lonKey", "longitude",
        "precision", "5",
        "layout", "raw"
    ));

    IdentifierResolver id = mock(IdentifierResolver.class, withSettings().lenient());
    when(id.get("latitude")).thenReturn(-33.8688);
    when(id.get("longitude")).thenReturn(151.2093);

    Protocol.ParsedMessage parsed = parsedMessage("/dst", mockMessage(utf8Bytes("{}")));

    try (MockedStatic<Filter> filter = mockStatic(Filter.class)) {
      filter.when(() -> Filter.getTopicResolver(anyString(), any())).thenReturn(id);

      Protocol.ParsedMessage result = resolver.transform("/src/topic", parsed);

      assertNotDropped(result);

      String geohash = GeoHashUtils.toGeoHash(-33.8688, 151.2093, 5);
      assertEquals("/" + geohash, result.getDestinationName());
    }
  }

  @Test
  void transform_layoutTwoPerSegment_splitsEveryTwoChars() {
    GeoHashResolver resolver = (GeoHashResolver) new GeoHashResolver().build(config(
        "prefix", "/geo",
        "latKey", "latitude",
        "lonKey", "longitude",
        "precision", "5",
        "layout", "two-per-segment"
    ));

    IdentifierResolver id = mock(IdentifierResolver.class, withSettings().lenient());
    when(id.get("latitude")).thenReturn(-33.8688);
    when(id.get("longitude")).thenReturn(151.2093);

    Protocol.ParsedMessage parsed = parsedMessage("/dst", mockMessage(utf8Bytes("{}")));

    try (MockedStatic<Filter> filter = mockStatic(Filter.class)) {
      filter.when(() -> Filter.getTopicResolver(anyString(), any())).thenReturn(id);

      Protocol.ParsedMessage result = resolver.transform("/src/topic", parsed);

      assertNotDropped(result);

      String geohash = GeoHashUtils.toGeoHash(-33.8688, 151.2093, 5);
      String expected = "/geo" + twoPerSegment(geohash);
      assertEquals(expected, result.getDestinationName());
    }
  }

  @Test
  void transform_unitsRad_convertsRadiansToDegrees() {
    GeoHashResolver resolver = (GeoHashResolver) new GeoHashResolver().build(config(
        "prefix", "/geo",
        "latKey", "latitude",
        "lonKey", "longitude",
        "precision", "5",
        "units", "rad"
    ));

    double latRad = Math.toRadians(-33.8688);
    double lonRad = Math.toRadians(151.2093);

    IdentifierResolver id = mock(IdentifierResolver.class, withSettings().lenient());
    when(id.get("latitude")).thenReturn(latRad);
    when(id.get("longitude")).thenReturn(lonRad);

    Protocol.ParsedMessage parsed = parsedMessage("/dst", mockMessage(utf8Bytes("{}")));

    try (MockedStatic<Filter> filter = mockStatic(Filter.class)) {
      filter.when(() -> Filter.getTopicResolver(anyString(), any())).thenReturn(id);

      Protocol.ParsedMessage result = resolver.transform("/src/topic", parsed);

      assertNotDropped(result);

      String expectedSuffix = GeoHashUtils.toTopicNameGeoHash(-33.8688, 151.2093, 5);
      assertEquals("/geo" + expectedSuffix, result.getDestinationName());
    }
  }

  @Test
  void transform_missingKeys_onMissingSkip_leavesDestinationUnchanged() {
    GeoHashResolver resolver = (GeoHashResolver) new GeoHashResolver().build(config(
        "prefix", "/geo",
        "latKey", "latitude",
        "lonKey", "longitude",
        "precision", "5",
        "onMissing", "skip"
    ));

    IdentifierResolver id = mock(IdentifierResolver.class, withSettings().lenient());
    when(id.get("latitude")).thenReturn(null);
    when(id.get("longitude")).thenReturn(null);

    Protocol.ParsedMessage parsed = parsedMessage("/dst", mockMessage(utf8Bytes("{}")));

    try (MockedStatic<Filter> filter = mockStatic(Filter.class)) {
      filter.when(() -> Filter.getTopicResolver(anyString(), any())).thenReturn(id);

      Protocol.ParsedMessage result = resolver.transform("/src/topic", parsed);

      assertNotDropped(result);
      assertEquals("/dst", result.getDestinationName());
    }
  }

  @Test
  void transform_missingKeys_onMissingDrop_returnsNull() {
    GeoHashResolver resolver = (GeoHashResolver) new GeoHashResolver().build(config(
        "prefix", "/geo",
        "latKey", "latitude",
        "lonKey", "longitude",
        "precision", "5",
        "onMissing", "drop"
    ));

    IdentifierResolver id = mock(IdentifierResolver.class, withSettings().lenient());
    when(id.get("latitude")).thenReturn(null);
    when(id.get("longitude")).thenReturn(null);

    Protocol.ParsedMessage parsed = parsedMessage("/dst", mockMessage(utf8Bytes("{}")));

    try (MockedStatic<Filter> filter = mockStatic(Filter.class)) {
      filter.when(() -> Filter.getTopicResolver(anyString(), any())).thenReturn(id);

      Protocol.ParsedMessage result = resolver.transform("/src/topic", parsed);

      assertDropped(result);
    }
  }

  @Test
  void transform_missingKeys_onMissingDefaultTo_setsDestinationFromDefault() {
    GeoHashResolver resolver = (GeoHashResolver) new GeoHashResolver().build(config(
        "prefix", "/geo",
        "latKey", "latitude",
        "lonKey", "longitude",
        "precision", "4",
        "onMissing", "defaultTo",
        "defaultTo", "0,0"
    ));

    IdentifierResolver id = mock(IdentifierResolver.class, withSettings().lenient());
    when(id.get("latitude")).thenReturn(null);
    when(id.get("longitude")).thenReturn(null);

    Protocol.ParsedMessage parsed = parsedMessage("/dst", mockMessage(utf8Bytes("{}")));

    try (MockedStatic<Filter> filter = mockStatic(Filter.class)) {
      filter.when(() -> Filter.getTopicResolver(anyString(), any())).thenReturn(id);

      Protocol.ParsedMessage result = resolver.transform("/src/topic", parsed);

      assertNotDropped(result);

      String expectedSuffix = GeoHashUtils.toTopicNameGeoHash(0.0, 0.0, 4);
      assertEquals("/geo" + expectedSuffix, result.getDestinationName());
    }
  }

  @Test
  void transform_usesAlternateKeys_whenPrimaryMissing() {
    GeoHashResolver resolver = (GeoHashResolver) new GeoHashResolver().build(config(
        "prefix", "/geo",
        "latKey", "latitude",
        "lonKey", "longitude",
        "precision", "5",
        "latKeys", "lat,latitudeDeg",
        "lonKeys", "lon,longitudeDeg"
    ));

    IdentifierResolver id = mock(IdentifierResolver.class, withSettings().lenient());
    when(id.get("latitude")).thenReturn(null);
    when(id.get("longitude")).thenReturn(null);
    when(id.get("lat")).thenReturn(-33.8688);
    when(id.get("lon")).thenReturn(151.2093);

    Protocol.ParsedMessage parsed = parsedMessage("/dst", mockMessage(utf8Bytes("{}")));

    try (MockedStatic<Filter> filter = mockStatic(Filter.class)) {
      filter.when(() -> Filter.getTopicResolver(anyString(), any())).thenReturn(id);

      Protocol.ParsedMessage result = resolver.transform("/src/topic", parsed);

      assertNotDropped(result);

      String expectedSuffix = GeoHashUtils.toTopicNameGeoHash(-33.8688, 151.2093, 5);
      assertEquals("/geo" + expectedSuffix, result.getDestinationName());
    }
  }

  @Test
  void metadata_isStable() {
    InterServerTransformation created = new GeoHashResolver();
    assertEquals("GeoHash", created.getName());
    assertNotNull(created.getDescription());
    assertFalse(created.getDescription().isBlank());
  }
}
