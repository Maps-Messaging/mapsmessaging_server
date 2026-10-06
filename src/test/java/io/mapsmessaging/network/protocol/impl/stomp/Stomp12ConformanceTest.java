/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.stomp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("conformance")
@Tag("conformance-core")
class Stomp12ConformanceTest extends StompBaseTest {

  private static final String SPEC = "STOMP Protocol Specification 1.2";
  private static final String SOURCE = ProtocolRequirement.STOMP_12_SOURCE;

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section CONNECT or STOMP Frame: servers MUST handle STOMP in the same manner as CONNECT",
      source = SOURCE)
  void stompCommandIsAcceptedAsConnect() throws Exception {
    try (RawStompConnection connection = new RawStompConnection(8674)) {
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put("accept-version", "1.2");
      headers.put("host", "localhost");
      connection.send("STOMP", headers, new byte[0], false, false);

      RawStompConnection.StompFrame connected = connection.readFrame();
      assertEquals("CONNECTED", connected.command());
      assertEquals("1.2", connected.headers().get("version"));
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section Protocol Negotiation: use the highest protocol version common to client and server",
      source = SOURCE)
  void selectsHighestMutuallySupportedVersion() throws Exception {
    try (RawStompConnection connection = new RawStompConnection(8674)) {
      connection.connect("1.0,1.1,1.2", "0,0");

      RawStompConnection.StompFrame connected = connection.readFrame();
      assertEquals("CONNECTED", connected.command());
      assertEquals("1.2", connected.headers().get("version"));
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section Protocol Negotiation: no common version MUST produce ERROR and close the connection",
      source = SOURCE)
  void unsupportedVersionReturnsErrorAndCloses() throws Exception {
    try (RawStompConnection connection = new RawStompConnection(8674)) {
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put("accept-version", "9.9");
      headers.put("host", "localhost");
      connection.send("STOMP", headers, new byte[0], false, false);

      assertEquals("ERROR", connection.readFrame().command());
      assertEquals(-1, connection.read());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section STOMP Frames: EOL is OPTIONAL CR followed by REQUIRED LF",
      source = SOURCE)
  void acceptsCrLfFrameLineEndings() throws Exception {
    try (RawStompConnection connection = new RawStompConnection(8674)) {
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put("accept-version", "1.2");
      headers.put("host", "localhost");
      connection.send("STOMP", headers, new byte[0], true, false);

      RawStompConnection.StompFrame connected = connection.readFrame();
      assertEquals("CONNECTED", connected.command());
      assertEquals("1.2", connected.headers().get("version"));
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section Header content-length: body octets MUST be read by length even when they contain NULL",
      source = SOURCE)
  void contentLengthPreservesEmbeddedNullOctets() throws Exception {
    String destination = "/topic/stomp-content-length-" + UUID.randomUUID();
    byte[] payload = new byte[] {'a', 0, 'b'};

    try (RawStompConnection subscriber = connected();
        RawStompConnection publisher = connected()) {
      subscribe(subscriber, destination, "binary-sub", "auto");

      Map<String, String> headers = new LinkedHashMap<>();
      headers.put("destination", destination);
      headers.put("receipt", "binary-sent");
      publisher.send("SEND", headers, payload, false, true);
      assertReceipt(publisher.readFrame(), "binary-sent");

      RawStompConnection.StompFrame message = subscriber.readFrame();
      assertEquals("MESSAGE", message.command());
      assertArrayEquals(payload, message.decodedBody());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section SEND: user-defined headers MUST be passed through in the MESSAGE frame",
      source = SOURCE)
  void userDefinedHeadersArePassedThrough() throws Exception {
    String destination = "/topic/stomp-user-header-" + UUID.randomUUID();

    try (RawStompConnection subscriber = connected();
        RawStompConnection publisher = connected()) {
      subscribe(subscriber, destination, "header-sub", "auto");

      Map<String, String> headers = new LinkedHashMap<>();
      headers.put("destination", destination);
      headers.put("trace-id", "trace-123");
      publisher.send(
          "SEND", headers, "payload".getBytes(StandardCharsets.UTF_8), false, true);

      RawStompConnection.StompFrame message = subscriber.readFrame();
      assertEquals("MESSAGE", message.command());
      assertEquals("trace-123", message.headers().get("trace-id"));
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section MESSAGE: destination, message-id and subscription MUST be present; acknowledged messages MUST include ack",
      source = SOURCE)
  void messageContainsRequiredHeaders() throws Exception {
    String destination = "/topic/stomp-message-headers-" + UUID.randomUUID();
    String subscriptionId = "required-header-sub";

    try (RawStompConnection subscriber = connected();
        RawStompConnection publisher = connected()) {
      subscribe(subscriber, destination, subscriptionId, "client-individual");

      Map<String, String> headers = new LinkedHashMap<>();
      headers.put("destination", destination);
      publisher.send(
          "SEND", headers, "payload".getBytes(StandardCharsets.UTF_8), false, true);

      RawStompConnection.StompFrame message = subscriber.readFrame();
      assertEquals("MESSAGE", message.command());
      assertEquals(destination, message.headers().get("destination"));
      assertEquals(subscriptionId, message.headers().get("subscription"));
      assertFalse(message.headers().getOrDefault("message-id", "").isBlank());
      assertFalse(message.headers().getOrDefault("ack", "").isBlank());
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section RECEIPT: RECEIPT MUST contain receipt-id matching the requested receipt value",
      source = SOURCE)
  void receiptEchoesRequestedIdentifier() throws Exception {
    String destination = "/topic/stomp-receipt-" + UUID.randomUUID();

    try (RawStompConnection connection = connected()) {
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put("destination", destination);
      headers.put("receipt", "receipt-123");
      connection.send(
          "SEND", headers, "payload".getBytes(StandardCharsets.UTF_8), false, true);

      assertReceipt(connection.readFrame(), "receipt-123");
    }
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section SUBSCRIBE id Header: id MUST be included and subscription creation failure MUST return ERROR then close",
      source = SOURCE)
  void subscribeWithoutIdIsRejected() throws Exception {
    String destination = "/topic/stomp-missing-id-" + UUID.randomUUID();

    try (RawStompConnection connection = connected()) {
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put("destination", destination);
      connection.send("SUBSCRIBE", headers, new byte[0], false, false);

      assertEquals("ERROR", connection.readFrame().command());
      assertEquals(-1, connection.read());
    }
  }

  private RawStompConnection connected() throws Exception {
    RawStompConnection connection = new RawStompConnection(8674);
    connection.connect("1.2", "0,0");
    assertEquals("CONNECTED", connection.readFrame().command());
    return connection;
  }

  private void subscribe(
      RawStompConnection connection,
      String destination,
      String subscriptionId,
      String acknowledgementMode) throws Exception {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("id", subscriptionId);
    headers.put("destination", destination);
    headers.put("ack", acknowledgementMode);
    headers.put("receipt", "subscribed-" + subscriptionId);
    connection.send("SUBSCRIBE", headers, new byte[0], false, false);
    assertReceipt(connection.readFrame(), "subscribed-" + subscriptionId);
  }

  private void assertReceipt(RawStompConnection.StompFrame frame, String receiptId) {
    assertEquals("RECEIPT", frame.command());
    assertEquals(receiptId, frame.headers().get("receipt-id"));
  }
}
