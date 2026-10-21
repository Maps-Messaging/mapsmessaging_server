/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.mavlink;

import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MavlinkStateSubscriberParsingCoverageTest {

  @ParameterizedTest
  @MethodSource("integrationSourceCases")
  void integrationSourceUsesStableFallbacks(
      String name, String topic, String expected) {
    io.mapsmessaging.state.config.MavlinkTwinConfigDTO config =
        mock(io.mapsmessaging.state.config.MavlinkTwinConfigDTO.class);
    when(config.getName()).thenReturn(name);
    when(config.getTopic()).thenReturn(topic);

    assertEquals(expected, MavlinkStateSubscriber.integrationSource(config));
  }

  @Test
  void packageConstructorRejectsEveryNullCollaborator() {
    StateLoopProtocol protocol = mock(StateLoopProtocol.class);
    MavlinkSourceRegistry sources = mock(MavlinkSourceRegistry.class);
    DroneInfoRegistry drones = mock(DroneInfoRegistry.class);
    MavlinkTwinUpdater updater = mock(MavlinkTwinUpdater.class);

    assertThrows(NullPointerException.class,
        () -> new MavlinkStateSubscriber(null, "topic", sources, drones, updater));
    assertThrows(NullPointerException.class,
        () -> new MavlinkStateSubscriber(protocol, null, sources, drones, updater));
    assertThrows(NullPointerException.class,
        () -> new MavlinkStateSubscriber(protocol, "topic", null, drones, updater));
    assertThrows(NullPointerException.class,
        () -> new MavlinkStateSubscriber(protocol, "topic", sources, null, updater));
    assertThrows(NullPointerException.class,
        () -> new MavlinkStateSubscriber(protocol, "topic", sources, drones, null));
  }

  @ParameterizedTest
  @MethodSource("invalidJsonCases")
  void parseJsonRejectsMalformedAndIncompleteEnvelopes(byte[] payload) throws Exception {
    Fixture fixture = fixture();

    assertNull(parseJson(fixture.subscriber, payload));
  }

  @ParameterizedTest
  @MethodSource("emptyDecodedCases")
  void parseJsonAcceptsValidFramesWithoutDecodedObject(String payload) throws Exception {
    Fixture fixture = fixture();

    ProcessedFrame frame = parseJson(
        fixture.subscriber, payload.getBytes(StandardCharsets.UTF_8));

    assertNotNull(frame);
    assertEquals(0, frame.getFrame().getMessageId());
    assertEquals(3, frame.getFrame().getSystemId());
    assertEquals(1, frame.getFrame().getComponentId());
    assertEquals(9, frame.getFrame().getSequence());
    assertTrue(frame.getFields().isEmpty());
  }

  @Test
  void parseJsonDecodesPayloadFields() throws Exception {
    Fixture fixture = fixture();

    ProcessedFrame frame = parseJson(
        fixture.subscriber, validJson("{\"custom_mode\":4,\"armed\":true}")
            .getBytes(StandardCharsets.UTF_8));

    assertNotNull(frame);
    assertEquals(2, frame.getFields().size());
    assertEquals(4.0, frame.getFields().get("custom_mode"));
    assertEquals(true, frame.getFields().get("armed"));
  }

  @Test
  void subscribeFailureClosesProtocolAndUpdaterAndClosesSubscriber() throws Exception {
    Fixture fixture = fixture();
    IOException failure = new IOException("subscribe");
    doThrow(failure).when(fixture.protocol).subscribeLocal(
        anyString(), anyString(), any(), any(), any(), any(), any(), any());

    IOException thrown = assertThrows(IOException.class, fixture.subscriber::start);

    assertSame(failure, thrown);
    verify(fixture.protocol).close();
    verify(fixture.updater).close();
    assertThrows(IllegalStateException.class, fixture.subscriber::start);
  }

  @Test
  void stopPropagatesUpdaterFailureWhenProtocolCleanupSucceeds() throws Exception {
    Fixture fixture = fixture();
    RuntimeException failure = new IllegalStateException("updater");
    doThrow(failure).when(fixture.updater).close();

    RuntimeException thrown = assertThrows(RuntimeException.class, fixture.subscriber::stop);

    assertSame(failure, thrown);
    verify(fixture.protocol).close();
  }

  @Test
  void stopRetainsCleanupFailuresAsSuppressedInOccurrenceOrder() throws Exception {
    Fixture fixture = fixture();
    fixture.subscriber.start();
    RuntimeException unsubscribeFailure = new IllegalStateException("unsubscribe");
    IOException closeFailure = new IOException("close");
    RuntimeException updaterFailure = new IllegalArgumentException("updater");
    doThrow(unsubscribeFailure).when(fixture.protocol).unsubscribeLocal("mavlink/state");
    doThrow(closeFailure).when(fixture.protocol).close();
    doThrow(updaterFailure).when(fixture.updater).close();

    RuntimeException thrown = assertThrows(RuntimeException.class, fixture.subscriber::stop);

    assertSame(unsubscribeFailure, thrown);
    assertEquals(2, thrown.getSuppressed().length);
    assertSame(closeFailure, thrown.getSuppressed()[0]);
    assertSame(updaterFailure, thrown.getSuppressed()[1]);
  }

  @Test
  void conversionFailureStillCompletesAndIsRethrown() {
    Fixture fixture = fixture();
    io.mapsmessaging.api.MessageEvent event = mock(io.mapsmessaging.api.MessageEvent.class);
    Runnable completion = mock(Runnable.class);
    io.mapsmessaging.api.message.Message message = mock(io.mapsmessaging.api.message.Message.class);
    when(event.getDestinationName()).thenReturn("/mavlink/source");
    when(event.getMessage()).thenReturn(message);
    when(event.getCompletionTask()).thenReturn(completion);
    when(message.getOpaqueData()).thenReturn(validJson("{}").getBytes(StandardCharsets.UTF_8));
    MavlinkKnownSourceDTO source = mock(MavlinkKnownSourceDTO.class);
    when(source.getName()).thenReturn("drone-1");
    when(fixture.sources.getKnownSource(any())).thenThrow(new IllegalStateException("source"));

    assertThrows(IllegalStateException.class, () -> fixture.subscriber.handle(event));

    verify(completion).run();
  }

  private static Fixture fixture() {
    StateLoopProtocol protocol = mock(StateLoopProtocol.class);
    MavlinkSourceRegistry sources = mock(MavlinkSourceRegistry.class);
    DroneInfoRegistry drones = mock(DroneInfoRegistry.class);
    MavlinkTwinUpdater updater = mock(MavlinkTwinUpdater.class);
    MavlinkStateSubscriber subscriber =
        new MavlinkStateSubscriber(protocol, "mavlink/state", sources, drones, updater);
    return new Fixture(protocol, sources, updater, subscriber);
  }

  private static ProcessedFrame parseJson(
      MavlinkStateSubscriber subscriber, byte[] payload) throws Exception {
    Method method =
        MavlinkStateSubscriber.class.getDeclaredMethod("parseJson", byte[].class, String.class);
    method.setAccessible(true);
    try {
      return (ProcessedFrame) method.invoke(subscriber, payload, "/mavlink/source");
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof Exception checked) {
        throw checked;
      }
      throw exception;
    }
  }

  private static String validJson(String decoded) {
    return """
        {
          "mavlink": {
            "version": "V2",
            "messageId": 0,
            "systemId": 3,
            "componentId": 1,
            "sequence": 9,
            "payloadLength": 9,
            "signed": false,
            "payload": {
              "decoded": %s
            }
          }
        }
        """.formatted(decoded);
  }

  private static Stream<Arguments> integrationSourceCases() {
    return Stream.of(
        Arguments.of("primary", "/mavlink/+", "primary|/mavlink/+"),
        Arguments.of(null, "/mavlink/+", "mavlink|/mavlink/+"),
        Arguments.of("", "/mavlink/+", "mavlink|/mavlink/+"),
        Arguments.of(" ", "/mavlink/+", "mavlink|/mavlink/+"),
        Arguments.of("primary", null, "primary|unknown"),
        Arguments.of("primary", "", "primary|unknown"),
        Arguments.of("primary", " ", "primary|unknown"),
        Arguments.of(null, null, "mavlink|unknown"),
        Arguments.of(" ", " ", "mavlink|unknown")
    );
  }

  private static Stream<Arguments> invalidJsonCases() {
    return Stream.of(
        Arguments.of((Object) null),
        Arguments.of(new byte[0]),
        Arguments.of(" ".getBytes(StandardCharsets.UTF_8)),
        Arguments.of("not-json".getBytes(StandardCharsets.UTF_8)),
        Arguments.of("[]".getBytes(StandardCharsets.UTF_8)),
        Arguments.of("null".getBytes(StandardCharsets.UTF_8)),
        Arguments.of("{}".getBytes(StandardCharsets.UTF_8)),
        Arguments.of("{\"mavlink\":null}".getBytes(StandardCharsets.UTF_8)),
        Arguments.of("{\"mavlink\":{}}".getBytes(StandardCharsets.UTF_8)),
        Arguments.of("{\"mavlink\":{\"messageId\":0}}".getBytes(StandardCharsets.UTF_8)),
        Arguments.of("{\"mavlink\":{\"messageId\":0,\"payload\":null}}"
            .getBytes(StandardCharsets.UTF_8)),
        Arguments.of("{\"mavlink\":{\"messageId\":null,\"payload\":{}}}"
            .getBytes(StandardCharsets.UTF_8)),
        Arguments.of("{\"mavlink\":{\"messageId\":0,\"payload\":{},\"version\":\"BAD\"}}"
            .getBytes(StandardCharsets.UTF_8)),
        Arguments.of("{\"mavlink\":{\"messageId\":0,\"payload\":{},\"version\":\"V2\"}}"
            .getBytes(StandardCharsets.UTF_8))
    );
  }

  private static Stream<Arguments> emptyDecodedCases() {
    String base = """
        {
          "mavlink": {
            "version": "V2",
            "messageId": 0,
            "systemId": 3,
            "componentId": 1,
            "sequence": 9,
            "payloadLength": 9,
            "signed": false,
            "payload": %s
          }
        }
        """;
    return Stream.of(
        Arguments.of(base.formatted("{}")),
        Arguments.of(base.formatted("{\"decoded\":null}")),
        Arguments.of(base.formatted("{\"decoded\":5}")),
        Arguments.of(base.formatted("{\"decoded\":\"text\"}"))
    );
  }

  private record Fixture(
      StateLoopProtocol protocol,
      MavlinkSourceRegistry sources,
      MavlinkTwinUpdater updater,
      MavlinkStateSubscriber subscriber) {
  }
}
