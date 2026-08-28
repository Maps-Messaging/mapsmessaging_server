/* Copyright [ 2020 - 2026 ] MapsMessaging B.V. */
package io.mapsmessaging.network.protocol.impl.maps;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.MessageFactory;
import io.mapsmessaging.network.io.Packet;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class MapsNativeMessageTest {

  @Test
  void publishCarriesNativeMessageFactoryRepresentation() throws Exception {
    byte[] payload = new byte[128 * 1024];
    for (int i = 0; i < payload.length; i++) payload[i] = (byte) (i * 31);
    Message message = new MessageBuilder().setOpaqueData(payload).setQoS(QualityOfService.AT_LEAST_ONCE).build();

    MapsPublishFrame publish = new MapsPublishFrame("telemetry/device/1", message, 77, true);
    Packet headerPacket = new Packet(4096, false);
    Packet[] parts = publish.packAdvancedFrame(headerPacket);
    parts[0].flip();

    int total = 0;
    for (Packet part : parts) total += part.getRawBuffer().remaining();
    ByteBuffer wire = ByteBuffer.allocate(total);
    for (Packet part : parts) wire.put(part.getRawBuffer().duplicate());
    wire.flip();

    List<MapsFrame> frames = new MapsFrameDecoder(1024 * 1024).decode(wire);
    assertEquals(1, frames.size());
    MapsFrame frame = frames.getFirst();
    assertEquals(MapsPacketType.PUBLISH, frame.type());
    assertEquals(77, frame.requestId());

    ByteBuffer body = frame.body();
    assertEquals("telemetry/device/1", MapsCodec.getString(body));
    int count = body.getInt();
    int[] lengths = new int[count];
    for (int i = 0; i < count; i++) lengths[i] = body.getInt();
    ByteBuffer[] packed = new ByteBuffer[count];
    for (int i = 0; i < count; i++) {
      ByteBuffer slice = body.slice();
      slice.limit(lengths[i]);
      packed[i] = slice;
      body.position(body.position() + lengths[i]);
    }

    Message restored = MessageFactory.getInstance().unpack(packed);
    assertEquals(QualityOfService.AT_LEAST_ONCE, restored.getQualityOfService());
    assertArrayEquals(payload, restored.getOpaqueData());
  }
}
