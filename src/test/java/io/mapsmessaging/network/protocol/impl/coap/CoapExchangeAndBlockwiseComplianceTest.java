package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.network.protocol.impl.coap.blockwise.BlockReceiveMonitor;
import io.mapsmessaging.network.protocol.impl.coap.blockwise.BlockReceiveState;
import io.mapsmessaging.network.protocol.impl.coap.blockwise.SendPacket;
import io.mapsmessaging.network.protocol.impl.coap.listeners.EmptyListener;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Block;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CoapExchangeAndBlockwiseComplianceTest {

  @Test
  void ackForUnknownMessageIdIsSafelyIgnored() {
    CoapProtocol protocol = mock(CoapProtocol.class);
    PacketPipeline pipeline = new PacketPipeline(protocol);
    BasePacket ack = packet(TYPE.ACK, Code.EMPTY, 0x1234);

    try {
      assertDoesNotThrow(
          () -> pipeline.ack(ack),
          "RFC 7252 permits duplicate/late ACKs; an unknown ACK must not crash the endpoint");
    } finally {
      pipeline.close();
    }
  }

  @Test
  void secondConfirmableMessageWaitsForFirstAcknowledgement() throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    PacketPipeline pipeline = new PacketPipeline(protocol);
    BasePacket first = packet(TYPE.CON, Code.CONTENT, 1);
    BasePacket second = packet(TYPE.CON, Code.CONTENT, 2);

    try {
      pipeline.send(first);
      pipeline.send(second);

      verify(protocol).send(first);
      verify(protocol, never()).send(second);

      pipeline.ack(packet(TYPE.ACK, Code.EMPTY, 1));

      verify(protocol).send(second);
    } finally {
      pipeline.close();
    }
  }

  @Test
  void nonConfirmableMessageAlsoWaitsBehindOutstandingConfirmableExchange() throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    PacketPipeline pipeline = new PacketPipeline(protocol);
    BasePacket first = packet(TYPE.CON, Code.CONTENT, 1);
    BasePacket second = packet(TYPE.NON, Code.CONTENT, 2);

    try {
      pipeline.send(first);
      pipeline.send(second);

      verify(protocol).send(first);
      verify(protocol, never()).send(second);

      pipeline.ack(packet(TYPE.ACK, Code.EMPTY, 1));

      verify(protocol).send(second);
    } finally {
      pipeline.close();
    }
  }

  @Test
  void acknowledgementPacketIsSentImmediatelyEvenWithOutstandingConfirmableExchange()
      throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    PacketPipeline pipeline = new PacketPipeline(protocol);
    BasePacket first = packet(TYPE.CON, Code.CONTENT, 1);
    BasePacket ack = packet(TYPE.ACK, Code.EMPTY, 99);

    try {
      pipeline.send(first);
      pipeline.send(ack);

      verify(protocol).send(first);
      verify(protocol).send(ack);
    } finally {
      pipeline.close();
    }
  }

  @Test
  void resetRejectsExchangeWithoutClosingEntireCoapPeer() throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    EmptyListener listener = new EmptyListener();
    BasePacket reset = packet(TYPE.RST, Code.EMPTY, 22);

    listener.handle(reset, protocol);

    verify(protocol, never()).close();
  }

  @Test
  void emptyConfirmablePingProducesResetWithSameMessageId() throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    EmptyListener listener = new EmptyListener();
    BasePacket ping = packet(TYPE.CON, Code.EMPTY, 0x4242);

    listener.handle(ping, protocol);

    ArgumentCaptor<BasePacket> response = ArgumentCaptor.forClass(BasePacket.class);
    verify(protocol).sendResponse(response.capture());
    assertEquals(TYPE.RST, response.getValue().getType());
    assertEquals(Code.EMPTY, response.getValue().getCode());
    assertEquals(0x4242, response.getValue().getMessageId());
  }

  @Test
  void freshBlockReceiveStateSurvivesIdleScan() {
    BlockReceiveMonitor monitor = new BlockReceiveMonitor();
    Block block = new Block(23, 0, true, 4);

    BlockReceiveState first = monitor.registerOrGet(block, "/upload");
    monitor.scanForIdle();
    BlockReceiveState second = monitor.registerOrGet(block, "/upload");

    assertSame(
        first,
        second,
        "A blockwise transfer accessed now must not expire until it has actually been idle for 30 seconds");
  }

  @Test
  void genuinelyIdleBlockReceiveStateExpires() {
    BlockReceiveMonitor monitor = new BlockReceiveMonitor();
    Block block = new Block(23, 0, true, 4);

    BlockReceiveState first = monitor.registerOrGet(block, "/upload");
    first.setLastAccess(System.currentTimeMillis() - 31_000L);
    monitor.scanForIdle();
    BlockReceiveState second = monitor.registerOrGet(block, "/upload");

    assertNotSame(first, second);
  }

  @Test
  void completingBlockTransferRemovesReceiveState() {
    BlockReceiveMonitor monitor = new BlockReceiveMonitor();
    Block block = new Block(23, 0, true, 4);

    BlockReceiveState first = monitor.registerOrGet(block, "/upload");
    monitor.complete("/upload");
    BlockReceiveState second = monitor.registerOrGet(block, "/upload");

    assertNotSame(first, second);
  }

  @ParameterizedTest
  @MethodSource("sendPacketShapes")
  void blockPacketSplitsPayloadWithoutLossOrDuplication(int payloadSize, int blockSize) {
    byte[] payload = sequence(payloadSize);
    SendPacket packet = new SendPacket(payload, blockSize);

    assertArrayEquals(payload, concatenate(packet));
    assertEquals((payloadSize + blockSize - 1) / blockSize, packet.getSize());

    for (int i = 0; i < packet.getSize() - 1; i++) {
      assertEquals(blockSize, packet.getBlock(i).length);
    }
    if (packet.getSize() > 0) {
      assertTrue(packet.getBlock(packet.getSize() - 1).length <= blockSize);
    }
  }

  @ParameterizedTest
  @MethodSource("resizeCases")
  void blockSizeRenegotiationPreservesAlreadySentPrefixAndRemainingPayload(
      int payloadSize, int initialSize, int newSize, int sentBlocks) {
    byte[] payload = sequence(payloadSize);
    SendPacket packet = new SendPacket(payload, initialSize);

    packet.resize(newSize, sentBlocks);

    byte[] reconstructed = concatenate(packet);
    assertArrayEquals(
        payload,
        reconstructed,
        "RFC 7959 block-size negotiation must neither duplicate nor lose representation bytes");
  }

  @ParameterizedTest
  @MethodSource("validBlockSizes")
  void sendPacketSupportsAllUdpBlockSizes(int blockSize) {
    byte[] payload = sequence(blockSize * 3 + 7);
    SendPacket packet = new SendPacket(payload, blockSize);

    assertArrayEquals(payload, concatenate(packet));
    assertEquals(4, packet.getSize());
  }

  @Test
  void requestingBlockBeyondRepresentationFailsClearly() {
    SendPacket packet = new SendPacket(new byte[16], 8);

    assertThrows(IndexOutOfBoundsException.class, () -> packet.getBlock(2));
    assertThrows(IndexOutOfBoundsException.class, () -> packet.getBlock(100));
  }

  private static BasePacket packet(TYPE type, Code code, int messageId) {
    return new BasePacket(0, type, code, 1, messageId, new byte[0]);
  }

  private static byte[] concatenate(SendPacket packet) {
    List<Byte> bytes = new ArrayList<>();
    for (int i = 0; i < packet.getSize(); i++) {
      for (byte value : packet.getBlock(i)) {
        bytes.add(value);
      }
    }
    byte[] result = new byte[bytes.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = bytes.get(i);
    }
    return result;
  }

  private static byte[] sequence(int size) {
    byte[] result = new byte[size];
    for (int i = 0; i < size; i++) {
      result[i] = (byte) (i & 0xff);
    }
    return result;
  }

  private static Stream<Arguments> sendPacketShapes() {
    return Stream.of(
        Arguments.of(1, 16),
        Arguments.of(15, 16),
        Arguments.of(16, 16),
        Arguments.of(17, 16),
        Arguments.of(31, 16),
        Arguments.of(32, 16),
        Arguments.of(33, 16),
        Arguments.of(63, 32),
        Arguments.of(64, 32),
        Arguments.of(65, 32),
        Arguments.of(255, 64),
        Arguments.of(256, 64),
        Arguments.of(257, 64),
        Arguments.of(1024, 128));
  }

  private static Stream<Arguments> resizeCases() {
    return Stream.of(
        Arguments.of(40, 16, 8, 1),
        Arguments.of(64, 16, 8, 1),
        Arguments.of(64, 16, 8, 2),
        Arguments.of(80, 32, 16, 1),
        Arguments.of(128, 32, 16, 2),
        Arguments.of(256, 64, 32, 1),
        Arguments.of(256, 64, 16, 2),
        Arguments.of(1024, 128, 64, 3));
  }

  private static Stream<Arguments> validBlockSizes() {
    return IntStream.of(16, 32, 64, 128, 256, 512, 1024)
        .mapToObj(Arguments::of);
  }
}
