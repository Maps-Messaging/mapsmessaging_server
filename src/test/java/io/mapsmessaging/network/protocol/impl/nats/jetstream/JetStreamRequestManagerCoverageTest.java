package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import io.mapsmessaging.dto.rest.config.protocol.impl.NatsConfigDTO;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocol;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JetStreamRequestManagerCoverageTest {

  @ParameterizedTest
  @MethodSource("classificationCases")
  void classifiesJetStreamNamespaces(String subject, boolean expected) {
    JetStreamRequestManager manager = new JetStreamRequestManager();
    PayloadFrame frame = new MsgFrame(128);
    frame.setSubject(subject);
    assertEquals(expected, manager.isJetStreamRequest(frame));
  }

  @Test
  void sidLookupUsesRegisteredPrefixAndCloseClearsMappings() {
    JetStreamRequestManager manager = new JetStreamRequestManager();
    manager.registerSid("_INBOX.A.", "sid-a");
    manager.registerSid("_INBOX.B.", "sid-b");

    assertEquals("sid-a", manager.getSid("_INBOX.A.123"));
    assertEquals("sid-b", manager.getSid("_INBOX.B.456"));
    assertEquals("", manager.getSid("_INBOX.C.789"));

    manager.close();

    assertEquals("", manager.getSid("_INBOX.A.123"));
  }

  @Test
  void jetSubjectRoundTrips() {
    JetStreamRequestManager manager = new JetStreamRequestManager();
    manager.setJetSubject("_INBOX.JS");
    assertEquals("_INBOX.JS", manager.getJetSubject());
  }

  @ParameterizedTest
  @MethodSource("disabledCases")
  void disabledFeaturesReturnSpecificErrors(
      String subject, boolean streams, boolean keyValues, boolean objectStore, String expected)
      throws Exception {
    JetStreamRequestManager manager = new JetStreamRequestManager();
    SessionState state = state(streams, keyValues, objectStore);
    PayloadFrame frame = new MsgFrame(128);
    frame.setSubject(subject);

    NatsFrame response = manager.process(frame, state);

    assertInstanceOf(ErrFrame.class, response);
    assertEquals(expected, ((ErrFrame) response).getError());
  }

  @ParameterizedTest
  @MethodSource("enabledUnimplementedCases")
  void enabledUnsupportedFeatureFamiliesReportNotImplemented(
      String subject, boolean keyValues, boolean objectStore, String expected) throws Exception {
    JetStreamRequestManager manager = new JetStreamRequestManager();
    SessionState state = state(false, keyValues, objectStore);
    PayloadFrame frame = new MsgFrame(128);
    frame.setSubject(subject);

    NatsFrame response = manager.process(frame, state);

    assertInstanceOf(ErrFrame.class, response);
    assertEquals(expected, ((ErrFrame) response).getError());
  }

  @Test
  void nonJetStreamSubjectReturnsUnknownRequestError() throws Exception {
    JetStreamRequestManager manager = new JetStreamRequestManager();
    SessionState state = state(true, true, true);
    PayloadFrame frame = new MsgFrame(128);
    frame.setSubject("foo.bar");

    NatsFrame response = manager.process(frame, state);

    assertInstanceOf(ErrFrame.class, response);
    assertEquals("Unknown JetStream request: foo.bar", ((ErrFrame) response).getError());
  }

  private static SessionState state(boolean streams, boolean keyValues, boolean objectStore) {
    SessionState state = mock(SessionState.class);
    NatsProtocol protocol = mock(NatsProtocol.class);
    NatsConfigDTO config = new NatsConfigDTO();
    config.setEnableStreams(streams);
    config.setEnableKeyValues(keyValues);
    config.setEnableObjectStore(objectStore);
    when(state.getProtocol()).thenReturn(protocol);
    when(protocol.getNatsConfig()).thenReturn(config);
    return state;
  }

  private static Stream<Arguments> classificationCases() {
    return Stream.of(
        Arguments.of(null, false),
        Arguments.of("", false),
        Arguments.of("foo", false),
        Arguments.of("$J", false),
        Arguments.of("$JS", true),
        Arguments.of("$JS.", true),
        Arguments.of("$JS.API.INFO", true),
        Arguments.of("$KV", true),
        Arguments.of("$KV.bucket.key", true),
        Arguments.of("$O", true),
        Arguments.of("$O.bucket", true),
        Arguments.of("x$JS.API", false)
    );
  }

  private static Stream<Arguments> disabledCases() {
    return Stream.of(
        Arguments.of("$JS.API.INFO", false, false, false, "Streams are disabled"),
        Arguments.of("$KV.bucket", false, false, false, "Key Values are disabled"),
        Arguments.of("$O.bucket", false, false, false, "Object Store is disabled")
    );
  }

  private static Stream<Arguments> enabledUnimplementedCases() {
    return Stream.of(
        Arguments.of("$KV.bucket", true, false, "KeyValue handler not yet implemented"),
        Arguments.of("$O.bucket", false, true, "ObjectStore handler not yet implemented")
    );
  }
}
