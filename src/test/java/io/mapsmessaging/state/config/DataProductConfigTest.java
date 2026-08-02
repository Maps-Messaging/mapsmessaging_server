/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DataProductConfigTest {

  @Test
  void droneInfoDefensivelyCopiesConfiguredProducts() {
    DataProductConfig product = new DataProductConfig();
    product.setIdentifier("dp-rtsp-feed-001");
    product.setUri("rtsp://drone01/videofeed01");
    product.setProductType(Map.of("name", "video/rtsp"));
    product.setConformsTo(Map.of("name", "ONVIF Profile S"));

    DroneInfoDTO droneInfo = new DroneInfoDTO();
    droneInfo.setDataProducts(List.of(product));

    assertEquals(1, droneInfo.getDataProducts().size());
    assertEquals("rtsp://drone01/videofeed01", droneInfo.getDataProducts().get(0).getUri());
  }
}
