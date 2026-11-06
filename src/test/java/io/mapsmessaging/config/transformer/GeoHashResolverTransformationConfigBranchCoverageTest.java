package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashLayout;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashOnMissingPolicy;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashUnits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoHashResolverTransformationConfigBranchCoverageTest {

  @Test
  void allExplicitUnitsLayoutsAndMissingPoliciesAreRecognized() {
    assertEquals(GeoHashUnits.RAD, config("rad", "raw", "drop").getUnits());
    assertEquals(GeoHashLayout.RAW, config("rad", "raw", "drop").getLayout());
    assertEquals(GeoHashOnMissingPolicy.DROP, config("rad", "raw", "drop").getOnMissing());

    assertEquals(GeoHashUnits.MICROS, config("micros", "two-per-segment", "skip").getUnits());
    assertEquals(
        GeoHashLayout.TWO_PER_SEGMENT,
        config("micros", "two-per-segment", "skip").getLayout());

    assertEquals(
        GeoHashOnMissingPolicy.DEFAULT_TO,
        config("deg", "chars-per-segment", "default_to").getOnMissing());
  }

  @Test
  void malformedLegacyDefaultPairIsIgnoredWithoutInventingCoordinates() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("onMissing", "defaultTo");
    properties.put("defaultTo", "not-a-coordinate");

    GeoHashResolverTransformationConfig config =
        new GeoHashResolverTransformationConfig(properties);

    assertEquals(GeoHashOnMissingPolicy.DEFAULT_TO, config.getOnMissing());
  }

  @Test
  void nonNumericLegacyDefaultPairIsIgnored() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("onMissing", "default-to");
    properties.put("defaultTo", "north,west");

    assertDoesNotThrow(() -> new GeoHashResolverTransformationConfig(properties));
  }

  @Test
  void blankCsvEntriesAreRemoved() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("latKeys", " lat, ,latitude, ");
    properties.put("lonKeys", "lon,, longitude");

    GeoHashResolverTransformationConfig config =
        new GeoHashResolverTransformationConfig(properties);

    assertEquals(java.util.List.of("lat", "latitude"), config.getLatKeys());
    assertEquals(java.util.List.of("lon", "longitude"), config.getLonKeys());
  }

  private static GeoHashResolverTransformationConfig config(
      String units,
      String layout,
      String onMissing) {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("units", units);
    properties.put("layout", layout);
    properties.put("onMissing", onMissing);
    return new GeoHashResolverTransformationConfig(properties);
  }
}