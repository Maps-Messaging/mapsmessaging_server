/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 */

package io.mapsmessaging.network.protocol.impl.stomp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StompWireComplianceTest extends StompBaseTest {

  @Test
  void malformedFrameReturnsErrorBeforeClosingConnection() throws Exception {
    try (RawStompConnection connection = new RawStompConnection(8674)) {
      connection.connect("1.2", "0,0");
      assertEquals("CONNECTED", connection.readFrame().command());

      connection.sendBytes("NOT-A-STOMP-COMMAND\n\n\0".getBytes(StandardCharsets.UTF_8));

      RawStompConnection.StompFrame error = connection.readFrame();
      assertEquals("ERROR", error.command());
      assertEquals(
          Integer.toString(error.body().length), error.headers().get("content-length"));
      assertFalse(error.bodyText().isBlank());
      assertEquals(-1, connection.read());
    }
  }

  @Test
  void acceptsCrLfConnectAndPublishFrames() throws Exception {
    String destination = "/topic/stomp-crlf-" + UUID.randomUUID();
    try (RawStompConnection subscriber = new RawStompConnection(8674);
        RawStompConnection publisher = new RawStompConnection(8674)) {
      subscriber.connect("1.2", "0,0");
      assertEquals("CONNECTED", subscriber.readFrame().command());
      subscribe(subscriber, destination, "crlf-sub", "auto");

      Map<String, String> connectHeaders = new LinkedHashMap<>();
      connectHeaders.put("accept-version", "1.2");
      connectHeaders.put("host", "localhost");
      connectHeaders.put("heart-beat", "0,0");
      publisher.send("STOMP", connectHeaders, new byte[0], true, false);
      assertEquals("CONNECTED", publisher.readFrame().command());

      Map<String, String> sendHeaders = new LinkedHashMap<>();
      sendHeaders.put("destination", destination);
      publisher.send(
          "SEND",
          sendHeaders,
          "crlf-payload".getBytes(StandardCharsets.UTF_8),
          true,
          true);

      RawStompConnection.StompFrame message = subscriber.readFrame();
      assertEquals("MESSAGE", message.command());
      assertEquals("crlf-payload", message.bodyText());
    }
  }

  @Test
  void stomp12AckIdRoundTripsThroughTheServer() throws Exception {
    String destination = "/topic/stomp-ack-" + UUID.randomUUID();
    try (RawStompConnection subscriber = new RawStompConnection(8674);
        RawStompConnection publisher = new RawStompConnection(8674)) {
      subscriber.connect("1.2", "0,0");
      assertEquals("CONNECTED", subscriber.readFrame().command());
      subscribe(subscriber, destination, "ack-sub", "client-individual");

      publisher.connect("1.2", "0,0");
      assertEquals("CONNECTED", publisher.readFrame().command());
      Map<String, String> sendHeaders = new LinkedHashMap<>();
      sendHeaders.put("destination", destination);
      sendHeaders.put("receipt", "published");
      publisher.send(
          "SEND",
          sendHeaders,
          "ack-payload".getBytes(StandardCharsets.UTF_8),
          false,
          true);
      assertReceipt(publisher.readFrame(), "published");

      RawStompConnection.StompFrame message = subscriber.readFrame();
      assertEquals("MESSAGE", message.command());
      String acknowledgementId = message.headers().get("ack");
      assertTrue(acknowledgementId != null && !acknowledgementId.isBlank());

      Map<String, String> ackHeaders = new LinkedHashMap<>();
      ackHeaders.put("id", acknowledgementId);
      ackHeaders.put("receipt", "acknowledged");
      subscriber.send("ACK", ackHeaders, new byte[0], false, false);
      assertReceipt(subscriber.readFrame(), "acknowledged");
    }
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
