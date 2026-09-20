package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UriQueryTest {

  @Test
  void uriQueryUsesRegisteredOptionIdAndAmpersandComposition() {
    UriQuery option = new UriQuery();
    option.add("sensor=temp");
    option.add("unit=c");

    assertEquals(Constants.URI_QUERY, option.getId());
    assertEquals("sensor=temp&unit=c", option.toString());
  }
}