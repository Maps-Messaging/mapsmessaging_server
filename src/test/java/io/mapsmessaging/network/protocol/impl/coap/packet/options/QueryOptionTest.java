package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryOptionTest {

  @Test
  void queryPartsCanBeSetUpdatedAndRenderedWithAmpersands() {
    QueryOption option = new QueryOption(99);

    option.setPath("a=1&b=2");
    option.update("c=3".getBytes());

    assertEquals(99, option.getId());
    assertEquals(3, option.getPath().size());
    assertEquals("a=1&b=2&c=3", option.toString());
    assertArrayEquals(new byte[0], option.pack());
  }
}