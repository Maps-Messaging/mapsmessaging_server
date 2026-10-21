package io.mapsmessaging.network.protocol.impl.coap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.network.protocol.impl.coap.blockwise.BlockReceiveMonitor;
import io.mapsmessaging.network.protocol.impl.coap.blockwise.BlockReceiveState;
import io.mapsmessaging.network.protocol.impl.coap.blockwise.SendController;
import io.mapsmessaging.network.protocol.impl.coap.listeners.EmptyListener;
import io.mapsmessaging.network.protocol.impl.coap.listeners.GetListener;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Block;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Observe;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.UriPath;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.SubscriptionState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag("conformance")
@Tag("conformance-core")
@Tag("coap")
class CoapLifecycleConformanceTest {

  private static final String RFC7252 = "CoAP RFC 7252";
  private static final String RFC7641 = "CoAP Observe RFC 7641";
  private static final String RFC7959 = "CoAP Blockwise RFC 7959";

  @Test
  @ProtocolRequirement(
      specification = RFC7252,
      value = "RFC 7252 Section 4.2: a Confirmable Empty message is a ping and is rejected with Reset using the same Message ID",
      source = ProtocolRequirement.COAP_RFC7252_SOURCE)
  void confirmableEmptyPingProducesResetWithSameMessageId() throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    BasePacket ping = new BasePacket(PacketFactory.EMPTY, TYPE.CON, Code.EMPTY, 1, 0x4242, new byte[0]);

    new EmptyListener().handle(ping, protocol);

    ArgumentCaptor<BasePacket> response = ArgumentCaptor.forClass(BasePacket.class);
    verify(protocol).sendResponse(response.capture());
    assertEquals(TYPE.RST, response.getValue().getType());
    assertEquals(Code.EMPTY, response.getValue().getCode());
    assertEquals(0x4242, response.getValue().getMessageId());
  }

  @Test
  @ProtocolRequirement(
      specification = RFC7252,
      value = "RFC 7252 Section 4.2 and 4.4: late or unmatched acknowledgements do not create a new exchange or crash the endpoint",
      source = ProtocolRequirement.COAP_RFC7252_SOURCE)
  void unknownAcknowledgementIsIgnoredSafely() throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    PacketPipeline pipeline = new PacketPipeline(protocol);
    try {
      BasePacket ack = new BasePacket(0, TYPE.ACK, Code.EMPTY, 1, 0x1234, new byte[0]);
      assertDoesNotThrow(() -> pipeline.ack(ack));
      verify(protocol, never()).send(any());
    } finally {
      pipeline.close();
    }
  }

  @Test
  @ProtocolRequirement(
      specification = RFC7252,
      value = "RFC 7252 Section 4.5: duplicate request detection state is retained for EXCHANGE_LIFETIME, not merely the most recent MID",
      source = ProtocolRequirement.COAP_RFC7252_SOURCE)
  void duplicateCacheRetainsMultipleRecentMessageIds() {
    DuplicationManager manager = new DuplicationManager();
    BasePacket first = packet(100);
    BasePacket second = packet(101);
    manager.put(first);
    manager.put(second);
    assertSame(first, manager.getResponse(100));
    assertSame(second, manager.getResponse(101));
  }

  @Test
  @ProtocolRequirement(
      specification = RFC7641,
      value = "RFC 7641 Section 3.6: GET with Observe=1 explicitly deregisters an observation while keeping the CoAP endpoint alive",
      source = ProtocolRequirement.COAP_RFC7641_SOURCE)
  void observeOneRemovesOnlyNamedObservation() throws Exception {
    String path = "/observe/test";
    Session session = mock(Session.class);
    CoapProtocol protocol = mock(CoapProtocol.class);
    SubscriptionState state = new SubscriptionState();
    when(protocol.getSession()).thenReturn(session);
    when(protocol.getSubscriptionState()).thenReturn(state);

    BasePacket original = getRequest(path, 1, 0);
    state.create(path, original);
    BasePacket cancel = getRequest(path, 2, 1);

    BasePacket response = new GetListener().handle(cancel, protocol);

    assertNotNull(response);
    assertEquals(Code.CONTENT, response.getCode());
    assertFalse(state.exists(path));
    verify(session).removeSubscription(path);
    verify(protocol, never()).close();
  }

  @Test
  @ProtocolRequirement(
      specification = RFC7959,
      value = "RFC 7959 Section 2.2: block-wise transfer preserves representation bytes across a peer-requested reduction in block size",
      source = ProtocolRequirement.COAP_RFC7959_SOURCE)
  void blockSizeReductionPreservesPayloadAndCorrectOffsets() {
    byte[] original = sequence(64);
    SendController controller = new SendController(original, 16);
    List<byte[]> delivered = new ArrayList<>();

    delivered.add(controller.get());
    assertEquals(0, controller.getBlockNumber());
    controller.ack(0);

    controller.resize(8);
    assertEquals(2, controller.getBlockNumber());

    while (!controller.isComplete()) {
      int block = controller.getBlockNumber();
      delivered.add(controller.get());
      controller.ack(block);
    }

    byte[] rebuilt = concatenate(delivered);
    assertArrayEquals(original, rebuilt);
  }

  @Test
  @ProtocolRequirement(
      specification = RFC7959,
      value = "RFC 7959 Section 2: in-progress block-wise state must remain available between successive exchanges",
      source = ProtocolRequirement.COAP_RFC7959_SOURCE)
  void activeBlockReceiveStateIsNotExpiredImmediately() {
    BlockReceiveMonitor monitor = new BlockReceiveMonitor();
    Block block = new Block(23, 0, true, 4);
    BlockReceiveState before = monitor.registerOrGet(block, "/upload");
    monitor.scanForIdle();
    assertSame(before, monitor.registerOrGet(block, "/upload"));
  }

  private static BasePacket packet(int messageId) {
    return new BasePacket(0, TYPE.ACK, Code.CONTENT, 1, messageId, new byte[0]);
  }

  private static BasePacket getRequest(String path, int messageId, int observeValue) {
    BasePacket packet = new BasePacket(PacketFactory.GET, TYPE.CON, Code.EMPTY, 1, messageId, new byte[]{1,2});
    UriPath uriPath = new UriPath();
    uriPath.setPath(path);
    packet.getOptions().putOption(uriPath);
    packet.getOptions().putOption(new Observe(observeValue));
    return packet;
  }

  private static byte[] sequence(int size) {
    byte[] bytes = new byte[size];
    for (int i=0;i<size;i++) bytes[i]=(byte)i;
    return bytes;
  }

  private static byte[] concatenate(List<byte[]> parts) {
    int size = parts.stream().mapToInt(p -> p.length).sum();
    byte[] result = new byte[size];
    int offset=0;
    for(byte[] part:parts){
      System.arraycopy(part,0,result,offset,part.length);
      offset += part.length;
    }
    return result;
  }
}
