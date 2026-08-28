/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.network.protocol.impl.maps;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.MessageFactory;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.shm.SharedMemoryTransport;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MapsSharedMemoryLoopbackTest {

  private static final int SLOT_SIZE = 4096;
  private static final int SLOT_COUNT = 64;

  @Test
  void connectSubscribeAndPublishAcrossSharedMemoryPair() throws Exception {
    String region = "maps-protocol-loopback-" + UUID.randomUUID();

    try (
        SharedMemoryTransport sideA = new SharedMemoryTransport(region, true, SLOT_SIZE, SLOT_COUNT);
        SharedMemoryTransport sideB = new SharedMemoryTransport(region, false, SLOT_SIZE, SLOT_COUNT)) {

      MapsFrameDecoder decoderA = new MapsFrameDecoder(2 * 1024 * 1024);
      MapsFrameDecoder decoderB = new MapsFrameDecoder(2 * 1024 * 1024);

      MapsFrame connect = createConnectFrame(1, "maps-loopback", "user1", "password1");
      write(sideA, connect);

      MapsFrame receivedConnect = readSingle(sideB, decoderB);
      assertEquals(MapsPacketType.CONNECT, receivedConnect.type());
      assertEquals(1, receivedConnect.requestId());
      verifyConnectBody(receivedConnect.body());

      MapsFrame connAck = createConnAckFrame(receivedConnect.requestId());
      write(sideB, connAck);

      MapsFrame receivedConnAck = readSingle(sideA, decoderA);
      assertEquals(MapsPacketType.CONNACK, receivedConnAck.type());
      assertEquals(1, receivedConnAck.requestId());

      MapsFrame subscribe = createSubscribeFrame(2, "/test/maps/#", "temperature > 20");
      write(sideA, subscribe);

      MapsFrame receivedSubscribe = readSingle(sideB, decoderB);
      assertEquals(MapsPacketType.SUBSCRIBE, receivedSubscribe.type());
      verifySubscribeBody(receivedSubscribe.body());

      Message message = new MessageBuilder()
          .setQoS(QualityOfService.AT_LEAST_ONCE)
          .setOpaqueData("native-maps-message".getBytes(StandardCharsets.UTF_8))
          .build();

      MapsPublishFrame publish = new MapsPublishFrame("/test/maps/value", message, 3, true);
      write(sideA, publish);

      MapsFrame receivedPublish = readSingle(sideB, decoderB);
      assertEquals(MapsPacketType.PUBLISH, receivedPublish.type());
      assertEquals(3, receivedPublish.requestId());
      assertTrue(receivedPublish.ackRequired());

      verifyNativePublish(receivedPublish.body(), message);

      MapsFrame pubAck = createAckFrame(MapsPacketType.PUBACK, 3);
      write(sideB, pubAck);

      MapsFrame receivedPubAck = readSingle(sideA, decoderA);
      assertEquals(MapsPacketType.PUBACK, receivedPubAck.type());
      assertEquals(3, receivedPubAck.requestId());
      assertFalse(receivedPubAck.ackRequired());
    }
  }

  private static MapsFrame createConnectFrame(int requestId, String sessionId, String username, String password) {
    int size = 4 + Long.BYTES + Integer.BYTES
        + MapsCodec.stringSize(sessionId)
        + 1
        + MapsCodec.stringSize(username)
        + MapsCodec.stringSize(password);
    ByteBuffer body = ByteBuffer.allocate(size);
    body.put((byte) 1).put((byte) 0).put((byte) 1).put((byte) 0);
    body.putLong(MapsCapabilities.VERSION_1);
    body.putInt(30);
    MapsCodec.putString(body, sessionId);
    body.put((byte) 1);
    MapsCodec.putString(body, username);
    MapsCodec.putString(body, password);
    body.flip();
    return new MapsFrame(MapsPacketType.CONNECT, 0, requestId, body);
  }

  private static MapsFrame createConnAckFrame(int requestId) {
    ByteBuffer body = ByteBuffer.allocate(3 + Long.BYTES + MapsCodec.stringSize("server") + MapsCodec.stringSize("connected"));
    body.put((byte) MapsProtocol.STATUS_OK).put((byte) 1).put((byte) 0);
    body.putLong(MapsCapabilities.VERSION_1);
    MapsCodec.putString(body, "server");
    MapsCodec.putString(body, "connected");
    body.flip();
    return new MapsFrame(MapsPacketType.CONNACK, 0, requestId, body);
  }

  private static MapsFrame createSubscribeFrame(int requestId, String topic, String selector) {
    ByteBuffer body = ByteBuffer.allocate(MapsCodec.stringSize(topic) + MapsCodec.stringSize(selector) + 1 + Integer.BYTES);
    MapsCodec.putString(body, topic);
    MapsCodec.putString(body, selector);
    body.put((byte) QualityOfService.AT_LEAST_ONCE.getLevel());
    body.putInt(1024);
    body.flip();
    return new MapsFrame(MapsPacketType.SUBSCRIBE, 0, requestId, body);
  }

  private static MapsFrame createAckFrame(MapsPacketType type, int requestId) {
    ByteBuffer body = ByteBuffer.allocate(1 + MapsCodec.stringSize(null));
    body.put((byte) MapsProtocol.STATUS_OK);
    MapsCodec.putString(body, null);
    body.flip();
    return new MapsFrame(type, 0, requestId, body);
  }

  private static void verifyConnectBody(ByteBuffer body) throws Exception {
    assertEquals(1, Byte.toUnsignedInt(body.get()));
    assertEquals(0, Byte.toUnsignedInt(body.get()));
    assertEquals(1, Byte.toUnsignedInt(body.get()));
    assertEquals(0, Byte.toUnsignedInt(body.get()));
    assertEquals(MapsCapabilities.VERSION_1, body.getLong());
    assertEquals(30, body.getInt());
    assertEquals("maps-loopback", MapsCodec.getString(body));
    assertEquals(1, Byte.toUnsignedInt(body.get()));
    assertEquals("user1", MapsCodec.getString(body));
    assertEquals("password1", MapsCodec.getString(body));
  }

  private static void verifySubscribeBody(ByteBuffer body) throws Exception {
    assertEquals("/test/maps/#", MapsCodec.getString(body));
    assertEquals("temperature > 20", MapsCodec.getString(body));
    assertEquals(QualityOfService.AT_LEAST_ONCE.getLevel(), Byte.toUnsignedInt(body.get()));
    assertEquals(1024, body.getInt());
  }

  private static void verifyNativePublish(ByteBuffer body, Message expected) throws Exception {
    String topic = MapsCodec.getString(body);
    assertEquals("/test/maps/value", topic);

    int count = body.getInt();
    assertTrue(count >= 2);
    int[] lengths = new int[count];
    for (int i = 0; i < count; i++) {
      lengths[i] = body.getInt();
    }

    ByteBuffer[] buffers = new ByteBuffer[count];
    for (int i = 0; i < count; i++) {
      ByteBuffer slice = body.slice();
      slice.limit(lengths[i]);
      buffers[i] = slice;
      body.position(body.position() + lengths[i]);
    }

    Message decoded = MessageFactory.getInstance().unpack(buffers);
    assertNotNull(decoded);
    assertEquals(expected.getQualityOfService(), decoded.getQualityOfService());
    assertArrayEquals(expected.getOpaqueData(), decoded.getOpaqueData());
  }

  private static void write(SharedMemoryTransport transport, MapsFrame frame) throws Exception {
    Packet packet = new Packet(256 * 1024, false);
    frame.packFrame(packet);
    packet.flip();
    writeFully(transport, packet.getRawBuffer());
  }

  private static void write(SharedMemoryTransport transport, MapsPublishFrame frame) throws Exception {
    Packet packet = new Packet(256 * 1024, false);
    frame.packFrame(packet);
    packet.flip();
    writeFully(transport, packet.getRawBuffer());
  }

  private static void writeFully(SharedMemoryTransport transport, ByteBuffer source) throws Exception {
    long deadline = System.nanoTime() + 2_000_000_000L;
    while (source.hasRemaining()) {
      int written = transport.write(source);
      if (written == 0) {
        if (System.nanoTime() > deadline) {
          throw new AssertionError("Timed out writing MAPS loopback frame");
        }
        Thread.onSpinWait();
      }
    }
  }

  private static MapsFrame readSingle(SharedMemoryTransport transport, MapsFrameDecoder decoder) throws Exception {
    ByteBuffer buffer = ByteBuffer.allocate(256 * 1024);
    long deadline = System.nanoTime() + 2_000_000_000L;
    while (System.nanoTime() < deadline) {
      int read = transport.read(buffer);
      if (read > 0) {
        buffer.flip();
        List<MapsFrame> frames = decoder.decode(buffer);
        if (!frames.isEmpty()) {
          assertEquals(1, frames.size());
          return frames.getFirst();
        }
        buffer.compact();
      } else {
        Thread.onSpinWait();
      }
    }
    throw new AssertionError("Timed out waiting for MAPS loopback frame");
  }
}
