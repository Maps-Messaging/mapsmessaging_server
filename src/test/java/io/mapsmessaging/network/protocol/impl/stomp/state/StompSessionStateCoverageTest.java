package io.mapsmessaging.network.protocol.impl.stomp.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocol;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Connect;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.listener.FrameListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StompSessionStateCoverageTest {

  @ParameterizedTest
  @MethodSource("mappingCases")
  void destinationMappingsRoundTrip(String source, String mapped, String lookup, String expected) {
    SessionState state = newState(false);

    if (source != null) {
      state.addMapping(source, mapped);
    }

    assertEquals(expected, state.getMapping(lookup));
  }

  @Test
  void setSessionMarksProtocolConnected() throws Exception {
    Fixture fixture = fixture(false);
    Session session = mock(Session.class);

    fixture.state.setSession(session);

    assertSame(session, fixture.state.getSession());
    verify(fixture.protocol).setConnected(true);
    verify(fixture.protocol).completedConnection();
  }

  @Test
  void createSubscriptionRegistersByAlias() throws Exception {
    Fixture fixture = fixture(false);
    Session session = mock(Session.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    SubscriptionContext context = new SubscriptionContext("/topic/a");
    context.setAlias("alias-a");
    when(session.addSubscription(context)).thenReturn(manager);
    fixture.state.setSession(session);

    assertSame(manager, fixture.state.createSubscription(context));
    assertSame(manager, fixture.state.findSubscription("alias-a"));
    verify(session).addSubscription(context);
  }

  @Test
  void queueSubscriptionRequestsQueueDestinationBeforeRegistration() throws Exception {
    Fixture fixture = fixture(false);
    Session session = mock(Session.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    SubscriptionContext context = new SubscriptionContext("/queue/orders");
    context.setAlias("orders");
    when(session.addSubscription(context)).thenReturn(manager);
    fixture.state.setSession(session);

    fixture.state.createSubscription(context);

    verify(session).findDestination(
        "/queue/orders", io.mapsmessaging.api.features.DestinationType.QUEUE);
    verify(session).addSubscription(context);
  }

  @Test
  void removeSubscriptionRemovesActiveEntryAndSessionSubscription() throws Exception {
    Fixture fixture = fixture(false);
    Session session = mock(Session.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    SubscriptionContext context = new SubscriptionContext("/topic/a");
    context.setAlias("alias-a");
    when(session.addSubscription(context)).thenReturn(manager);
    fixture.state.setSession(session);
    fixture.state.createSubscription(context);

    fixture.state.removeSubscription("alias-a");

    assertNull(fixture.state.findSubscription("alias-a"));
    verify(session).removeSubscription("alias-a");
  }

  @Test
  void removingUnknownSubscriptionIsNoOp() throws Exception {
    Fixture fixture = fixture(false);
    Session session = mock(Session.class);
    fixture.state.setSession(session);

    fixture.state.removeSubscription("missing");

    verify(session, never()).removeSubscription(anyString());
  }

  @Test
  void errorSendMarksStateInvalidAndInstallsShutdownCompletion() {
    Fixture fixture = fixture(false);
    io.mapsmessaging.network.protocol.impl.stomp.frames.Error error =
        new io.mapsmessaging.network.protocol.impl.stomp.frames.Error();

    assertTrue(fixture.state.send(error));
    assertFalse(fixture.state.isValid());

    verify(fixture.protocol).writeFrame(error);
    error.complete();
    verify(fixture.protocol).close();
  }

  @Test
  void ordinarySendLeavesStateValid() {
    Fixture fixture = fixture(false);
    Frame frame = mock(Frame.class);

    assertTrue(fixture.state.send(frame));
    assertTrue(fixture.state.isValid());
    verify(fixture.protocol).writeFrame(frame);
  }

  @Test
  void closedStateRejectsFramesAndMessages() {
    ClosedState state = new ClosedState();

    assertThrows(IOException.class,
        () -> state.handleFrame(mock(SessionState.class), mock(Frame.class), true));
    assertFalse(state.sendMessage(
        mock(SessionState.class), "/topic/a", null, mock(Message.class), () -> {}));
  }

  @Test
  void initialServerStateDelegatesConnectFrame() throws Exception {
    InitialServerState state = new InitialServerState();
    SessionState engine = mock(SessionState.class);
    FrameListener listener = mock(FrameListener.class);
    Connect connect = spy(new Connect());
    connect.setListener(listener);

    state.handleFrame(engine, connect, true);

    verify(listener).frameEvent(connect, engine, true);
    verify(listener).postFrameHandling(connect, engine);
  }

  @Test
  void initialServerStateRejectsNonConnectFrame() {
    InitialServerState state = new InitialServerState();

    assertThrows(StompProtocolException.class,
        () -> state.handleFrame(mock(SessionState.class), mock(Frame.class), true));
  }

  @Test
  void connectedStateDelegatesFrameAndCompletesIt() throws Exception {
    ConnectedState state = new ConnectedState();
    SessionState engine = mock(SessionState.class);
    Frame frame = mock(Frame.class);
    FrameListener listener = mock(FrameListener.class);
    when(frame.getFrameListener()).thenReturn(listener);

    state.handleFrame(engine, frame, true);

    verify(listener).frameEvent(frame, engine, true);
    verify(listener).postFrameHandling(frame, engine);
    verify(frame).complete();
  }

  @Test
  void connectedStateRejectsFrameWithoutListener() {
    ConnectedState state = new ConnectedState();
    Frame frame = mock(Frame.class);

    assertThrows(StompProtocolException.class,
        () -> state.handleFrame(mock(SessionState.class), frame, true));
  }

  @Test
  void connectedStateConvertsProtocolExceptionToErrorFrame() throws Exception {
    ConnectedState state = new ConnectedState();
    SessionState engine = mock(SessionState.class);
    Frame frame = mock(Frame.class);
    FrameListener listener = mock(FrameListener.class);
    when(frame.getFrameListener()).thenReturn(listener);
    doThrow(new StompProtocolException("bad frame"))
        .when(listener).frameEvent(frame, engine, true);

    state.handleFrame(engine, frame, true);

    ArgumentCaptor<Frame> captor = ArgumentCaptor.forClass(Frame.class);
    verify(engine).send(captor.capture());
    io.mapsmessaging.network.protocol.impl.stomp.frames.Error error =
        assertInstanceOf(io.mapsmessaging.network.protocol.impl.stomp.frames.Error.class,
            captor.getValue());
    assertTrue(new String(error.getContent(), StandardCharsets.UTF_8).contains("bad frame"));
  }

  @ParameterizedTest
  @MethodSource("acknowledgementModes")
  void connectedStateMessageAddsAckHeaderOnlyWhenRequired(
      ClientAcknowledgement acknowledgement, boolean expectedAck) {
    ConnectedState state = new ConnectedState();
    SessionState engine = mock(SessionState.class);
    StompProtocol protocol = mock(StompProtocol.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    Message message = new MessageBuilder()
        .setId(123)
        .setOpaqueData("payload".getBytes(StandardCharsets.UTF_8))
        .build();
    when(engine.getProtocol()).thenReturn(protocol);
    when(protocol.isBase64Encode()).thenReturn(false);
    when(protocol.isStomp12()).thenReturn(true);
    when(context.getAcknowledgementController()).thenReturn(acknowledgement);
    when(context.getAlias()).thenReturn("sub-1");
    when(engine.send(any(Frame.class))).thenReturn(true);

    assertTrue(state.sendMessage(engine, "/topic/a", context, message, () -> {}));

    ArgumentCaptor<Frame> captor = ArgumentCaptor.forClass(Frame.class);
    verify(engine).send(captor.capture());
    io.mapsmessaging.network.protocol.impl.stomp.frames.Message outbound =
        assertInstanceOf(io.mapsmessaging.network.protocol.impl.stomp.frames.Message.class,
            captor.getValue());
    assertEquals(expectedAck, outbound.getHeader().containsKey("ack"));
    assertEquals("sub-1", outbound.getHeader().get("subscription"));
    assertEquals("123", outbound.getHeader().get("message-id"));
  }

  @Test
  void connectedStateCompletionCallbackIsRetained() {
    ConnectedState state = new ConnectedState();
    SessionState engine = mock(SessionState.class);
    StompProtocol protocol = mock(StompProtocol.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    Message message = new MessageBuilder()
        .setId(7)
        .setOpaqueData(new byte[]{1})
        .build();
    java.util.concurrent.atomic.AtomicBoolean completed =
        new java.util.concurrent.atomic.AtomicBoolean(false);
    when(engine.getProtocol()).thenReturn(protocol);
    when(protocol.isBase64Encode()).thenReturn(false);
    when(protocol.isStomp12()).thenReturn(true);
    when(context.getAcknowledgementController()).thenReturn(ClientAcknowledgement.AUTO);
    when(context.getAlias()).thenReturn("sub");
    when(engine.send(any(Frame.class))).thenReturn(true);

    state.sendMessage(engine, "/topic/a", context, message, () -> completed.set(true));

    ArgumentCaptor<Frame> captor = ArgumentCaptor.forClass(Frame.class);
    verify(engine).send(captor.capture());
    captor.getValue().complete();
    assertTrue(completed.get());
  }

  private SessionState newState(boolean client) {
    return fixture(client).state;
  }

  private Fixture fixture(boolean client) {
    StompProtocol protocol = mock(StompProtocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    when(protocol.getEndPoint()).thenReturn(endPoint);
    when(endPoint.isClient()).thenReturn(client);
    return new Fixture(protocol, new SessionState(protocol));
  }

  private static Stream<Arguments> mappingCases() {
    return Stream.of(
        Arguments.of(null, null, "/topic/a", "/topic/a"),
        Arguments.of("/a", "/b", "/a", "/b"),
        Arguments.of("/a", "/b", "/missing", "/missing"),
        Arguments.of("a", "b", "a", "b"),
        Arguments.of("source", "/mapped/path", "source", "/mapped/path"),
        Arguments.of("/one", "/two", "/one", "/two"),
        Arguments.of("/one", "/two", "/two", "/two"),
        Arguments.of("", "mapped-empty", "", "mapped-empty"),
        Arguments.of(" spaced ", "mapped", " spaced ", "mapped"),
        Arguments.of("/case", "/CASE", "/case", "/CASE"),
        Arguments.of("/x/y", "/z", "/x/y", "/z"),
        Arguments.of("/x", "/z", "/x/child", "/x/child")
    );
  }

  private static Stream<Arguments> acknowledgementModes() {
    return Stream.of(
        Arguments.of(ClientAcknowledgement.AUTO, false),
        Arguments.of(ClientAcknowledgement.BLOCK, true),
        Arguments.of(ClientAcknowledgement.INDIVIDUAL, true)
    );
  }

  private record Fixture(StompProtocol protocol, SessionState state) {
  }
}
