package io.mapsmessaging.utilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemPropertiesTest {

  private static final String STRING_KEY = "maps.test.system.string";
  private static final String BOOL_KEY = "maps.test.system.bool";
  private static final String LONG_KEY = "maps.test.system.long";
  private static final String DOUBLE_KEY = "maps.test.system.double";

  @AfterEach
  void clearProperties() {
    System.clearProperty(STRING_KEY);
    System.clearProperty(BOOL_KEY);
    System.clearProperty(LONG_KEY);
    System.clearProperty(DOUBLE_KEY);
  }

  @Test
  void stringPropertyUsesConfiguredValueAndEmptyFallsBackToDefault() {
    SystemProperties properties = SystemProperties.getInstance();

    System.setProperty(STRING_KEY, "configured");
    assertEquals("configured", properties.getProperty(STRING_KEY, "default"));
    assertEquals("configured", properties.locateProperty(STRING_KEY, "default"));

    System.setProperty(STRING_KEY, "");
    assertEquals("default", properties.getProperty(STRING_KEY, "default"));
  }

  @Test
  void booleanLongAndDoubleParsingUseDefaultsForMissingOrInvalidValues() {
    SystemProperties properties = SystemProperties.getInstance();

    assertTrue(properties.getBooleanProperty(BOOL_KEY, true));
    System.setProperty(BOOL_KEY, "false");
    assertFalse(properties.getBooleanProperty(BOOL_KEY, true));

    System.setProperty(LONG_KEY, "12345");
    assertEquals(12345L, properties.getLongProperty(LONG_KEY, 7L));
    System.setProperty(LONG_KEY, "not-a-long");
    assertEquals(7L, properties.getLongProperty(LONG_KEY, 7L));

    System.setProperty(DOUBLE_KEY, "12.5");
    assertEquals(12.5, properties.getDoubleProperty(DOUBLE_KEY, 1.0), 0.0);
    System.setProperty(DOUBLE_KEY, "invalid");
    assertEquals(1.0, properties.getDoubleProperty(DOUBLE_KEY, 1.0), 0.0);
  }

  @Test
  void missingEnvironmentAndSystemPropertyFallsBack() {
    String impossible = "MAPS_TEST_PROPERTY_SHOULD_NOT_EXIST_9F3A17";
    System.clearProperty(impossible);

    assertEquals(
        "fallback",
        SystemProperties.getInstance().locateProperty(impossible, "fallback")
    );
  }
}
