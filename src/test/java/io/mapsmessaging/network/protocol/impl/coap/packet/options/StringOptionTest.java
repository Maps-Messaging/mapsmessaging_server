package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class StringOptionTest {

  @Test
  void updateAndPackRoundTripStringPayload() throws Exception {
    StringOption option = new StringOption(11);

    option.update("sensor/temperature".getBytes(StandardCharsets.UTF_8));

    assertEquals(11, option.getId());
    assertEquals("sensor/temperature", option.getValue());
    assertArrayEquals(
        "sensor/temperature".getBytes(StandardCharsets.UTF_8),
        option.pack()
    );
  }

  @Test
  void emptyPayloadBecomesEmptyStringAndEmptyPackedBytes() throws Exception {
    StringOption option = new StringOption(1);

    option.update(new byte[0]);

    assertEquals("", option.getValue());
    assertArrayEquals(new byte[0], option.pack());
  }

  @Test
  void packBeforeValueIsSetFailsRatherThanFabricatingData() {
    StringOption option = new StringOption(1);

    assertThrows(NullPointerException.class, option::pack);
  }
}
