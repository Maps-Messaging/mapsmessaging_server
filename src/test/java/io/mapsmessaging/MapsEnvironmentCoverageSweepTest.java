package io.mapsmessaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapsEnvironmentCoverageSweepTest {
  @Test
  void systemPropertiesOverrideHomeAndDataResolution() {
    String oldHome = System.getProperty("MAPS_HOME");
    String oldData = System.getProperty("MAPS_DATA");
    try {
      System.setProperty("MAPS_HOME", "/tmp/maps-home");
      System.setProperty("MAPS_DATA", "/tmp/maps-data");

      assertEquals("/tmp/maps-home", MapsEnvironment.getMapsHome());
      assertEquals("/tmp/maps-data", MapsEnvironment.getMapsData());
    } finally {
      restore("MAPS_HOME", oldHome);
      restore("MAPS_DATA", oldData);
    }
  }

  private static void restore(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }
}
