package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class NatsFrameBoundaryCoverageTest {

  @ParameterizedTest
  @MethodSource("booleanCases")
  void booleanExtractionHandlesPresentMissingAndTerminatedValues(
      String json, String key, boolean expected) {
    HelperFrame frame = new HelperFrame();
    assertEquals(expected, frame.bool(json, key));
  }

  @ParameterizedTest
  @MethodSource("stringCases")
  void stringExtractionHandlesPresentMissingAndWhitespace(
      String json, String key, String expected) {
    HelperFrame frame = new HelperFrame();
    assertEquals(expected, frame.string(json, key));
  }

  @ParameterizedTest
  @MethodSource("errCases")
  void errFrameParsesQuotedErrorsAndMalformedFormat(String line, String expected) throws Exception {
    ExposedErrFrame frame = new ExposedErrFrame();
    frame.parse(line);
    assertEquals(expected, frame.getError());
    assertTrue(frame.isValid());
    assertNull(frame.getFromAddress());
    assertInstanceOf(ErrFrame.class, frame.instance());
  }

  @Test
  void emptyErrFrameStartsInvalid() {
    ErrFrame frame = new ErrFrame();
    assertFalse(frame.isValid());
    assertNull(frame.getError());
  }

  @ParameterizedTest
  @MethodSource("errPackCases")
  void errFramePackingUsesExpectedWireFormat(String error, String expected) {
    ErrFrame frame = new ErrFrame(error);
    Packet packet = new Packet(256, false);
    int written = frame.packFrame(packet);
    packet.flip();
    byte[] data = new byte[packet.available()];
    packet.get(data);
    assertEquals(expected, new String(data, StandardCharsets.US_ASCII));
    assertEquals(expected.length(), written);
  }

  @ParameterizedTest
  @MethodSource("unsubCases")
  void unsubFrameParsesValidHeaders(String line, String sid, Integer max) throws Exception {
    ExposedUnsubFrame frame = new ExposedUnsubFrame();
    frame.parse(line);
    assertEquals(sid, frame.getSubscriptionId());
    assertEquals(max, frame.getMaxMessages());
    assertTrue(frame.isValid());
    assertNull(frame.getFromAddress());
  }

  @ParameterizedTest
  @MethodSource("invalidUnsubCases")
  void unsubFrameRejectsInvalidHeaders(String line, Class<? extends Throwable> expected) {
    ExposedUnsubFrame frame = new ExposedUnsubFrame();
    assertThrows(expected, () -> frame.parse(line));
  }

  @ParameterizedTest
  @MethodSource("payloadLineCases")
  void payloadHeaderParsingHandlesReplyAndBounds(
      String line, String subject, String reply, int size) throws Exception {
    MsgFrame frame = new MsgFrame(64);
    frame.parseLine(line);
    assertEquals(subject, frame.getSubject());
    assertEquals(reply, frame.getReplyTo());
    assertEquals(size, frame.getPayloadSize());
    assertTrue(frame.isValid());
  }

  @ParameterizedTest
  @MethodSource("invalidPayloadLineCases")
  void payloadHeaderRejectsMalformedOrOversizeHeaders(String line) {
    MsgFrame frame = new MsgFrame(8);
    assertThrows(Exception.class, () -> frame.parseLine(line));
  }

  @ParameterizedTest
  @MethodSource("payloadPackCases")
  void payloadPackingProducesExpectedWireFormat(
      String subject, String subscriptionId, String replyTo, byte[] payload, String expected) {
    MsgFrame frame = new MsgFrame(256);
    frame.setSubject(subject);
    frame.setSubscriptionId(subscriptionId);
    frame.setReplyTo(replyTo);
    frame.setPayload(payload);
    Packet packet = new Packet(512, false);

    int written = frame.packFrame(packet);
    packet.flip();
    byte[] data = new byte[packet.available()];
    packet.get(data);
    assertEquals(expected, new String(data, StandardCharsets.US_ASCII));
    assertEquals(expected.length(), written);
  }

  @Test
  void duplicateCopiesPayloadFrameState() {
    MsgFrame source = new MsgFrame(512);
    source.setSubject("foo.bar");
    source.setSubscriptionId("sid");
    source.setReplyTo("_INBOX.x");
    source.setPayloadSize(3);
    source.setPayload(new byte[]{1, 2, 3});

    PayloadFrame duplicate = source.duplicate();

    assertNotSame(source, duplicate);
    assertEquals(source.getSubject(), duplicate.getSubject());
    assertEquals(source.getSubscriptionId(), duplicate.getSubscriptionId());
    assertEquals(source.getReplyTo(), duplicate.getReplyTo());
    assertEquals(source.getPayloadSize(), duplicate.getPayloadSize());
    assertArrayEquals(source.getPayload(), duplicate.getPayload());
    assertEquals(512, duplicate.getMaxBufferSize());
  }

  @Test
  void okFrameContractIsConstant() {
    OkFrame frame = new OkFrame();
    assertArrayEquals("+OK".getBytes(StandardCharsets.US_ASCII), frame.getCommand());
    assertTrue(frame.isValid());
    assertInstanceOf(OkFrame.class, frame.instance());
    assertNull(frame.getFromAddress());
  }

  private static Stream<Arguments> booleanCases() {
    return Stream.of(
        Arguments.of("{\"verbose\":true}", "\"verbose\":", true),
        Arguments.of("{\"verbose\":TRUE}", "\"verbose\":", true),
        Arguments.of("{\"verbose\":false}", "\"verbose\":", false),
        Arguments.of("{\"verbose\": true,\"x\":1}", "\"verbose\":", true),
        Arguments.of("{\"verbose\": false,\"x\":1}", "\"verbose\":", false),
        Arguments.of("{\"x\":true}", "\"verbose\":", false),
        Arguments.of("{}", "\"verbose\":", false),
        Arguments.of("{\"verbose\":null}", "\"verbose\":", false)
    );
  }

  private static Stream<Arguments> stringCases() {
    return Stream.of(
        Arguments.of("{\"user\":\"alice\"}", "\"user\":", "alice"),
        Arguments.of("{\"user\": \"alice\"}", "\"user\":", "alice"),
        Arguments.of("{\"user\":\"a b c\"}", "\"user\":", "a b c"),
        Arguments.of("{\"user\":\"\"}", "\"user\":", null),
        Arguments.of("{\"other\":\"alice\"}", "\"user\":", null),
        Arguments.of("{}", "\"user\":", null),
        Arguments.of("{\"user\":null}", "\"user\":", null)
    );
  }

  private static Stream<Arguments> errCases() {
    return Stream.of(
        Arguments.of("'Authorization Violation'", "Authorization Violation"),
        Arguments.of(" 'Permissions Violation' ", "Permissions Violation"),
        Arguments.of("'x'", "x"),
        Arguments.of("no-quotes", "Unknown Error Format"),
        Arguments.of("'unterminated", "Unknown Error Format"),
        Arguments.of("", "Unknown Error Format")
    );
  }

  private static Stream<Arguments> errPackCases() {
    return Stream.of(
        Arguments.of("boom", "-ERR 'boom'\r\n"),
        Arguments.of("", "-ERR ''\r\n"),
        Arguments.of(null, "-ERR 'Unknown Error'\r\n")
    );
  }

  private static Stream<Arguments> unsubCases() {
    return Stream.of(
        Arguments.of("1", "1", null),
        Arguments.of("sid", "sid", null),
        Arguments.of("sid 1", "sid", 1),
        Arguments.of("sid 1000", "sid", 1000),
        Arguments.of("  sid 5  ", "sid", 5),
        Arguments.of("sid 0", "sid", 0),
        Arguments.of("sid -1", "sid", -1)
    );
  }

  private static Stream<Arguments> invalidUnsubCases() {
    return Stream.of(
        Arguments.of("a b c", NatsProtocolException.class),
        Arguments.of("sid not-a-number", NumberFormatException.class),
        Arguments.of("sid 1 2", NatsProtocolException.class)
    );
  }

  private static Stream<Arguments> payloadLineCases() {
    return Stream.of(
        Arguments.of("foo 0", "foo", null, 0),
        Arguments.of("foo 5", "foo", null, 5),
        Arguments.of("foo _INBOX.x 5", "foo", "_INBOX.x", 5),
        Arguments.of("a.b.c reply 64", "a.b.c", "reply", 64),
        Arguments.of("  foo 7  ", "foo", null, 7)
    );
  }

  private static Stream<Arguments> invalidPayloadLineCases() {
    return Stream.of(
        Arguments.of("foo"),
        Arguments.of("foo a b c"),
        Arguments.of("foo 9"),
        Arguments.of("foo reply 9"),
        Arguments.of("foo nope"),
        Arguments.of("foo reply nope")
    );
  }

  private static Stream<Arguments> payloadPackCases() {
    return Stream.of(
        Arguments.of("foo", null, null, null, "MSG foo 0\r\n\r\n"),
        Arguments.of("foo", "", "", new byte[0], "MSG foo 0\r\n\r\n"),
        Arguments.of("foo", "1", null, "abc".getBytes(StandardCharsets.US_ASCII),
            "MSG foo 1 3\r\nabc\r\n"),
        Arguments.of("foo", null, "_INBOX.x", "abc".getBytes(StandardCharsets.US_ASCII),
            "MSG foo _INBOX.x 3\r\nabc\r\n"),
        Arguments.of("foo", "1", "_INBOX.x", "abc".getBytes(StandardCharsets.US_ASCII),
            "MSG foo 1 _INBOX.x 3\r\nabc\r\n")
    );
  }

  private static final class HelperFrame extends NatsFrame {
    boolean bool(String json, String key) { return extractBoolean(json, key); }
    String string(String json, String key) { return extractString(json, key); }
    @Override public byte[] getCommand() { return "HELP".getBytes(StandardCharsets.US_ASCII); }
    @Override public boolean isValid() { return true; }
    @Override public NatsFrame instance() { return new HelperFrame(); }
  }

  private static final class ExposedErrFrame extends ErrFrame {
    void parse(String line) throws NatsProtocolException { parseLine(line); }
  }

  private static final class ExposedUnsubFrame extends UnsubFrame {
    void parse(String line) throws NatsProtocolException { parseLine(line); }
  }
}
