package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BypassSatelliteMessageTest {

  @Test
  void bypassPackingReturnsPayloadWithoutSatelliteHeader() {
    BypassSatelliteMessage message =
        new BypassSatelliteMessage(3, new byte[]{1, 2, 3, 4}, 7, true);

    assertArrayEquals(new byte[]{1, 2, 3, 4}, message.packToSend());
    assertEquals(1, message.getTotalPackets());
  }

  @Test
  void receivedBypassPayloadIsAcceptedVerbatim() {
    TestBypass message = new TestBypass();
    byte[] payload = new byte[]{10, 20};

    message.unpack(payload);

    assertArrayEquals(payload, message.getMessage());
  }

  @Test
  void nullReceivedPayloadDoesNotDestroyExistingPayload() {
    TestBypass message = new TestBypass();
    message.unpack(new byte[]{5});
    message.unpack(null);

    assertArrayEquals(new byte[]{5}, message.getMessage());
  }

  private static final class TestBypass extends BypassSatelliteMessage {
    TestBypass() {
      super(0, new byte[0], 0, false);
    }

    void unpack(byte[] payload) {
      unpackFromReceived(payload);
    }
  }
}
