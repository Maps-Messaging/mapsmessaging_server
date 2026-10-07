/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.nats;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.test.BaseTestConfig;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("conformance")
@Tag("conformance-core")
@Tag("nats")
class NatsClientProtocolConformanceTest extends BaseTestConfig {

  private static final String SPEC = "NATS Client Protocol";
  private static final String SOURCE = ProtocolRequirement.NATS_CLIENT_PROTOCOL_SOURCE;
  private static final int PORT = 4222;

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section INFO: INFO and its JSON payload are one protocol control line terminated by CRLF",
      source = SOURCE)
  void infoIsEmittedAsSingleControlLine() throws Exception {
    try (RawNatsConnection connection = new RawNatsConnection(PORT, false)) {
      assertTrue(connection.rawInfoLine().startsWith("INFO "));
      assertTrue(connection.rawInfoLine().trim().endsWith("}"));
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section INFO: server sends INFO immediately after accepting the client TCP connection",
      source = SOURCE)
  void serverInfoAdvertisesCoreCapabilities() throws Exception {
    try (RawNatsConnection connection = new RawNatsConnection(PORT)) {
      String info = connection.info();
      assertTrue(info.startsWith("INFO "));
      assertTrue(info.contains("\"server_id\""));
      assertTrue(info.contains("\"version\""));
      assertTrue(info.contains("\"port\""));
      assertTrue(info.contains("\"headers\""));
      assertTrue(info.contains("\"max_payload\""));
      assertTrue(info.contains("\"proto\""));
    }
  }

  @Test
  @Disabled("Known NATS case-insensitive operation parsing gap: MSG-367")
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section Overview and PING/PONG: protocol operation names are case insensitive and PING is answered with PONG",
      source = SOURCE)
  void operationsAreCaseInsensitiveAndPingReturnsPong() throws Exception {
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("ping\r\n");
      assertEquals("PONG", connection.readLine());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections CONNECT and +OK/ERR: verbose=true enables +OK acknowledgements for well-formed client protocol messages",
      source = SOURCE)
  void verboseConnectProducesOkAcknowledgement() throws Exception {
    try (RawNatsConnection connection = new RawNatsConnection(PORT)) {
      connection.connect(true, true, true);
      assertEquals("+OK", connection.readLine());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections PUB, SUB and MSG: publish payload is delivered as MSG with matching subject, sid and byte count",
      source = SOURCE)
  void pubSubProducesCorrectMsgFrame() throws Exception {
    String subject = subject("basic");
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("SUB " + subject + " sid1\r\n");
      connection.send("PUB " + subject + " 5\r\nhello\r\n");

      MessageFrame message = connection.readMessage();
      assertEquals("MSG", message.command());
      assertEquals(subject, message.subject());
      assertEquals("sid1", message.sid());
      assertNull(message.replyTo());
      assertArrayEquals("hello".getBytes(StandardCharsets.US_ASCII), message.payload());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections PUB and MSG: optional reply subject is preserved in the delivered MSG frame",
      source = SOURCE)
  void replySubjectRoundTripsThroughMsg() throws Exception {
    String subject = subject("reply");
    String reply = "_INBOX." + UUID.randomUUID().toString().replace("-", "");
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("SUB " + subject + " sid2\r\n");
      connection.send("PUB " + subject + " " + reply + " 2\r\nok\r\n");

      MessageFrame message = connection.readMessage();
      assertEquals(reply, message.replyTo());
      assertArrayEquals("ok".getBytes(StandardCharsets.US_ASCII), message.payload());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section PUB: payload is optional; byte count 0 still requires the terminating CRLF",
      source = SOURCE)
  void zeroLengthPublishIsDelivered() throws Exception {
    String subject = subject("zero");
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("SUB " + subject + " sid3\r\n");
      connection.send("PUB " + subject + " 0\r\n\r\n");

      MessageFrame message = connection.readMessage();
      assertEquals(0, message.payload().length);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section UNSUB: UNSUB <sid> removes the subscription immediately",
      source = SOURCE)
  void immediateUnsubscribeStopsDelivery() throws Exception {
    String subject = subject("unsub");
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("SUB " + subject + " sid4\r\n");
      connection.send("UNSUB sid4\r\n");
      connection.send("PUB " + subject + " 1\r\nx\r\n");
      connection.send("PING\r\n");

      assertEquals("PONG", connection.readLine());
    }
  }

  @Test
  @Disabled("Known NATS UNSUB max_msgs handling gap: MSG-367")
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section UNSUB: optional max_msgs automatically removes the subscription after the requested number of messages",
      source = SOURCE)
  void autoUnsubscribeHonoursMaxMessages() throws Exception {
    String subject = subject("max");
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("SUB " + subject + " sid5\r\n");
      connection.send("UNSUB sid5 2\r\n");
      for (int i = 0; i < 3; i++) {
        connection.send("PUB " + subject + " 1\r\n" + i + "\r\n");
      }
      connection.send("PING\r\n");

      List<MessageFrame> messages = new ArrayList<>();
      String line;
      while (!(line = connection.readLine()).equals("PONG")) {
        messages.add(connection.readMessageFromHeader(line));
      }
      assertEquals(2, messages.size());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section CONNECT echo: echo=false prevents messages published by a connection from being sent to that connection's own subscriptions",
      source = SOURCE)
  void echoFalseSuppressesOwnPublishedMessages() throws Exception {
    String subject = subject("echo");
    try (RawNatsConnection connection = connected(false, false, true)) {
      connection.send("SUB " + subject + " sid6\r\n");
      connection.send("PUB " + subject + " 1\r\nx\r\n");
      connection.send("PING\r\n");

      assertEquals("PONG", connection.readLine());
    }
  }

  @Test
  @Disabled("Known NATS repeated-header preservation gap: MSG-367")
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections HPUB and HMSG: header length and total length delimit headers/payload and header name case is preserved",
      source = SOURCE)
  void hpubProducesHmsgAndPreservesHeaderCase() throws Exception {
    String subject = subject("headers");
    byte[] header =
        ("NATS/1.0\r\nX-Custom-Case: Value\r\nX-Multi: one\r\nX-Multi: two\r\n\r\n")
            .getBytes(StandardCharsets.US_ASCII);
    byte[] payload = "body".getBytes(StandardCharsets.US_ASCII);

    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("SUB " + subject + " sid7\r\n");
      connection.send(
          "HPUB " + subject + " " + header.length + " " + (header.length + payload.length) + "\r\n");
      connection.sendBytes(header);
      connection.sendBytes(payload);
      connection.send("\r\n");

      HMessageFrame message = connection.readHeaderMessage();
      assertEquals(subject, message.subject());
      assertEquals("sid7", message.sid());
      String receivedHeaders = new String(message.headers(), StandardCharsets.US_ASCII);
      assertTrue(receivedHeaders.contains("X-Custom-Case: Value"));
      assertTrue(receivedHeaders.contains("X-Multi: one"));
      assertTrue(receivedHeaders.contains("X-Multi: two"));
      assertArrayEquals(payload, message.payload());
    }
  }

  @Test
  @Disabled("Known NATS core inbox subscription routing gap: MSG-367")
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section SUB: any valid subject, including an exact _INBOX subject, can be used as a core subscription subject",
      source = SOURCE)
  void exactInboxSubjectCanBeUsedAsCoreSubscription() throws Exception {
    String inbox = "_INBOX." + UUID.randomUUID().toString().replace("-", "");
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("SUB " + inbox + " inbox-sid\r\n");
      connection.send("PUB " + inbox + " 1\r\nx\r\n");
      MessageFrame message = connection.readMessage();
      assertEquals(inbox, message.subject());
      assertEquals("inbox-sid", message.sid());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section SUB and subject wildcards: '*' matches exactly one subject token",
      source = SOURCE)
  void singleTokenWildcardSubscriptionReceivesMatchingSubject() throws Exception {
    String root = subject("wildcard");
    String filter = root + ".*";
    String actual = root + ".value";
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("SUB " + filter + " sid-wild\r\n");
      connection.send("PUB " + actual + " 1\r\nx\r\n");

      MessageFrame message = connection.readMessage();
      assertEquals(actual, message.subject());
      assertEquals("sid-wild", message.sid());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section SUB queue group: subscribers in the same queue group load-balance each message to one group member",
      source = SOURCE)
  void queueGroupDeliversEachMessageToOnlyOneMember() throws Exception {
    String subject = subject("queue");
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("SUB " + subject + " workers sid-a\r\n");
      connection.send("SUB " + subject + " workers sid-b\r\n");
      connection.send("PUB " + subject + " 1\r\nx\r\n");
      connection.send("PING\r\n");

      MessageFrame message = connection.readMessage();
      assertTrue(message.sid().equals("sid-a") || message.sid().equals("sid-b"));
      assertEquals("PONG", connection.readLine());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections CONNECT and +OK/ERR: verbose=true causes well-formed client operations to receive +OK",
      source = SOURCE)
  void verboseModeAcknowledgesSubAndPubOperations() throws Exception {
    String subject = subject("verbose");
    try (RawNatsConnection connection = connected(true, true, true)) {
      connection.send("SUB " + subject + " sid-ok\r\n");
      assertEquals("+OK", connection.readLine());

      connection.send("PUB " + subject + " 1\r\nx\r\n");

      boolean sawOk = false;
      MessageFrame message = null;
      for (int i = 0; i < 2; i++) {
        String line = connection.readLine();
        if ("+OK".equals(line)) {
          sawOk = true;
        } else {
          message = connection.readMessageFromHeader(line);
        }
      }

      assertTrue(sawOk);
      assertNotNull(message);
      assertEquals("sid-ok", message.sid());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section protocol framing: multiple complete operations may be coalesced in one TCP write",
      source = SOURCE)
  void coalescedOperationsAreParsedIndependently() throws Exception {
    String subject = subject("coalesced");
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send(
          "SUB " + subject + " sid8\r\n"
              + "PUB " + subject + " 3\r\none\r\n"
              + "PING\r\n");

      MessageFrame message = connection.readMessage();
      assertArrayEquals("one".getBytes(StandardCharsets.US_ASCII), message.payload());
      assertEquals("PONG", connection.readLine());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section protocol framing: protocol operations are stream-oriented and may arrive split across TCP reads",
      source = SOURCE)
  void splitOperationBoundaryIsAccepted() throws Exception {
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.sendBytes("PI".getBytes(StandardCharsets.US_ASCII));
      connection.sendBytes("NG\r".getBytes(StandardCharsets.US_ASCII));
      connection.sendBytes("\n".getBytes(StandardCharsets.US_ASCII));
      assertEquals("PONG", connection.readLine());
    }
  }

  @Test
  @Disabled("Known NATS client/server operation-direction gap: MSG-367")
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section Protocol Messages: MSG is a server-to-client operation and is not a valid client-to-server command",
      source = SOURCE)
  void clientCannotSendServerOnlyMsgOperation() throws Exception {
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("MSG illegal sid 1\r\nx\r\n");
      String error = connection.readLine();
      assertNotNull(error);
      assertTrue(error.startsWith("-ERR"));
      assertTrue(connection.awaitClose());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section +OK/ERR Errors: Unknown Protocol Operation is non-recoverable and invalidates the connection",
      source = SOURCE)
  void unknownOperationReturnsErrAndCloses() throws Exception {
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("NOT_A_NATS_OPERATION\r\n");
      String error = connection.readLine();
      assertTrue(error.startsWith("-ERR"));
      assertTrue(error.toLowerCase().contains("unknown") || error.toLowerCase().contains("protocol"));
      assertTrue(connection.awaitClose());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section +OK/ERR Errors: Invalid Subject is recoverable",
      source = SOURCE)
  void invalidSubjectReturnsRecoverableError() throws Exception {
    try (RawNatsConnection connection = connected(false, true, true)) {
      connection.send("SUB foo. sid9\r\n");
      String error = connection.readLine();
      assertTrue(error.startsWith("-ERR"));
      connection.send("PING\r\n");
      assertEquals("PONG", connection.readLine());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections INFO and +OK/ERR Errors: publishing a declared payload larger than INFO max_payload is a non-recoverable Maximum Payload Violation",
      source = SOURCE)
  void declaredPayloadAboveMaxPayloadIsRejectedAndClosed() throws Exception {
    try (RawNatsConnection connection = connected(false, true, true)) {
      long maxPayload = jsonLong(connection.info(), "max_payload");
      int declaredSize = Math.toIntExact(maxPayload + 1);
      connection.send("PUB " + subject("oversize") + " " + declaredSize + "\r\n");
      connection.sendBytes(new byte[declaredSize]);
      connection.send("\r\n");
      String error = connection.readLine();
      assertTrue(error.startsWith("-ERR"));
      assertTrue(error.toLowerCase().contains("payload"));
      assertTrue(connection.awaitClose());
    }
  }

  private RawNatsConnection connected(boolean verbose, boolean echo, boolean headers)
      throws Exception {
    RawNatsConnection connection = new RawNatsConnection(PORT);
    connection.connect(verbose, echo, headers);
    if (verbose) {
      assertEquals("+OK", connection.readLine());
    }
    return connection;
  }

  private static String subject(String suffix) {
    return "conformance." + suffix + "." + UUID.randomUUID().toString().replace("-", "");
  }

  private static long jsonLong(String line, String field) {
    Matcher matcher = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*(\\d+)").matcher(line);
    assertTrue(matcher.find(), "Missing INFO field " + field + " in " + line);
    return Long.parseLong(matcher.group(1));
  }

  private record MessageFrame(
      String command, String subject, String sid, String replyTo, byte[] payload) {}

  private record HMessageFrame(
      String subject, String sid, String replyTo, byte[] headers, byte[] payload) {}

  private static final class RawNatsConnection implements AutoCloseable {
    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private final String rawInfoLine;
    private final String info;

    RawNatsConnection(int port) throws IOException {
      this(port, true);
    }

    RawNatsConnection(int port, boolean tolerateMultilineInfo) throws IOException {
      socket = new Socket("127.0.0.1", port);
      socket.setSoTimeout(3_000);
      input = socket.getInputStream();
      output = socket.getOutputStream();
      rawInfoLine = readLine();
      if (rawInfoLine == null || !rawInfoLine.startsWith("INFO ")) {
        throw new IOException("Expected initial INFO, received " + rawInfoLine);
      }
      info = tolerateMultilineInfo ? consumeKnownMultilineInfo(rawInfoLine) : rawInfoLine;
    }

    String rawInfoLine() {
      return rawInfoLine;
    }

    String info() {
      return info;
    }

    private String consumeKnownMultilineInfo(String firstLine) throws IOException {
      if (firstLine.trim().endsWith("}")) {
        return firstLine;
      }
      StringBuilder combined = new StringBuilder(firstLine);
      while (true) {
        String next = readLine();
        if (next == null) {
          throw new EOFException("Connection closed during multiline INFO");
        }
        combined.append(next.trim());
        if (next.trim().equals("}")) {
          return combined.toString();
        }
      }
    }

    void connect(boolean verbose, boolean echo, boolean headers) throws IOException {
      send(
          "CONNECT {\"verbose\":" + verbose
              + ",\"pedantic\":true,\"tls_required\":false"
              + ",\"lang\":\"java-test\",\"version\":\"1\",\"protocol\":1"
              + ",\"echo\":" + echo + ",\"headers\":" + headers + "}\r\n");
    }

    void send(String value) throws IOException {
      sendBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    void sendBytes(byte[] value) throws IOException {
      output.write(value);
      output.flush();
    }

    String readLine() throws IOException {
      ByteArrayOutputStream line = new ByteArrayOutputStream();
      while (true) {
        int value = input.read();
        if (value < 0) {
          if (line.size() == 0) {
            return null;
          }
          throw new EOFException("Connection closed during NATS control line");
        }
        if (value == '\n') {
          byte[] bytes = line.toByteArray();
          int length = bytes.length;
          if (length > 0 && bytes[length - 1] == '\r') {
            length--;
          }
          return new String(bytes, 0, length, StandardCharsets.US_ASCII);
        }
        line.write(value);
      }
    }

    MessageFrame readMessage() throws IOException {
      return readMessageFromHeader(readLine());
    }

    MessageFrame readMessageFromHeader(String line) throws IOException {
      String[] parts = line.trim().split("\\s+");
      if (parts.length != 4 && parts.length != 5) {
        throw new IOException("Invalid MSG frame " + line);
      }
      assertEquals("MSG", parts[0]);
      String reply = parts.length == 5 ? parts[3] : null;
      int length = Integer.parseInt(parts[parts.length - 1]);
      byte[] payload = readExact(length);
      consumeCrlf();
      return new MessageFrame(parts[0], parts[1], parts[2], reply, payload);
    }

    HMessageFrame readHeaderMessage() throws IOException {
      String line = readLine();
      String[] parts = line.trim().split("\\s+");
      if (parts.length != 5 && parts.length != 6) {
        throw new IOException("Invalid HMSG frame " + line);
      }
      assertEquals("HMSG", parts[0]);
      String reply = parts.length == 6 ? parts[3] : null;
      int headerIndex = parts.length == 6 ? 4 : 3;
      int headerLength = Integer.parseInt(parts[headerIndex]);
      int totalLength = Integer.parseInt(parts[headerIndex + 1]);
      byte[] content = readExact(totalLength);
      consumeCrlf();
      byte[] headers = java.util.Arrays.copyOfRange(content, 0, headerLength);
      byte[] payload = java.util.Arrays.copyOfRange(content, headerLength, totalLength);
      return new HMessageFrame(parts[1], parts[2], reply, headers, payload);
    }

    boolean awaitClose() throws IOException {
      socket.setSoTimeout(1_000);
      try {
        return input.read() < 0;
      } catch (SocketTimeoutException timeout) {
        return false;
      } catch (IOException closed) {
        return true;
      }
    }

    private byte[] readExact(int length) throws IOException {
      byte[] result = input.readNBytes(length);
      if (result.length != length) {
        throw new EOFException("Expected " + length + " NATS payload bytes, received " + result.length);
      }
      return result;
    }

    private void consumeCrlf() throws IOException {
      if (input.read() != '\r' || input.read() != '\n') {
        throw new IOException("NATS payload missing CRLF terminator");
      }
    }

    @Override
    public void close() throws IOException {
      socket.close();
    }
  }
}
