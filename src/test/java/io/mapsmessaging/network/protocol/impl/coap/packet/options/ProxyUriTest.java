package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProxyUriTest {

  @Test
  void proxyUriRoundTripsTextPayload() throws Exception {
    ProxyUri option = new ProxyUri();
    option.setValue("coap://proxy.example/resource");

    assertEquals(Constants.PROXY_URI, option.getId());
    assertArrayEquals(
        "coap://proxy.example/resource".getBytes(StandardCharsets.UTF_8),
        option.pack());

    option.update("coaps://secure.example".getBytes(StandardCharsets.UTF_8));
    assertEquals("coaps://secure.example", option.getValue());
  }
}