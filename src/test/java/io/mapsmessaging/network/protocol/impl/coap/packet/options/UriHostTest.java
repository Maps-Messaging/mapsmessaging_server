package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UriHostTest {

  @Test
  void uriHostRoundTripsHostnamePayload() throws Exception {
    UriHost option = new UriHost();
    option.setValue("edge.maps.local");

    assertEquals(Constants.URI_HOST, option.getId());
    assertArrayEquals("edge.maps.local".getBytes(StandardCharsets.UTF_8), option.pack());

    option.update("backup.maps.local".getBytes(StandardCharsets.UTF_8));
    assertEquals("backup.maps.local", option.getValue());
  }
}