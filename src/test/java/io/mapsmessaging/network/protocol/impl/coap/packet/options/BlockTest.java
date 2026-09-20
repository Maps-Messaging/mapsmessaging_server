package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockTest {

  @Test
  void packAndUpdateRoundTripFlagsAndSmallBlockNumber() throws Exception {
    Block source = new Block(23, 15, true, 6);

    byte[] packed = source.pack();
    assertEquals(1, packed.length);

    Block restored = new Block(23);
    restored.update(packed);

    assertEquals(15, restored.getNumber());
    assertTrue(restored.isMore());
    assertEquals(6, restored.getSizeEx());
  }

  @Test
  void mediumAndLargeBlockNumbersChooseExpectedPayloadWidth() throws Exception {
    Block medium = new Block(23, 16, false, 0);
    assertEquals(2, medium.pack().length);

    Block large = new Block(23, 4096, false, 7);
    byte[] packed = large.pack();
    assertEquals(3, packed.length);

    Block restored = new Block(23);
    restored.update(packed);
    assertEquals(4096, restored.getNumber());
    assertFalse(restored.isMore());
    assertEquals(7, restored.getSizeEx());
  }

  @Test
  void nullAndEmptyPayloadResetState() throws Exception {
    Block block = new Block(23, 99, true, 7);

    block.update(null);
    assertEquals(0, block.getNumber());
    assertFalse(block.isMore());
    assertEquals(0, block.getSizeEx());

    block.setNumber(9);
    block.setMore(true);
    block.setSizeEx(4);
    block.update(new byte[0]);

    assertEquals(0, block.getNumber());
    assertFalse(block.isMore());
    assertEquals(0, block.getSizeEx());
  }

  @Test
  void sizeExponentIsMaskedToThreeBitsWhenPacked() throws Exception {
    Block source = new Block(23, 1, false, 15);
    Block restored = new Block(23);

    restored.update(source.pack());

    assertEquals(7, restored.getSizeEx());
  }
}
