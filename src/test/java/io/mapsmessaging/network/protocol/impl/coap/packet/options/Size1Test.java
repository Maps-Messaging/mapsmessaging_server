package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Size1Test {

  @Test
  void size1RoundTripsUnsignedBinaryValue() throws Exception {
    Size1 option = new Size1();
    option.setValue(0x123456L);

    assertEquals(Constants.SIZE1, option.getId());
    assertArrayEquals(new byte[]{0x12, 0x34, 0x56}, option.pack());

    option.update(new byte[]{0x01, 0x00});
    assertEquals(256L, option.getValue());
  }
}