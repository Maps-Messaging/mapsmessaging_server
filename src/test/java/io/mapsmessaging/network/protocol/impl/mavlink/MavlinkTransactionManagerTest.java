/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.mavlink.message.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MavlinkTransactionManagerTest {

  @Test
  void singlePacketIsSentAndReportedWithoutAwaitingAcknowledgement() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submit(request("packet-1", "PACKET", packet(0)), "127.0.0.1:14550");
      assertEquals(1, sent.size());
      assertEquals("QUEUED", statuses.getFirst().get("status").getAsString());
      assertEquals("SENT", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void commandCompletesOnlyAfterMatchingAck() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submit(request("command-1", "COMMAND", packet(76)), "127.0.0.1:14550");
      assertEquals("WAITING_FOR_ACK", statuses.getLast().get("status").getAsString());

      manager.onFrame(inbound(77, 2, 1, Map.of("command", 400, "result", 0)));
      assertEquals("WAITING_FOR_ACK", statuses.getLast().get("status").getAsString());

      manager.onFrame(inbound(77, 1, 1, Map.of("command", 401, "result", 0)));
      assertEquals("WAITING_FOR_ACK", statuses.getLast().get("status").getAsString());

      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 5)));
      assertEquals("IN_PROGRESS", statuses.getLast().get("status").getAsString());

      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
      assertEquals("command-1", statuses.getLast().get("transactionId").getAsString());
    }
  }

  @Test
  void missionSendsOnlyRequestedItemsAndReportsUploadCompletion() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject count = packet(44);
      count.getAsJsonObject("payload").addProperty("count", 2);
      JsonObject item0 = packet(73);
      item0.getAsJsonObject("payload").addProperty("seq", 0);
      JsonObject item1 = packet(73);
      item1.getAsJsonObject("payload").addProperty("seq", 1);
      manager.submit(request("plan-1", "MISSION_UPLOAD", count, item0, item1),
          "127.0.0.1:14550");

      assertEquals(1, sent.size());
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 1, "mission_type", 0)));
      assertEquals(1, sent.size()); // An old upload's request must not start this upload.

      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));
      assertEquals(2, sent.size());
      assertEquals(0, sent.getLast().getAsJsonObject("payload").get("seq").getAsInt());
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 1, "mission_type", 0)));
      assertEquals(3, sent.size());
      manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 0)));

      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
      assertEquals(2, statuses.getLast().get("requestedItems").getAsInt());
      assertEquals(2, statuses.getLast().get("totalItems").getAsInt());
    }
  }

  @Test
  void cancellationDoesNotSendAnotherPacket() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submit(request("command-1", "COMMAND", packet(76)), "127.0.0.1:14550");
      JsonObject cancel = new JsonObject();
      cancel.addProperty("transactionId", "command-1");
      cancel.addProperty("operation", "CANCEL");
      manager.submit(cancel, "127.0.0.1:14550");

      assertEquals(1, sent.size());
      assertEquals("CANCELLED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void nonMatchingConcurrentTransactionIsRejected() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submit(request("command-1", "COMMAND", packet(76)), "127.0.0.1:14550");
      manager.submit(request("command-2", "COMMAND", packet(76)), "127.0.0.1:14550");

      assertEquals(1, sent.size());
      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());
      assertEquals("command-2", statuses.getLast().get("transactionId").getAsString());
      assertTrue(statuses.getLast().get("reason").getAsString().contains("active"));
    }
  }

  private static MavlinkTransactionManager manager(
      List<JsonObject> sent, List<JsonObject> statuses) {
    return new MavlinkTransactionManager(
        (message, address) -> sent.add(message.deepCopy()),
        (topic, status) -> statuses.add(status.deepCopy()),
        "/mavlink/outbound/transactions", 1, 255, 190);
  }

  private static JsonObject request(String id, String operation, JsonObject... packets) {
    JsonObject request = new JsonObject();
    request.addProperty("transactionId", id);
    request.addProperty("operation", operation);
    JsonArray messages = new JsonArray();
    for (JsonObject packet : packets) {
      messages.add(packet);
    }
    request.add("messages", messages);
    return request;
  }

  private static JsonObject packet(int messageId) {
    JsonObject packet = new JsonObject();
    JsonObject header = new JsonObject();
    header.addProperty("messageId", messageId);
    header.addProperty("systemId", 255);
    header.addProperty("componentId", 190);
    packet.add("header", header);
    JsonObject payload = new JsonObject();
    payload.addProperty("target_system", 1);
    payload.addProperty("target_component", 1);
    if (messageId == 76) {
      payload.addProperty("command", 400);
    }
    if (messageId == 44 || messageId == 73) {
      payload.addProperty("mission_type", 0);
    }
    packet.add("payload", payload);
    return packet;
  }

  private static ProcessedFrame inbound(int id, int system, int component,
      Map<String, Object> fields) {
    ProcessedFrame received = mock(ProcessedFrame.class);
    Frame frame = mock(Frame.class);
    when(received.isValid()).thenReturn(true);
    when(received.getFrame()).thenReturn(frame);
    when(received.getFields()).thenReturn(fields);
    when(frame.getMessageId()).thenReturn(id);
    when(frame.getSystemId()).thenReturn(system);
    when(frame.getComponentId()).thenReturn(component);
    return received;
  }
}
