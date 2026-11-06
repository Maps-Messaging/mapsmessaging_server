package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashLayout;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashOnMissingPolicy;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashUnits;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoHashResolverTransformationConfigTest {

  @Test
  void legacyParametersParseAlternateKeysUnitsLayoutAndDefaultCoordinates() {
    ConfigurationProperties parameters = new ConfigurationProperties();
    parameters.put("latKeys", "lat, latitudeDeg");
    parameters.put("lonKeys", "lon, longitudeDeg");
    parameters.put("units", "e7");
    parameters.put("layout", "two-per-segment");
    parameters.put("onMissing", "defaultTo");
    parameters.put("defaultTo", "38.5,-9.1");
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("parameters", parameters);

    GeoHashResolverTransformationConfig config = new GeoHashResolverTransformationConfig(root);

    assertEquals(List.of("lat", "latitudeDeg"), config.getLatKeys());
    assertEquals(List.of("lon", "longitudeDeg"), config.getLonKeys());
    assertEquals(GeoHashUnits.E7, config.getUnits());
    assertEquals(GeoHashLayout.TWO_PER_SEGMENT, config.getLayout());
    assertEquals(GeoHashOnMissingPolicy.DEFAULT_TO, config.getOnMissing());
    assertEquals(38.5, config.getDefaultLatitude(), 0.0);
    assertEquals(-9.1, config.getDefaultLongitude(), 0.0);
  }

  @Test
  void invalidEnumsFallBackToSafeDefaultsAndBlankCsvProducesEmptyLists() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("units", "parsecs");
    properties.put("layout", "chaos");
    properties.put("onMissing", "explode");
    properties.put("latKeys", " , ");

    GeoHashResolverTransformationConfig config = new GeoHashResolverTransformationConfig(properties);

    assertEquals(GeoHashUnits.DEG, config.getUnits());
    assertEquals(GeoHashLayout.CHARS_PER_SEGMENT, config.getLayout());
    assertEquals(GeoHashOnMissingPolicy.SKIP, config.getOnMissing());
    assertTrue(config.getLatKeys().isEmpty());
  }

  @Test
  void explicitDefaultCoordinateFieldsAreUsedWhenLegacyPairIsAbsent() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("onMissing", "default-to");
    properties.put("defaultLatitude", 1.25);
    properties.put("defaultLongitude", 2.5);

    GeoHashResolverTransformationConfig config = new GeoHashResolverTransformationConfig(properties);

    assertEquals(1.25, config.getDefaultLatitude(), 0.0);
    assertEquals(2.5, config.getDefaultLongitude(), 0.0);
  }
}