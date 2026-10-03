package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocol;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import io.mapsmessaging.network.protocol.impl.stomp.frames.*;
import io.mapsmessaging.network.protocol.impl.stomp.state.SessionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StompServerListenerCoverageTest {

  @ParameterizedTest
  @MethodSource("acknowledgementIds")
  void stomp12AckResolvesOpaqueAcknowledgementId(String subscriptionId, long messageId)
      throws Exception {
    SessionState engine = engine(true);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    when(engine.findSubscription(subscriptionId)).thenReturn(subscription);

    Ack frame = (Ack) parse("ACK\nid:" + escape(subscriptionId + ":" + messageId) + "\n\n\0");

    new AckListener().frameEvent(frame, engine, true);

    verify(subscription).ackReceived(messageId);
  }

  @ParameterizedTest
  @MethodSource("acknowledgementIds")
  void stomp12NackResolvesOpaqueAcknowledgementId(String subscriptionId, long messageId)
      throws Exception {
    SessionState engine = engine(true);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    when(engine.findSubscription(subscriptionId)).thenReturn(subscription);

    Nack frame = (Nack) parse("NACK\nid:" + escape(subscriptionId + ":" + messageId) + "\n\n\0");

    new NackListener().frameEvent(frame, engine, true);

    verify(subscription).rollbackReceived(messageId);
  }

  @ParameterizedTest
  @MethodSource("legacyAcknowledgements")
  void stomp11AckUsesSubscriptionAndMessageId(String subscriptionId, long messageId)
      throws Exception {
    SessionState engine = engine(false);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    when(engine.findSubscription(subscriptionId)).thenReturn(subscription);

    Ack frame = (Ack) parse(
        "ACK\nsubscription:" + subscriptionId + "\nmessage-id:" + messageId + "\n\n\0");

    new AckListener().frameEvent(frame, engine, true);

    verify(subscription).ackReceived(messageId);
  }

  @ParameterizedTest
  @MethodSource("legacyAcknowledgements")
  void stomp11NackUsesSubscriptionAndMessageId(String subscriptionId, long messageId)
      throws Exception {
    SessionState engine = engine(false);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    when(engine.findSubscription(subscriptionId)).thenReturn(subscription);

    Nack frame = (Nack) parse(
        "NACK\nsubscription:" + subscriptionId + "\nmessage-id:" + messageId + "\n\n\0");

    new NackListener().frameEvent(frame, engine, true);

    verify(subscription).rollbackReceived(messageId);
  }

  @Test
  void ackRejectsUnknownSubscription() throws Exception {
    SessionState engine = engine(true);
    Ack frame = (Ack) parse("ACK\nid:missing:42\n\n\0");

    StompProtocolException error =
        assertThrows(StompProtocolException.class,
            () -> new AckListener().frameEvent(frame, engine, true));

    assertTrue(error.getMessage().contains("No subscription"));
  }

  @Test
  void nackRejectsUnknownSubscription() throws Exception {
    SessionState engine = engine(true);
    Nack frame = (Nack) parse("NACK\nid:missing:42\n\n\0");

    assertThrows(StompProtocolException.class,
        () -> new NackListener().frameEvent(frame, engine, true));
  }

  @ParameterizedTest
  @MethodSource("validVersions")
  void versionNegotiationChoosesHighestSupportedVersion(String offered, float expected) {
    SessionState engine = mock(SessionState.class);
    StompProtocol protocol = mock(StompProtocol.class);
    when(engine.getProtocol()).thenReturn(protocol);

    assertEquals(expected, new ExposedConnectListener().version(engine, offered));

    verify(protocol).setVersion(expected);
  }

  @ParameterizedTest
  @MethodSource("invalidVersions")
  void invalidVersionNegotiationSendsError(String offered) {
    SessionState engine = mock(SessionState.class);
    StompProtocol protocol = mock(StompProtocol.class);
    when(engine.getProtocol()).thenReturn(protocol);

    assertTrue(Float.isNaN(new ExposedConnectListener().version(engine, offered)));

    verify(engine).send(isA(io.mapsmessaging.network.protocol.impl.stomp.frames.Error.class));
    verify(protocol, never()).setVersion(anyFloat());
  }

  @Test
  void beginStartsNamedTransaction() throws Exception {
    SessionState engine = mock(SessionState.class);
    Session session = mock(Session.class);
    when(engine.getSession()).thenReturn(session);
    Begin begin = (Begin) parse("BEGIN\ntransaction:tx-1\n\n\0");

    new BeginListener().frameEvent(begin, engine, true);

    verify(session).startTransaction("tx-1");
  }

  @Test
  void commitCommitsAndClosesTransaction() throws Exception {
    SessionState engine = mock(SessionState.class);
    Session session = mock(Session.class);
    Transaction transaction = mock(Transaction.class);
    when(engine.getSession()).thenReturn(session);
    when(session.getTransaction("tx-1")).thenReturn(transaction);
    Commit commit = (Commit) parse("COMMIT\ntransaction:tx-1\n\n\0");

    new CommitListener().frameEvent(commit, engine, true);

    verify(transaction).commit();
    verify(session).closeTransaction(transaction);
  }

  @Test
  void abortAbortsAndClosesTransaction() throws Exception {
    SessionState engine = mock(SessionState.class);
    Session session = mock(Session.class);
    Transaction transaction = mock(Transaction.class);
    when(engine.getSession()).thenReturn(session);
    when(session.getTransaction("tx-1")).thenReturn(transaction);
    Abort abort = (Abort) parse("ABORT\ntransaction:tx-1\n\n\0");

    new AbortListener().frameEvent(abort, engine, true);

    verify(transaction).abort();
    verify(session).closeTransaction(transaction);
  }

  @Test
  void transactionLookupRejectsUnknownTransaction() throws Exception {
    SessionState engine = mock(SessionState.class);
    Session session = mock(Session.class);
    when(engine.getSession()).thenReturn(session);
    Commit commit = (Commit) parse("COMMIT\ntransaction:missing\n\n\0");

    assertThrows(StompProtocolException.class,
        () -> new CommitListener().frameEvent(commit, engine, true));
  }

  @Test
  void disconnectWithoutReceiptMovesToClosedState() {
    SessionState engine = mock(SessionState.class);
    Disconnect disconnect = new Disconnect();

    new DisconnectListener().frameEvent(disconnect, engine, true);

    verify(engine).changeState(isA(io.mapsmessaging.network.protocol.impl.stomp.state.ClosedState.class));
    verify(engine, never()).send(any());
  }

  @Test
  void disconnectWithReceiptSendsReceiptWhoseCompletionShutsDown() {
    SessionState engine = mock(SessionState.class);
    Disconnect disconnect = new Disconnect();
    disconnect.setReceipt("bye");

    new DisconnectListener().frameEvent(disconnect, engine, true);

    ArgumentCaptor<Frame> captor = ArgumentCaptor.forClass(Frame.class);
    verify(engine).send(captor.capture());
    Receipt receipt = assertInstanceOf(Receipt.class, captor.getValue());
    assertEquals("bye", receipt.getReceipt());
    receipt.complete();
    verify(engine).shutdown();
  }

  @Test
  void errorListenerInstallsShutdownCompletion() {
    SessionState engine = mock(SessionState.class);
    io.mapsmessaging.network.protocol.impl.stomp.frames.Error error =
        new io.mapsmessaging.network.protocol.impl.stomp.frames.Error();

    new ErrorListener().frameEvent(error, engine, true);

    error.complete();
    verify(engine).shutdown();
  }

  @Test
  void framePostHandlingCompletesDirectlyWithoutReceipt() {
    Frame frame = mock(Frame.class);
    SessionState engine = mock(SessionState.class);

    FrameListener listener = (ignored, state, end) -> {};
    listener.postFrameHandling(frame, engine);

    verify(frame).complete();
    verify(engine, never()).send(any());
  }

  @Test
  void framePostHandlingConvertsReceiptRequestIntoReceiptFrame() {
    Frame frame = mock(Frame.class);
    SessionState engine = mock(SessionState.class);
    when(frame.getReceipt()).thenReturn("r-1");

    FrameListener listener = (ignored, state, end) -> {};
    listener.postFrameHandling(frame, engine);

    ArgumentCaptor<Frame> captor = ArgumentCaptor.forClass(Frame.class);
    verify(engine).send(captor.capture());
    Receipt receipt = assertInstanceOf(Receipt.class, captor.getValue());
    assertEquals("r-1", receipt.getReceipt());
  }

  @Test
  void messageListenerIgnoresNullLookupFuture() throws Exception {
    SessionState engine = mock(SessionState.class);
    Session session = mock(Session.class);
    when(engine.getMapping("/topic/in")).thenReturn("/topic/out");
    when(engine.getSession()).thenReturn(session);
    when(session.findDestination("/topic/out", io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(null);

    new MessageListener().processEvent(
        engine, sendEvent("/topic/in", null), mock(Message.class));

    verifyNoMoreInteractions(session);
  }

  @Test
  void messageListenerIgnoresMissingDestination() throws Exception {
    SessionState engine = mock(SessionState.class);
    Session session = mock(Session.class);
    when(engine.getMapping("/topic/in")).thenReturn("/topic/out");
    when(engine.getSession()).thenReturn(session);
    when(session.findDestination("/topic/out", io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(null));

    new MessageListener().processEvent(
        engine, sendEvent("/topic/in", null), mock(Message.class));
  }

  @Test
  void messageListenerStoresMessageWhenDestinationExists() throws Exception {
    SessionState engine = mock(SessionState.class);
    Session session = mock(Session.class);
    Destination destination = mock(Destination.class);
    Message message = mock(Message.class);
    when(engine.getMapping("/topic/in")).thenReturn("/topic/out");
    when(engine.getSession()).thenReturn(session);
    when(session.findDestination("/topic/out", io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));

    new MessageListener().processEvent(
        engine, sendEvent("/topic/in", null), message);

    verify(destination).storeMessage(message);
  }

  @Test
  void messageListenerAddsMessageToKnownTransaction() throws Exception {
    SessionState engine = mock(SessionState.class);
    Session session = mock(Session.class);
    Destination destination = mock(Destination.class);
    Transaction transaction = mock(Transaction.class);
    Message message = mock(Message.class);
    when(engine.getMapping("/topic/in")).thenReturn("/topic/out");
    when(engine.getSession()).thenReturn(session);
    when(session.findDestination("/topic/out", io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));
    when(session.getTransaction("tx-1")).thenReturn(transaction);

    new MessageListener().processEvent(
        engine, sendEvent("/topic/in", "tx-1"), message);

    verify(transaction).add(destination, message);
    verify(destination, never()).storeMessage(any());
  }

  private SessionState engine(boolean stomp12) {
    SessionState engine = mock(SessionState.class);
    StompProtocol protocol = mock(StompProtocol.class);
    when(engine.getProtocol()).thenReturn(protocol);
    when(protocol.isStomp12()).thenReturn(stomp12);
    return engine;
  }

  private Frame parse(String wire) throws Exception {
    Packet packet = new Packet(ByteBuffer.wrap(wire.getBytes(StandardCharsets.UTF_8)));
    Frame frame = new FrameFactory(4096, false, false).parseFrame(packet);
    frame.scanFrame(packet);
    return frame;
  }

  private Send sendEvent(String destination, String transaction) throws Exception {
    StringBuilder wire = new StringBuilder("SEND\ndestination:")
        .append(destination).append("\n");
    if (transaction != null) {
      wire.append("transaction:").append(transaction).append("\n");
    }
    wire.append("\n\0");
    return (Send) parse(wire.toString());
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace(":", "\\c");
  }

  private static Stream<Arguments> acknowledgementIds() {
    return IntStream.range(0, 20)
        .mapToObj(i -> Arguments.of("sub:" + i, 1000L + i));
  }

  private static Stream<Arguments> legacyAcknowledgements() {
    return IntStream.range(0, 12)
        .mapToObj(i -> Arguments.of("sub-" + i, 2000L + i));
  }

  private static Stream<Arguments> validVersions() {
    return Stream.of(
        Arguments.of("1.0", 1.0f),
        Arguments.of("1.1", 1.1f),
        Arguments.of("1.2", 1.2f),
        Arguments.of("1.0,1.1", 1.1f),
        Arguments.of("1.0,1.2", 1.2f),
        Arguments.of("1.1,1.2", 1.2f),
        Arguments.of("1.0,1.1,1.2", 1.2f),
        Arguments.of(" 1.0 , 1.2 ", 1.2f)
    );
  }

  private static Stream<Arguments> invalidVersions() {
    return Stream.of(
        Arguments.of((Object) null),
        Arguments.of(""),
        Arguments.of(" "),
        Arguments.of("abc"),
        Arguments.of("2.0"),
        Arguments.of("0.9"),
        Arguments.of("1.2,garbage")
    );
  }

  private static final class ExposedConnectListener extends BaseConnectListener {
    float version(SessionState engine, String offered) {
      return processVersion(engine, offered);
    }

    @Override
    public void frameEvent(Frame frame, SessionState engine, boolean endOfBuffer) {
    }
  }
}
