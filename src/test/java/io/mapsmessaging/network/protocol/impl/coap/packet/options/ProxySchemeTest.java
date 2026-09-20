package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProxySchemeTest {

  @Test
  void proxySchemeRoundTripsStringPayload() throws Exception {
    ProxyScheme option = new ProxyScheme();
    option.setValue("coaps");

    assertEquals(Constants.PROXY_SCHEME, option.getId());
    assertArrayEquals("coaps".getBytes(StandardCharsets.UTF_8), option.pack());

    option.update("coap".getBytes(StandardCharsets.UTF_8));
    assertEquals("coap", option.getValue());
  }
}