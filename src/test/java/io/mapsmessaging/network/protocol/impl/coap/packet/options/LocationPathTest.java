package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocationPathTest {

  @Test
  void locationPathSplitsAndRendersSlashSeparatedSegments() {
    LocationPath option = new LocationPath();
    option.setPath("api/v1/device");

    assertEquals(Constants.LOCATION_PATH, option.getId());
    assertEquals(List.of("api", "v1", "device"), option.getPath());
    assertEquals("api/v1/device", option.toString());

    option.update("status".getBytes());
    assertEquals("api/v1/device/status", option.toString());
  }
}