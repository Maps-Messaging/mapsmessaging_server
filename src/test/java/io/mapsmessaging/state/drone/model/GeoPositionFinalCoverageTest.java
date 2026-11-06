package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoPositionFinalCoverageTest {

  @Test
  void mslAltitudeTakesPrecedenceOverOtherAltitudeSources() {
    GeoPosition position = new GeoPosition(1.0, 2.0, 100.0, 50.0, 25.0);

    assertEquals(100.0, position.getPreferredAltitudeMeters());
    assertEquals("MSL", position.getPreferredAltitudeType());
  }

  @Test
  void aglAltitudeIsUsedWhenMslIsMissing() {
    GeoPosition position = new GeoPosition(1.0, 2.0, null, 50.0, 25.0);

    assertEquals(50.0, position.getPreferredAltitudeMeters());
    assertEquals("AGL", position.getPreferredAltitudeType());
  }

  @Test
  void relativeAltitudeIsUsedWhenMslAndAglAreMissing() {
    GeoPosition position = new GeoPosition(1.0, 2.0, null, null, 25.0);

    assertEquals(25.0, position.getPreferredAltitudeMeters());
    assertEquals("RELATIVE", position.getPreferredAltitudeType());
  }

  @Test
  void preferredAltitudeIsNullWhenNoAltitudeSourceExists() {
    GeoPosition position = new GeoPosition(1.0, 2.0, null, null, null);

    assertNull(position.getPreferredAltitudeMeters());
    assertNull(position.getPreferredAltitudeType());
  }
}