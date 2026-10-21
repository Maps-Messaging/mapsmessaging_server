package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenericOptionTest {

  @Test
  void updateAndPackPreservePayload() throws Exception {
    GenericOption option = new GenericOption(2048);
    byte[] payload = new byte[]{0x01, 0x0f, 0x10, 0x41};

    option.update(payload);

    assertEquals(2048, option.getId());
    assertSame(payload, option.getValue());
    assertSame(payload, option.pack());
  }

  @Test
  void stringRepresentationContainsPaddedHexAndText() throws Exception {
    GenericOption option = new GenericOption(12);
    option.update(new byte[]{0x01, 0x0f, 0x10, 'A'});

    String text = option.toString();

    assertTrue(text.startsWith("Id:12["));
    assertTrue(text.contains("0x01"));
    assertTrue(text.contains("0x0f"));
    assertTrue(text.contains("0x10"));
    assertTrue(text.endsWith("<\u0001\u000f\u0010A>"));
  }

  @Test
  void nullPayloadFailsWhenRenderedRatherThanBeingSilentlyInvented() {
    GenericOption option = new GenericOption(1);

    assertNull(option.pack());
    assertThrows(NullPointerException.class, option::toString);
  }
}
