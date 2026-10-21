package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SatelliteMessageTest {

  @Test
  void structuredMessageRoundTripsHeaderFlagsAndPayload() {
    SatelliteMessage source =
        new SatelliteMessage(200, new byte[]{1, 2, 3}, 17, 23, true, (byte) 7);

    SatelliteMessage restored = new SatelliteMessage(source.packToSend());

    assertFalse(restored.isRaw());
    assertTrue(restored.isCompressed());
    assertEquals(200, restored.getStreamNumber());
    assertEquals(17, restored.getPacketNumber());
    assertEquals(23, restored.getTotalPackets());
    assertEquals(7, restored.getTransformationId());
    assertArrayEquals(new byte[]{1, 2, 3}, restored.getMessage());
  }

  @Test
  void shortInputIsPreservedAsRawPayload() {
    byte[] raw = new byte[]{9, 8, 7};

    SatelliteMessage message = new SatelliteMessage(raw);

    assertTrue(message.isRaw());
    assertArrayEquals(raw, message.getMessage());
  }

  @Test
  void declaredPayloadLongerThanAvailableFallsBackToRawData() {
    SatelliteMessage source =
        new SatelliteMessage(1, new byte[]{4, 5}, 1, 1, false, (byte) 0);
    byte[] packed = source.packToSend();
    packed[6] = 0;
    packed[7] = 20;

    SatelliteMessage restored = new SatelliteMessage(packed);

    assertTrue(restored.isRaw());
    assertArrayEquals(packed, restored.getMessage());
  }

  @Test
  void nullInputLeavesEmptyMessageObjectRatherThanThrowing() {
    SatelliteMessage message = new SatelliteMessage((byte[]) null);

    assertDoesNotThrow(() -> message.setCompletionCallback(() -> {}));
    assertNull(message.getMessage());
  }
}
