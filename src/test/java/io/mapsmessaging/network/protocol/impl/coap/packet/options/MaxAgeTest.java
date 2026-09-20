package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaxAgeTest {

  @Test
  void maxAgeRoundTripsUnsignedSecondsAndZeroPacksEmpty() throws Exception {
    MaxAge option = new MaxAge();
    option.setValue(3600L);

    assertEquals(Constants.MAX_AGE, option.getId());
    byte[] packed = option.pack();

    MaxAge restored = new MaxAge();
    restored.update(packed);
    assertEquals(3600L, restored.getValue());

    restored.setValue(0);
    assertArrayEquals(new byte[0], restored.pack());
  }
}