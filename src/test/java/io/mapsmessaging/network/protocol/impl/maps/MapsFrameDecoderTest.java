/* Copyright [ 2020 - 2026 ] MapsMessaging B.V. */
package io.mapsmessaging.network.protocol.impl.maps;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.network.io.Packet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MapsFrameDecoderTest {

  @Test
  void decodesFrameAcrossArbitraryReadBoundaries() throws Exception {
    byte[] payload = new byte[8193];
    for (int i = 0; i < payload.length; i++) payload[i] = (byte) i;
    MapsFrame outbound = new MapsFrame(MapsPacketType.CONNECT, MapsFrame.FLAG_ACK_REQUIRED, 42, ByteBuffer.wrap(payload));
    Packet packet = new Packet(MapsFrame.HEADER_SIZE + payload.length, false);
    outbound.packFrame(packet);
    packet.flip();

    ByteBuffer wire = packet.getRawBuffer();
    MapsFrameDecoder decoder = new MapsFrameDecoder(1024 * 1024);
    List<MapsFrame> decoded = new ArrayList<>();
    while (wire.hasRemaining()) {
      int length = Math.min(37, wire.remaining());
      ByteBuffer fragment = wire.slice();
      fragment.limit(length);
      decoded.addAll(decoder.decode(fragment));
      wire.position(wire.position() + length);
    }

    assertEquals(1, decoded.size());
    MapsFrame frame = decoded.getFirst();
    assertEquals(MapsPacketType.CONNECT, frame.type());
    assertEquals(42, frame.requestId());
    byte[] received = new byte[frame.body().remaining()];
    frame.body().get(received);
    assertArrayEquals(payload, received);
  }
}
