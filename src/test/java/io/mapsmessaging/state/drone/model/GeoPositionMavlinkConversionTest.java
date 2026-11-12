package io.mapsmessaging.state.drone.model;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.network.protocol.impl.mavlink.messages.MavlinkPosition;
import org.junit.jupiter.api.Test;

class GeoPositionMavlinkConversionTest {

  @Test
  void preservesMslAltitude() {
    GeoPosition position = new GeoPosition(-33.86, 151.2, 120.0, null, null);
    MavlinkPosition mavlink = position.toMavlinkPosition();
    assertEquals(MavlinkPosition.GLOBAL_INT, mavlink.getFrame());
    GeoPosition restored = new GeoPosition(mavlink);
    assertEquals(position, restored);
  }

  @Test
  void preservesTerrainAltitude() {
    GeoPosition position = new GeoPosition(-33.86, 151.2, null, 35.0, null);
    MavlinkPosition mavlink = position.toMavlinkPosition();
    assertEquals(MavlinkPosition.GLOBAL_TERRAIN_ALT_INT, mavlink.getFrame());
    assertEquals(position, new GeoPosition(mavlink));
  }

  @Test
  void preservesHomeRelativeAltitude() {
    GeoPosition position = new GeoPosition(-33.86, 151.2, null, null, 15.0);
    MavlinkPosition mavlink = position.toMavlinkPosition();
    assertEquals(MavlinkPosition.GLOBAL_RELATIVE_ALT_INT, mavlink.getFrame());
    assertEquals(position, new GeoPosition(mavlink));
  }

  @Test
  void acceptsNonIntFrames() {
    assertEquals(100.0, new GeoPosition(new MavlinkPosition(1.0, 2.0, 100.0, MavlinkPosition.GLOBAL)).getAltitudeMslMeters());
    assertEquals(20.0, new GeoPosition(new MavlinkPosition(1.0, 2.0, 20.0, MavlinkPosition.GLOBAL_TERRAIN_ALT)).getAltitudeAglMeters());
    assertEquals(10.0, new GeoPosition(new MavlinkPosition(1.0, 2.0, 10.0, MavlinkPosition.GLOBAL_RELATIVE_ALT)).getAltitudeRelativeMeters());
  }

  @Test
  void rejectsInvalidFrame() {
    assertThrows(IllegalArgumentException.class, () -> new MavlinkPosition(0.0, 0.0, 1.0, -1));
  }
}
