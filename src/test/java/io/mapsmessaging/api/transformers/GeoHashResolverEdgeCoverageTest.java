package io.mapsmessaging.api.transformers;

import io.mapsmessaging.api.message.Filter;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.config.transformer.GeoHashResolverTransformationConfig;
import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.utilities.GeoHashUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.util.stream.Stream;

import static io.mapsmessaging.api.transformers.TransformationTestSupport.mockMessage;
import static io.mapsmessaging.api.transformers.TransformationTestSupport.parsedMessage;
import static io.mapsmessaging.api.transformers.TransformationTestSupport.utf8Bytes;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GeoHashResolverEdgeCoverageTest {

  @ParameterizedTest
  @MethodSource("unitLocationCases")
  void equivalentUnitEncodingsResolveToSameRawGeohash(
      String units, double latitude, double longitude) {
    GeoHashResolver resolver = resolver(
        "prefix", "/geo/{geohash}",
        "precision", "8",
        "layout", "raw",
        "units", units);

    Object encodedLatitude = encode(latitude, units);
    Object encodedLongitude = encode(longitude, units);
    ParsedMessage result = transform(resolver, encodedLatitude, encodedLongitude);

    assertNotNull(result);
    assertEquals(
        "/geo/" + GeoHashUtils.toGeoHash(latitude, longitude, 8),
        result.getDestinationName());
  }

  @ParameterizedTest
  @MethodSource("layoutLocationCases")
  void everyLayoutProducesItsDefinedTopicShape(
      String layout, double latitude, double longitude) {
    GeoHashResolver resolver = resolver(
        "prefix", "/geo/{geohash}",
        "precision", "7",
        "layout", layout,
        "units", "deg");

    ParsedMessage result = transform(resolver, latitude, longitude);

    assertNotNull(result);
    String geohash = GeoHashUtils.toGeoHash(latitude, longitude, 7);
    String suffix = switch (layout) {
      case "raw" -> "/" + geohash;
      case "two-per-segment" -> twoPerSegment(geohash);
      default -> GeoHashUtils.toTopicNameGeoHash(latitude, longitude, 7);
    };
    assertEquals("/geo" + suffix, result.getDestinationName());
  }

  @ParameterizedTest
  @MethodSource("stringLocationCases")
  void numericStringsAreAcceptedAsCoordinates(double latitude, double longitude) {
    GeoHashResolver resolver = resolver(
        "prefix", "/geo/{geohash}",
        "precision", "6",
        "layout", "raw",
        "units", "deg");

    ParsedMessage result =
        transform(resolver, Double.toString(latitude), Double.toString(longitude));

    assertNotNull(result);
    assertEquals(
        "/geo/" + GeoHashUtils.toGeoHash(latitude, longitude, 6),
        result.getDestinationName());
  }

  @ParameterizedTest
  @MethodSource("autoScaledDegreeCases")
  void degreeModeRecognisesMavlinkE7Coordinates(double latitude, double longitude) {
    GeoHashResolver resolver = resolver(
        "prefix", "/geo/{geohash}",
        "precision", "6",
        "layout", "raw",
        "units", "deg");

    ParsedMessage result = transform(resolver, latitude * 1e7d, longitude * 1e7d);

    assertNotNull(result);
    assertEquals(
        "/geo/" + GeoHashUtils.toGeoHash(latitude, longitude, 6),
        result.getDestinationName());
  }

  @ParameterizedTest
  @MethodSource("alternateKeyCases")
  void alternateKeysResolveWhenPrimaryKeysAreAbsent(
      String latKeys, String lonKeys, String resolvedLatKey, String resolvedLonKey,
      double latitude, double longitude) {
    GeoHashResolver resolver = resolver(
        "prefix", "/geo/{geohash}",
        "precision", "6",
        "layout", "raw",
        "latKeys", latKeys,
        "lonKeys", lonKeys);

    IdentifierResolver identifiers = mock(IdentifierResolver.class, withSettings().lenient());
    when(identifiers.get("latitude")).thenReturn(null);
    when(identifiers.get("longitude")).thenReturn(null);
    when(identifiers.get(resolvedLatKey)).thenReturn(latitude);
    when(identifiers.get(resolvedLonKey)).thenReturn(longitude);

    ParsedMessage result = transform(resolver, identifiers);

    assertNotNull(result);
    assertEquals(
        "/geo/" + GeoHashUtils.toGeoHash(latitude, longitude, 6),
        result.getDestinationName());
  }

  @ParameterizedTest
  @MethodSource("missingCases")
  void missingAndInvalidCoordinatePoliciesAreApplied(
      String policy, Object latitude, Object longitude, boolean resolverPresent,
      boolean dropped, boolean destinationChanged) {
    GeoHashResolver resolver = resolver(
        "prefix", "/geo",
        "precision", "5",
        "layout", "raw",
        "onMissing", policy,
        "defaultLatitude", "-33.8688",
        "defaultLongitude", "151.2093");

    ParsedMessage parsed = parsedMessage("/original", mockMessage(utf8Bytes("{}")));
    ParsedMessage result;
    try (MockedStatic<Filter> filter = mockStatic(Filter.class)) {
      if (resolverPresent) {
        IdentifierResolver identifiers = mock(IdentifierResolver.class, withSettings().lenient());
        when(identifiers.get("latitude")).thenReturn(latitude);
        when(identifiers.get("longitude")).thenReturn(longitude);
        filter.when(() -> Filter.getTopicResolver(anyString(), any())).thenReturn(identifiers);
      } else {
        filter.when(() -> Filter.getTopicResolver(anyString(), any())).thenReturn(null);
      }
      result = resolver.transform("/source", parsed);
    }

    if (dropped) {
      assertNull(result);
      return;
    }
    assertSame(parsed, result);
    if (destinationChanged) {
      assertEquals(
          "/geo/" + GeoHashUtils.toGeoHash(-33.8688, 151.2093, 5),
          result.getDestinationName());
    } else {
      assertEquals("/original", result.getDestinationName());
    }
  }

  @ParameterizedTest
  @MethodSource("legacyDefaultCases")
  void legacyDefaultPairIsParsedAndApplied(
      String defaultPair, String layout, double latitude, double longitude) {
    GeoHashResolver resolver = resolver(
        "prefix", "/fallback",
        "precision", "6",
        "layout", layout,
        "onMissing", "defaultTo",
        "defaultTo", defaultPair);

    ParsedMessage parsed = parsedMessage("/original", mockMessage(utf8Bytes("{}")));
    ParsedMessage result;
    try (MockedStatic<Filter> filter = mockStatic(Filter.class)) {
      filter.when(() -> Filter.getTopicResolver(anyString(), any())).thenReturn(null);
      result = resolver.transform("/source", parsed);
    }

    assertNotNull(result);
    String geohash = GeoHashUtils.toGeoHash(latitude, longitude, 6);
    String suffix = switch (layout) {
      case "raw" -> "/" + geohash;
      case "two-per-segment" -> twoPerSegment(geohash);
      default -> GeoHashUtils.toTopicNameGeoHash(latitude, longitude, 6);
    };
    assertEquals("/fallback" + suffix, result.getDestinationName());
  }

  private static ParsedMessage transform(
      GeoHashResolver resolver, Object latitude, Object longitude) {
    IdentifierResolver identifiers = mock(IdentifierResolver.class, withSettings().lenient());
    when(identifiers.get("latitude")).thenReturn(latitude);
    when(identifiers.get("longitude")).thenReturn(longitude);
    return transform(resolver, identifiers);
  }

  private static ParsedMessage transform(
      GeoHashResolver resolver, IdentifierResolver identifiers) {
    ParsedMessage parsed = parsedMessage("/original", mockMessage(utf8Bytes("{}")));
    try (MockedStatic<Filter> filter = mockStatic(Filter.class)) {
      filter.when(() -> Filter.getTopicResolver(anyString(), any())).thenReturn(identifiers);
      return resolver.transform("/source", parsed);
    }
  }

  private static GeoHashResolver resolver(String... values) {
    ConfigurationProperties parameters = new ConfigurationProperties();
    for (int i = 0; i < values.length; i += 2) {
      parameters.put(values[i], values[i + 1]);
    }
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("parameters", parameters);
    GeoHashResolverTransformationConfig config = new GeoHashResolverTransformationConfig(root);
    return (GeoHashResolver) new GeoHashResolver().build(config);
  }

  private static Object encode(double value, String units) {
    return switch (units) {
      case "rad" -> Math.toRadians(value);
      case "e7" -> value * 1e7d;
      case "micros" -> value * 1e6d;
      default -> value;
    };
  }

  private static String twoPerSegment(String geohash) {
    StringBuilder builder = new StringBuilder("/");
    for (int i = 0; i < geohash.length(); i += 2) {
      int end = Math.min(i + 2, geohash.length());
      builder.append(geohash, i, end);
      if (end < geohash.length()) {
        builder.append('/');
      }
    }
    return builder.toString();
  }

  private static Stream<Arguments> unitLocationCases() {
    return locations().flatMap(location -> Stream.of("deg", "rad", "e7", "micros")
        .map(units -> Arguments.of(units, location[0], location[1])));
  }

  private static Stream<Arguments> layoutLocationCases() {
    return locations().flatMap(location ->
        Stream.of("chars-per-segment", "two-per-segment", "raw")
            .map(layout -> Arguments.of(layout, location[0], location[1])));
  }

  private static Stream<Arguments> stringLocationCases() {
    return locations().map(location -> Arguments.of(location[0], location[1]));
  }

  private static Stream<Arguments> autoScaledDegreeCases() {
    return Stream.of(
        Arguments.of(-33.8688, 151.2093),
        Arguments.of(51.5074, -0.1278),
        Arguments.of(37.7749, -122.4194),
        Arguments.of(35.6762, 139.6503),
        Arguments.of(-22.9068, -43.1729),
        Arguments.of(-33.9249, 18.4241),
        Arguments.of(64.1466, -21.9426),
        Arguments.of(1.3521, 103.8198));
  }

  private static Stream<Arguments> alternateKeyCases() {
    return Stream.of(
        Arguments.of("lat", "lon", "lat", "lon", -33.8688, 151.2093),
        Arguments.of("missing,lat", "missing,lon", "lat", "lon", 51.5074, -0.1278),
        Arguments.of("x,y,lat", "a,b,lon", "lat", "lon", 37.7749, -122.4194),
        Arguments.of("latitudeDeg", "longitudeDeg", "latitudeDeg", "longitudeDeg", 35.6762, 139.6503),
        Arguments.of("gpsLat,lat", "gpsLon,lon", "gpsLat", "gpsLon", -22.9068, -43.1729),
        Arguments.of("lat1,lat2", "lon1,lon2", "lat2", "lon2", -33.9249, 18.4241),
        Arguments.of("northing,latitudeDeg", "easting,longitudeDeg", "latitudeDeg", "longitudeDeg", 64.1466, -21.9426),
        Arguments.of("lat,latitude", "lon,longitude", "lat", "lon", 1.3521, 103.8198));
  }

  private static Stream<Arguments> missingCases() {
    return Stream.of(
        Arguments.of("skip", null, null, true, false, false),
        Arguments.of("drop", null, null, true, true, false),
        Arguments.of("defaultTo", null, null, true, false, true),
        Arguments.of("skip", "not-a-number", "151.2", true, false, false),
        Arguments.of("drop", "not-a-number", "151.2", true, true, false),
        Arguments.of("defaultTo", "not-a-number", "151.2", true, false, true),
        Arguments.of("skip", "-33.8", new Object(), true, false, false),
        Arguments.of("drop", "-33.8", new Object(), true, true, false),
        Arguments.of("defaultTo", "-33.8", new Object(), true, false, true),
        Arguments.of("skip", null, null, false, false, false),
        Arguments.of("drop", null, null, false, true, false),
        Arguments.of("defaultTo", null, null, false, false, true));
  }

  private static Stream<Arguments> legacyDefaultCases() {
    return Stream.of(
        Arguments.of("-33.8688,151.2093", "raw", -33.8688, 151.2093),
        Arguments.of("-33.8688,151.2093", "two-per-segment", -33.8688, 151.2093),
        Arguments.of("-33.8688,151.2093", "chars-per-segment", -33.8688, 151.2093),
        Arguments.of("51.5074,-0.1278", "raw", 51.5074, -0.1278),
        Arguments.of("51.5074,-0.1278", "two-per-segment", 51.5074, -0.1278),
        Arguments.of("51.5074,-0.1278", "chars-per-segment", 51.5074, -0.1278));
  }

  private static Stream<double[]> locations() {
    return Stream.of(
        new double[]{0.0, 0.0},
        new double[]{-33.8688, 151.2093},
        new double[]{51.5074, -0.1278},
        new double[]{37.7749, -122.4194},
        new double[]{35.6762, 139.6503},
        new double[]{-22.9068, -43.1729},
        new double[]{-33.9249, 18.4241},
        new double[]{64.1466, -21.9426},
        new double[]{1.3521, 103.8198},
        new double[]{48.8566, 2.3522},
        new double[]{89.9999, 179.9999},
        new double[]{-89.9999, -179.9999});
  }
}
