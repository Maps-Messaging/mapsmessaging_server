package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SatelliteMessageFactoryTest {

  @Test
  void createMessagesSplitsPayloadAndPreservesFragmentMetadata() {
    byte[] payload = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    List<SatelliteMessage> messages =
        SatelliteMessageFactory.createMessages(payload, 4, true, (byte) 7);

    assertEquals(3, messages.size());
    int stream = messages.getFirst().getStreamNumber();
    assertArrayEquals(new byte[]{0, 1, 2, 3}, messages.get(0).getMessage());
    assertArrayEquals(new byte[]{4, 5, 6, 7}, messages.get(1).getMessage());
    assertArrayEquals(new byte[]{8, 9}, messages.get(2).getMessage());

    for (int i = 0; i < messages.size(); i++) {
      SatelliteMessage message = messages.get(i);
      assertEquals(stream, message.getStreamNumber());
      assertEquals(i, message.getPacketNumber());
      assertEquals(3, message.getTotalPackets());
      assertTrue(message.isCompressed());
      assertEquals((byte) 7, message.getTransformationId());
    }
  }

  @Test
  void reconstructSingleUncompressedMessageReturnsSameInstance() {
    SatelliteMessage message =
        SatelliteMessageFactory.createMessages(new byte[]{1, 2, 3}, 32, false, (byte) 0).getFirst();

    assertSame(message, SatelliteMessageFactory.reconstructMessage(List.of(message)));
  }

  @Test
  void reconstructCombinesFragmentsInProvidedOrderAndPreservesFlags() {
    List<SatelliteMessage> messages =
        SatelliteMessageFactory.createMessages(new byte[]{1, 2, 3, 4, 5}, 2, true, (byte) 3);

    SatelliteMessage rebuilt = SatelliteMessageFactory.reconstructMessage(messages);

    assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, rebuilt.getMessage());
    assertTrue(rebuilt.isCompressed());
    assertEquals((byte) 3, rebuilt.getTransformationId());
    assertEquals(messages.getFirst().getStreamNumber(), rebuilt.getStreamNumber());
  }
}