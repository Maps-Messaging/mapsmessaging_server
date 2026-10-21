/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.state.n2k;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.state.MessageHandler;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.util.SessionHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class N2kSessionLifecycleCoverageTest {

  @Test
  void startSkipsProtocolWhenDroneConfigurationIsMissing() {
    Fixture fixture = fixture(false);

    fixture.session.start();

    verifyNoInteractions(fixture.protocol);
  }

  @Test
  void startConnectsAndSubscribesConfiguredTopic() throws Exception {
    Fixture fixture = fixture(true);

    fixture.session.start();

    verify(fixture.protocol).connect(anyString(), eq("anonymous"), eq("anonymous"));
    verify(fixture.protocol).subscribeLocal(
        eq("/n2k/feed"),
        eq("/n2k/feed"),
        eq(QualityOfService.AT_MOST_ONCE),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull());
  }

  @Test
  void startContainsConnectFailureAndDoesNotSubscribe() throws Exception {
    Fixture fixture = fixture(true);
    doThrow(new IOException("connect failed"))
        .when(fixture.protocol).connect(anyString(), anyString(), anyString());

    assertDoesNotThrow(fixture.session::start);

    verify(fixture.protocol, never()).subscribeLocal(
        anyString(), anyString(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void startContainsSubscriptionFailureAfterConnect() throws Exception {
    Fixture fixture = fixture(true);
    doThrow(new IOException("subscribe failed"))
        .when(fixture.protocol).subscribeLocal(
            anyString(), anyString(), any(), any(), any(), any(), any(), any());

    assertDoesNotThrow(fixture.session::start);

    verify(fixture.protocol).connect(anyString(), eq("anonymous"), eq("anonymous"));
  }

  @Test
  void stopSkipsProtocolWhenDroneConfigurationIsMissing() {
    Fixture fixture = fixture(false);

    fixture.session.stop();

    verifyNoInteractions(fixture.protocol);
  }

  @Test
  void stopUnsubscribesAndClosesLoopbackProtocol() throws Exception {
    Fixture fixture = fixture(true);

    fixture.session.stop();

    verify(fixture.protocol).unsubscribeLocal("/n2k/feed");
    verify(fixture.protocol).close();
  }

  @Test
  void stopContainsCloseFailure() throws Exception {
    Fixture fixture = fixture(true);
    doThrow(new IOException("close failed")).when(fixture.protocol).close();

    assertDoesNotThrow(fixture.session::stop);

    verify(fixture.protocol).unsubscribeLocal("/n2k/feed");
  }

  @Test
  void stopPropagatesUncheckedUnsubscribeFailureWithoutClosing() throws Exception {
    Fixture fixture = fixture(true);
    doThrow(new IllegalStateException("unsubscribe failed"))
        .when(fixture.protocol).unsubscribeLocal("/n2k/feed");

    assertThrows(IllegalStateException.class, fixture.session::stop);

    verify(fixture.protocol, never()).close();
  }

  @ParameterizedTest
  @MethodSource("ignoredPayloads")
  void ignoredPayloadAlwaysCompletes(byte[] payload) {
    Fixture fixture = fixture(true);
    MessageEvent event = event("/n2k/feed", payload);

    assertDoesNotThrow(() -> fixture.session.handle(event));

    verify(event.getCompletionTask()).run();
    verifyNoInteractions(fixture.twinManager);
  }

  @ParameterizedTest
  @MethodSource("malformedJsonPayloads")
  void malformedJsonIsRethrownButStillCompletes(String json) {
    Fixture fixture = fixture(true);
    MessageEvent event = event("/n2k/feed", bytes(json));

    assertThrows(RuntimeException.class, () -> fixture.session.handle(event));

    verify(event.getCompletionTask()).run();
    verifyNoInteractions(fixture.twinManager);
  }

  @ParameterizedTest
  @MethodSource("validPayloads")
  void validPayloadBuildsContextAndUpdatesTwin(
      String json,
      String expectedSourceInstance,
      Long expectedSequence,
      String expectedReason) {

    Fixture fixture = fixture(true);
    when(fixture.twinManager.getTwin("usv-1")).thenReturn(Optional.empty());
    MessageEvent event = event("/n2k/feed", bytes(json));

    fixture.session.handle(event);

    ArgumentCaptor<TwinUpdateContext> contextCaptor =
        ArgumentCaptor.forClass(TwinUpdateContext.class);
    verify(fixture.twinManager)
        .registerTwin(any(DroneTwin.class), contextCaptor.capture());
    TwinUpdateContext context = contextCaptor.getValue();

    assertEquals("n2k-updater", context.getUpdateSource());
    assertEquals(expectedSourceInstance, context.getSourceInstanceId());
    assertEquals(expectedSequence, context.getSequenceNumber());
    assertEquals(expectedReason, context.getReason());
    assertNotNull(context.getReceivedTime());
    assertFalse(context.isFullSnapshot());

    verify(fixture.twinManager)
        .updateTwin(eq("usv-1"), any(), same(context));
    verify(event.getCompletionTask()).run();
  }

  @Test
  void repeatedValidPayloadUsesExistingTwinWithoutRegisteringAgain() {
    Fixture fixture = fixture(true);
    DroneTwin existing = mock(DroneTwin.class);
    when(fixture.twinManager.getTwin("usv-1")).thenReturn(Optional.of(existing));

    MessageEvent event = event(
        "/n2k/feed",
        bytes("{\"j1939\":{\"pgn\":130000,\"n2k\":{\"packet\":{}}}}"));

    fixture.session.handle(event);

    verify(fixture.twinManager, never()).registerTwin(any(), any());
    verify(fixture.twinManager).updateTwin(eq("usv-1"), any(), any());
    verify(event.getCompletionTask()).run();
  }

  @Test
  void twinUpdateRuntimeFailureIsRethrownAndStillCompletes() {
    Fixture fixture = fixture(true);
    when(fixture.twinManager.getTwin("usv-1"))
        .thenThrow(new IllegalStateException("twin failure"));
    MessageEvent event = event(
        "/n2k/feed",
        bytes("{\"j1939\":{\"pgn\":130000,\"n2k\":{\"packet\":{}}}}"));

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> fixture.session.handle(event));

    assertEquals("twin failure", exception.getMessage());
    verify(event.getCompletionTask()).run();
  }

  @Test
  void messageLookupRuntimeFailureIsRethrownAndStillCompletes() {
    Fixture fixture = fixture(true);
    MessageEvent event = mock(MessageEvent.class);
    Runnable completion = mock(Runnable.class);
    when(event.getDestinationName()).thenReturn("/n2k/feed");
    when(event.getCompletionTask()).thenReturn(completion);
    when(event.getMessage()).thenThrow(new IllegalStateException("message failure"));

    assertThrows(IllegalStateException.class, () -> fixture.session.handle(event));

    verify(completion).run();
  }

  @Test
  void payloadLookupRuntimeFailureIsRethrownAndStillCompletes() {
    Fixture fixture = fixture(true);
    MessageEvent event = mock(MessageEvent.class);
    Message message = mock(Message.class);
    Runnable completion = mock(Runnable.class);
    when(event.getDestinationName()).thenReturn("/n2k/feed");
    when(event.getMessage()).thenReturn(message);
    when(event.getCompletionTask()).thenReturn(completion);
    when(message.getOpaqueData()).thenThrow(new IllegalStateException("payload failure"));

    assertThrows(IllegalStateException.class, () -> fixture.session.handle(event));

    verify(completion).run();
  }

  @Test
  void completionFailurePropagatesAfterIgnoredPayload() {
    Fixture fixture = fixture(true);
    MessageEvent event = mock(MessageEvent.class);
    Message message = mock(Message.class);
    Runnable completion = mock(Runnable.class);
    when(event.getDestinationName()).thenReturn("/n2k/feed");
    when(event.getMessage()).thenReturn(message);
    when(event.getCompletionTask()).thenReturn(completion);
    when(message.getOpaqueData()).thenReturn(new byte[0]);
    doThrow(new IllegalStateException("completion failure")).when(completion).run();

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> fixture.session.handle(event));

    assertEquals("completion failure", exception.getMessage());
    verify(completion).run();
  }

  @Test
  void missingDroneConfigurationCompletesWithoutReadingMessage() {
    Fixture fixture = fixture(false);
    MessageEvent event = mock(MessageEvent.class);
    Runnable completion = mock(Runnable.class);
    when(event.getDestinationName()).thenReturn("/n2k/feed");
    when(event.getCompletionTask()).thenReturn(completion);

    fixture.session.handle(event);

    verify(event, never()).getMessage();
    verify(completion).run();
    verifyNoInteractions(fixture.twinManager);
  }

  private static Fixture fixture(boolean withDrone) {
    StateLoopProtocol protocol = mock(StateLoopProtocol.class);
    TwinManager twinManager = mock(TwinManager.class);
    DroneInfoRegistry registry = mock(DroneInfoRegistry.class);
    N2KTwinConfig config = new N2KTwinConfig();
    config.setName("usv-1");
    config.setTopic("/n2k/feed");

    DroneInfoDTO droneInfo = withDrone ? droneInfo() : null;
    when(registry.getDroneInfo("usv-1")).thenReturn(droneInfo);

    N2kSession session;
    try (MockedStatic<SessionHelper> helper = mockStatic(SessionHelper.class)) {
      helper.when(() -> SessionHelper.createLoopbackProtocol(any(MessageHandler.class)))
          .thenReturn(protocol);
      session = new N2kSession(twinManager, config, registry);
    }

    return new Fixture(session, protocol, twinManager);
  }

  private static DroneInfoDTO droneInfo() {
    DroneInfoDTO droneInfo = mock(DroneInfoDTO.class);
    when(droneInfo.getUuid())
        .thenReturn(UUID.fromString("12345678-1234-1234-1234-123456789abc"));
    when(droneInfo.getDataProducts()).thenReturn(List.of());
    return droneInfo;
  }

  private static MessageEvent event(String sourceName, byte[] payload) {
    MessageEvent event = mock(MessageEvent.class);
    Message message = mock(Message.class);
    Runnable completion = mock(Runnable.class);

    when(event.getDestinationName()).thenReturn(sourceName);
    when(event.getMessage()).thenReturn(message);
    when(event.getCompletionTask()).thenReturn(completion);
    when(message.getOpaqueData()).thenReturn(payload);
    return event;
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static Stream<Arguments> ignoredPayloads() {
    return Stream.of(
        Arguments.of((Object) null),
        Arguments.of((Object) new byte[0]),
        Arguments.of((Object) bytes("x")),
        Arguments.of((Object) bytes("[]")),
        Arguments.of((Object) bytes("null")),
        Arguments.of((Object) bytes("{}")),
        Arguments.of((Object) bytes("{\"j1939\":null}")),
        Arguments.of((Object) bytes("{\"j1939\":1}")),
        Arguments.of((Object) bytes("{\"j1939\":\"x\"}")),
        Arguments.of((Object) bytes("{\"j1939\":[]}")),
        Arguments.of((Object) bytes("{\"j1939\":{}}")),
        Arguments.of((Object) bytes("{\"j1939\":{\"pgn\":null}}")),
        Arguments.of((Object) bytes("{\"j1939\":{\"pgn\":127250}}")),
        Arguments.of((Object) bytes("{\"j1939\":{\"pgn\":127250,\"n2k\":null}}")),
        Arguments.of((Object) bytes("{\"j1939\":{\"pgn\":127250,\"n2k\":1}}")),
        Arguments.of((Object) bytes("{\"j1939\":{\"pgn\":127250,\"n2k\":[]}}")),
        Arguments.of((Object) bytes("{\"j1939\":{\"pgn\":127250,\"n2k\":{}}}")),
        Arguments.of((Object) bytes("{\"j1939\":{\"pgn\":127250,\"n2k\":{\"packet\":null}}}")),
        Arguments.of((Object) bytes("{\"j1939\":{\"pgn\":127250,\"n2k\":{\"packet\":1}}}")),
        Arguments.of((Object) bytes("{\"j1939\":{\"pgn\":127250,\"n2k\":{\"packet\":[]}}}"))
    );
  }

  private static Stream<Arguments> malformedJsonPayloads() {
    return Stream.of(
        Arguments.of("{"),
        Arguments.of("{]"),
        Arguments.of("{\"j1939\":"),
        Arguments.of("{\"j1939\":{\"pgn\":}}")
    );
  }

  private static Stream<Arguments> validPayloads() {
    return Stream.of(
        Arguments.of(
            "{\"j1939\":{\"pgn\":130000,\"n2k\":{\"packet\":{}}}}",
            "/n2k/feed", null, null),
        Arguments.of(
            "{\"j1939\":{\"source\":0,\"pgn\":130000,\"n2k\":{\"packet\":{}}}}",
            "/n2k/feed:source-0", null, null),
        Arguments.of(
            "{\"j1939\":{\"source\":255,\"pgn\":130000,\"n2k\":{\"packet\":{}}}}",
            "/n2k/feed:source-255", null, null),
        Arguments.of(
            "{\"j1939\":{\"pgn\":130000,\"n2k\":{\"packet\":{\"sequenceId\":0}}}}",
            "/n2k/feed", 0L, null),
        Arguments.of(
            "{\"j1939\":{\"pgn\":130000,\"n2k\":{\"packet\":{\"sequenceId\":12.9}}}}",
            "/n2k/feed", 12L, null),
        Arguments.of(
            "{\"j1939\":{\"pgn\":130000,\"n2k\":{\"packet\":{\"sid\":7}}}}",
            "/n2k/feed", 7L, null),
        Arguments.of(
            "{\"j1939\":{\"pgn\":130000,\"n2k\":{\"packet\":{\"sequenceId\":5,\"sid\":7}}}}",
            "/n2k/feed", 5L, null),
        Arguments.of(
            "{\"j1939\":{\"pgn\":130000,\"n2k\":{\"name\":\"\",\"packet\":{}}}}",
            "/n2k/feed", null, ""),
        Arguments.of(
            "{\"j1939\":{\"pgn\":130000,\"n2k\":{\"name\":\"Heading\",\"packet\":{}}}}",
            "/n2k/feed", null, "Heading"),
        Arguments.of(
            "{\"j1939\":{\"source\":23,\"pgn\":130000,\"n2k\":{\"name\":\"Position\",\"packet\":{\"sid\":15}}}}",
            "/n2k/feed:source-23", 15L, "Position")
    );
  }

  private record Fixture(
      N2kSession session,
      StateLoopProtocol protocol,
      TwinManager twinManager) {
  }
}
