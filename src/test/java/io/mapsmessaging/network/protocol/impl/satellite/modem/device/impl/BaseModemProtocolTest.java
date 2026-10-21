package io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl;

import io.mapsmessaging.network.protocol.impl.satellite.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.data.NetworkStatus;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.IncomingMessageDetails;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.ModemSatelliteMessage;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.SendMessageState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BaseModemProtocolTest {

  @Test
  void sendingBytesTracksPacketAndByteCounts() {
    TestProtocol protocol = new TestProtocol(false);

    protocol.recordSent(25);
    protocol.recordSent(10);

    assertEquals(35, protocol.getSentBytes().get());
    assertEquals(2, protocol.getSentPackets().get());
  }

  @Test
  void outgoingListIgnoresCommandMarkersAndTerminalStatusLines() {
    TestProtocol protocol = new TestProtocol(false);

    List<SendMessageState> states = protocol.outgoing(
        "OK\n%MGRL:\n%MGRS: \"MSG1\",12.5,2,44,3,100,25\nERROR\n"
    );

    assertEquals(1, states.size());
    assertEquals("MSG1", states.getFirst().getMessageName());
    assertEquals(SendMessageState.State.TX_READY, states.getFirst().getState());
  }

  @Test
  void incomingListStripsLegacyPrefixBeforeParsing() {
    TestProtocol protocol = new TestProtocol(false);

    List<IncomingMessageDetails> details = protocol.incoming(
        "OK\r\n%MGFN: msg-9,12.5,3,44,3,100,80\r\nERROR\r\n"
    );

    assertEquals(1, details.size());
    assertEquals("msg-9", details.getFirst().getId());
    assertEquals(44, details.getFirst().getSin());
  }

  @Test
  void incomingMessageParsingUpdatesReceiveCounters() {
    TestProtocol protocol = new TestProtocol(false);

    ModemSatelliteMessage message =
        protocol.message("msg,x,x,10,2,x,2,14414243");

    assertNotNull(message);
    assertEquals(10, message.getSin());
    assertEquals(20, message.getMin());
    assertArrayEquals(new byte[]{0x41, 0x42, 0x43}, message.getPayload());
    assertEquals(3, protocol.getReceivedBytes().get());
    assertEquals(1, protocol.getReceivedPackets().get());
  }

  private static final class TestProtocol extends BaseModemProtocol {
    TestProtocol(boolean ogx) {
      super(mock(Modem.class), ogx);
    }

    void recordSent(int count) {
      sendingBytes(count);
    }

    List<SendMessageState> outgoing(String response) {
      return parseOutgoingMessageList(response);
    }

    List<IncomingMessageDetails> incoming(String response) {
      return parseIncomingListResponse(response);
    }

    ModemSatelliteMessage message(String response) {
      return parseIncomingMessageResponse(response);
    }

    @Override
    public NetworkStatus getCurrentNetworkStatus() {
      return NetworkStatus.parse("5");
    }

    @Override
    public CompletableFuture<List<SendMessageState>> listSentMessages() {
      return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public void sendMessage(ModemSatelliteMessage modemSatelliteMessage) {}

    @Override
    public CompletableFuture<Boolean> deleteSentMessages(String msgName) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<List<IncomingMessageDetails>> listIncomingMessages() {
      return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public CompletableFuture<ModemSatelliteMessage> getMessage(IncomingMessageDetails details) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> deleteIncomingMessage(String name) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    public String getType() {
      return "test";
    }
  }
}
