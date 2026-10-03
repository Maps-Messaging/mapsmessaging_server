package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.OkFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RequestHandlerCoverageTest {

  @Test
  void dispatchesMatchingHandlerWithNullJsonForMissingPayload() throws Exception {
    JetStreamFrameHandler delegate = mock(JetStreamFrameHandler.class);
    when(delegate.getName()).thenReturn("STREAM.INFO");
    when(delegate.handle(any(), isNull(), any())).thenReturn(new OkFrame());
    TestRequestHandler handler = new TestRequestHandler(delegate);
    SessionState state = mock(SessionState.class);
    PayloadFrame frame = frame("$JS.API.STREAM.INFO.TEST", null, "_INBOX.1");

    NatsFrame result = handler.process(frame.getSubject(), frame, state);

    assertInstanceOf(OkFrame.class, result);
    verify(delegate).handle(same(frame), isNull(), same(state));
  }

  @Test
  void dispatchesMatchingHandlerWithParsedJsonPayload() throws Exception {
    JetStreamFrameHandler delegate = mock(JetStreamFrameHandler.class);
    when(delegate.getName()).thenReturn("STREAM.CREATE");
    when(delegate.handle(any(), any(JsonObject.class), any())).thenReturn(new OkFrame());
    TestRequestHandler handler = new TestRequestHandler(delegate);
    SessionState state = mock(SessionState.class);
    PayloadFrame frame = frame(
        "$JS.API.STREAM.CREATE.TEST",
        "{\"name\":\"TEST\",\"max_msgs\":10}".getBytes(StandardCharsets.UTF_8),
        "_INBOX.2");

    NatsFrame result = handler.process(frame.getSubject(), frame, state);

    assertInstanceOf(OkFrame.class, result);
    verify(delegate).handle(
        same(frame),
        argThat(json -> json.has("name")
            && "TEST".equals(json.get("name").getAsString())
            && json.get("max_msgs").getAsInt() == 10),
        same(state));
  }

  @Test
  void firstMatchingHandlerWins() throws Exception {
    JetStreamFrameHandler first = mock(JetStreamFrameHandler.class);
    JetStreamFrameHandler second = mock(JetStreamFrameHandler.class);
    when(first.getName()).thenReturn("STREAM");
    when(second.getName()).thenReturn("STREAM.INFO");
    OkFrame expected = new OkFrame();
    when(first.handle(any(), any(), any())).thenReturn(expected);
    TestRequestHandler handler = new TestRequestHandler(first, second);
    SessionState state = mock(SessionState.class);
    PayloadFrame frame = frame("$JS.API.STREAM.INFO.TEST", "{}".getBytes(), "_INBOX.3");

    assertSame(expected, handler.process(frame.getSubject(), frame, state));
    verify(first).handle(any(), any(), same(state));
    verify(second, never()).handle(any(), any(), any());
  }

  @Test
  void unknownActionBuilds501ResponseUsingJetStreamReplyMapping() throws Exception {
    TestRequestHandler handler = new TestRequestHandler();
    SessionState state = mock(SessionState.class);
    JetStreamRequestManager manager = mock(JetStreamRequestManager.class);
    when(state.getJetStreamRequestManager()).thenReturn(manager);
    when(manager.getJetSubject()).thenReturn("_INBOX.JS");
    when(manager.getSid("_INBOX.reply")).thenReturn("42");
    PayloadFrame frame = frame("$JS.API.UNKNOWN.ACTION", null, "_INBOX.reply");

    NatsFrame result = handler.process(frame.getSubject(), frame, state);

    assertInstanceOf(MsgFrame.class, result);
    MsgFrame message = (MsgFrame) result;
    assertEquals("_INBOX.JS", message.getSubject());
    assertEquals("42", message.getSubscriptionId());
    String json = new String(message.getPayload(), StandardCharsets.UTF_8);
    assertTrue(json.contains("\"code\": 501"));
    assertTrue(json.contains("\"err_code\": 1"));
    assertTrue(json.contains("Function not implemented: $JS.API.UNKNOWN.ACTION"));
  }

  @Test
  void createErrorIncludesSuppliedSubjectSubscriptionAndDescription() {
    TestRequestHandler handler = new TestRequestHandler();

    MsgFrame result = (MsgFrame) handler.error("_INBOX.JS", "7", "not implemented");

    assertEquals("_INBOX.JS", result.getSubject());
    assertEquals("7", result.getSubscriptionId());
    String json = new String(result.getPayload(), StandardCharsets.UTF_8);
    assertTrue(json.contains("io.nats.jetstream.api.v1.error"));
    assertTrue(json.contains("\"code\": 501"));
    assertTrue(json.contains("\"description\": \"not implemented\""));
  }

  @Test
  void getTypeIsProvidedByConcreteManager() {
    assertEquals("TEST", new TestRequestHandler().getType());
  }

  private static PayloadFrame frame(String subject, byte[] payload, String replyTo) {
    MsgFrame frame = new MsgFrame(1024);
    frame.setSubject(subject);
    frame.setPayload(payload);
    frame.setReplyTo(replyTo);
    return frame;
  }

  private static final class TestRequestHandler extends RequestHandler {
    TestRequestHandler(JetStreamFrameHandler... handlers) {
      super(handlers);
    }

    @Override
    public String getType() {
      return "TEST";
    }

    NatsFrame error(String subject, String subscriptionId, String message) {
      return createError(subject, subscriptionId, message);
    }
  }
}
