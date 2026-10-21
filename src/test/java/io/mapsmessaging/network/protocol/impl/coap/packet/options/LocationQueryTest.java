package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationQueryTest {

  @Test
  void locationQueryUsesRegisteredIdAndQueryDelimiter() {
    LocationQuery option = new LocationQuery();
    option.setPath("page=1&limit=10");

    assertEquals(Constants.LOCATION_QUERY, option.getId());
    assertEquals("page=1&limit=10", option.toString());
    assertEquals(2, option.getPath().size());
  }
}