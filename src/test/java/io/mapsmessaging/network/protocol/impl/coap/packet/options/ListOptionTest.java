package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListOptionTest {

  @Test
  void updatesAccumulateRepeatedOptionValuesInOrder() {
    ListOption option = new ListOption(8);
    byte[] first = new byte[]{1, 2};
    byte[] second = new byte[]{3};

    option.update(first);
    option.update(second);

    assertEquals(2, option.getList().size());
    assertSame(first, option.getList().get(0));
    assertSame(second, option.getList().get(1));
  }

  @Test
  void newListStartsEmptyAndHasNoSinglePackedRepresentation() {
    ListOption option = new ListOption(8);

    assertTrue(option.getList().isEmpty());
    assertArrayEquals(new byte[0], option.pack());
  }
}
