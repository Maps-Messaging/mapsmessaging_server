package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UriPortTest {

  @Test
  void uriPortRoundTripsNetworkPortValue() throws Exception {
    UriPort option = new UriPort();
    option.setValue(5683L);

    assertEquals(Constants.URI_PORT, option.getId());
    byte[] packed = option.pack();

    UriPort restored = new UriPort();
    restored.update(packed);
    assertEquals(5683L, restored.getValue());
  }
}