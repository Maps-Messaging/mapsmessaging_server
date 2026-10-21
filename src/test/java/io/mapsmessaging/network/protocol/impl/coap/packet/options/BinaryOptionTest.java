package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BinaryOptionTest {

  @Test
  void updateInterpretsNetworkOrderAndPackUsesMinimalBytes() throws Exception {
    BinaryOption option = new BinaryOption(99);

    option.update(new byte[]{0x01, 0x02, 0x03});

    assertEquals(0x010203L, option.getValue());
    assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, option.pack());
  }

  @Test
  void updateResetsExistingValueBeforeParsingNewBytes() throws Exception {
    BinaryOption option = new BinaryOption(1);
    option.setValue(0x12345678L);

    option.update(new byte[]{0x05});

    assertEquals(5L, option.getValue());
    assertArrayEquals(new byte[]{0x05}, option.pack());
  }

  @Test
  void zeroValuePacksToEmptyOptionPayload() throws Exception {
    BinaryOption option = new BinaryOption(1);

    option.update(new byte[0]);

    assertEquals(0L, option.getValue());
    assertArrayEquals(new byte[0], option.pack());
  }

  @Test
  void highBitBytesAreTreatedUnsigned() throws Exception {
    BinaryOption option = new BinaryOption(1);

    option.update(new byte[]{(byte) 0xff, (byte) 0xfe});

    assertEquals(65534L, option.getValue());
  }
}
