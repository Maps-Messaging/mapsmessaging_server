/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.logging.LEVEL;
import com.google.gson.JsonObject;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.mavlink.message.Frame;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MavlinkTransactionManagerTest {

  @Test
  void transactionDiagnosticsHaveCentralizedSeverityAndCorrectParameterCounts() {
    assertEquals(4, ServerLogMessages.MAVLINK_TRANSACTION_REJECTED.getParameterCount());
    assertEquals(5, ServerLogMessages.MAVLINK_TRANSACTION_RETRYING.getParameterCount());
    assertEquals(3, ServerLogMessages.MAVLINK_TRANSACTION_SEND_FAILED.getParameterCount());
    assertEquals(4, ServerLogMessages.MAVLINK_TRANSACTION_FAILED.getParameterCount());
    assertEquals(6, ServerLogMessages.MAVLINK_TRANSACTION_TIMED_OUT.getParameterCount());
    assertEquals(3, ServerLogMessages.MAVLINK_TRANSACTION_CANCELLED.getParameterCount());
    assertEquals(3, ServerLogMessages.MAVLINK_TRANSACTION_CONNECTION_CLOSED.getParameterCount());
    assertEquals(1, ServerLogMessages.MAVLINK_TRANSACTION_STATUS_PUBLISH_FAILED.getParameterCount());

    assertEquals(LEVEL.WARN, ServerLogMessages.MAVLINK_TRANSACTION_RETRYING.getLevel());
    assertEquals(LEVEL.WARN, ServerLogMessages.MAVLINK_TRANSACTION_TIMED_OUT.getLevel());
    assertEquals(LEVEL.ERROR, ServerLogMessages.MAVLINK_TRANSACTION_FAILED.getLevel());
    assertEquals(LEVEL.ERROR, ServerLogMessages.MAVLINK_TRANSACTION_SEND_FAILED.getLevel());
  }

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

  @Test
  void commandAckLossRetriesCommandLongWithIncreasingConfirmationAndBoundedAttempts() throws Exception {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submit(request("command-retry", "COMMAND", packet(76)), "127.0.0.1:14550");
      for (int attempt = 1; attempt <= MavlinkTransactionManager.DEFAULT_MAX_RETRIES; attempt++) {
        triggerTimeout(manager);
        assertEquals(attempt, sent.getLast().getAsJsonObject("payload").get("confirmation").getAsInt());
      }
      triggerTimeout(manager);
      assertEquals(MavlinkTransactionManager.DEFAULT_MAX_RETRIES + 1, sent.size());
      assertEquals("TIMED_OUT", statuses.getLast().get("status").getAsString());
      assertEquals(MavlinkTransactionManager.DEFAULT_MAX_RETRIES, statuses.getLast().get("retryCount").getAsInt());
    }
  }

  @Test
  void inProgressCommandIsNeverRetransmittedWhenProgressStops() throws Exception {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submit(request("command-progress", "COMMAND", packet(76)), "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 5)));
      triggerTimeout(manager);
      assertEquals(1, sent.size());
      assertEquals("TIMED_OUT", statuses.getLast().get("status").getAsString());
      assertTrue(statuses.getLast().get("reason").getAsString().contains("unknown"));
    }
  }

  @Test
  void lostMissionRequestRetriesLastItemAndKeepsTransferProgress() throws Exception {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject count = packet(44);
      count.getAsJsonObject("payload").addProperty("count", 2);
      JsonObject item0 = packet(73);
      item0.getAsJsonObject("payload").addProperty("seq", 0);
      JsonObject item1 = packet(73);
      item1.getAsJsonObject("payload").addProperty("seq", 1);
      manager.submit(request("mission-retry", "MISSION_UPLOAD", count, item0, item1), "127.0.0.1:14550");
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));
      triggerTimeout(manager);
      assertEquals(3, sent.size());
      assertEquals(73, sent.getLast().getAsJsonObject("header").get("messageId").getAsInt());
      assertEquals(0, sent.getLast().getAsJsonObject("payload").get("seq").getAsInt());
      assertEquals(1, statuses.getLast().get("requestedItems").getAsInt());

      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 1, "mission_type", 0)));
      manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void failedStatusPublicationDoesNotPreventTransmissionOrCompletion() {
    List<JsonObject> sent = new ArrayList<>();
    MavlinkTransactionManager.StatusPublisher failingPublisher = (topic, message) -> {
      throw new IllegalStateException("simulated status destination failure");
    };
    try (MavlinkTransactionManager manager = new MavlinkTransactionManager(
        (packet, destination) -> sent.add(packet.deepCopy()), failingPublisher, "/status", 1, 255, 190)) {
      assertDoesNotThrow(() -> manager.submit(request("publish-failure", "PACKET", packet(0)), "127.0.0.1:14550"));
      assertEquals(1, sent.size());
      assertDoesNotThrow(() -> manager.submit(request("next-packet", "PACKET", packet(0)), "127.0.0.1:14550"));
      assertEquals(2, sent.size());
    }
  }

  @Test
  void missingStatusTopicDoesNotChangeTransactionOutcome() {
    List<JsonObject> sent = new ArrayList<>();
    try (MavlinkTransactionManager manager = new MavlinkTransactionManager(
        (packet, destination) -> sent.add(packet.deepCopy()), (topic, status) -> {
          throw new AssertionError("No topic: status publisher should not be called");
        }, null, 1, 255, 190)) {
      manager.submit(request("no-topic", "PACKET", packet(0)), "127.0.0.1:14550");
      assertEquals(1, sent.size());
    }
  }

  @Test
  void duplicateRequestedMissionItemIsTransmittedAgainWithoutDoubleCountingProgress() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject count = packet(44);
      count.getAsJsonObject("payload").addProperty("count", 1);
      JsonObject item = packet(73);
      item.getAsJsonObject("payload").addProperty("seq", 0);
      manager.submit(request("mission-duplicate", "MISSION_UPLOAD", count, item), "127.0.0.1:14550");
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));
      assertEquals(3, sent.size());
      assertEquals(1, statuses.getLast().get("requestedItems").getAsInt());
      manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void rejectedCommandAckFailsWithoutRetry() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submit(request("rejected", "COMMAND", packet(76)), "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 2)));
      assertEquals("FAILED", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void missionRetryStopsAtConfiguredLimit() throws Exception {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject count = packet(44);
      count.getAsJsonObject("payload").addProperty("count", 1);
      JsonObject item = packet(73);
      item.getAsJsonObject("payload").addProperty("seq", 0);
      manager.submit(request("retry-limit", "MISSION_UPLOAD", count, item), "127.0.0.1:14550");
      for (int attempt = 0; attempt <= MavlinkTransactionManager.DEFAULT_MAX_RETRIES; attempt++) {
        triggerTimeout(manager);
      }
      assertEquals(MavlinkTransactionManager.DEFAULT_MAX_RETRIES + 1, sent.size());
      assertEquals("TIMED_OUT", statuses.getLast().get("status").getAsString());
      assertEquals(MavlinkTransactionManager.DEFAULT_MAX_RETRIES, statuses.getLast().get("retryCount").getAsInt());
    }
  }

  @Test
  void closingTransactionReportsClosedAndRejectsFurtherCommands() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    MavlinkTransactionManager manager = manager(sent, statuses);
    manager.submit(request("closing", "COMMAND", packet(76)), "127.0.0.1:14550");
    manager.close();
    assertEquals("CLOSED", statuses.getLast().get("status").getAsString());
    manager.submit(request("after-close", "COMMAND", packet(76)), "127.0.0.1:14550");
    assertEquals("REJECTED", statuses.getLast().get("status").getAsString());
    assertEquals(1, sent.size());
  }

  @Test
  void invalidMissionItemAndWrongTargetAreRejectedBeforeSending() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject count = packet(44);
      count.getAsJsonObject("payload").addProperty("count", 1);
      JsonObject invalid = packet(73);
      invalid.getAsJsonObject("payload").addProperty("seq", 4);
      manager.submit(request("bad-mission", "MISSION_UPLOAD", count, invalid), "127.0.0.1:14550");
      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());

      JsonObject wrongTarget = packet(76);
      wrongTarget.getAsJsonObject("payload").addProperty("target_system", 2);
      manager.submit(request("bad-target", "COMMAND", wrongTarget), "127.0.0.1:14550");
      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());
      assertEquals(0, sent.size());
    }
  }

  @Test
  void wireSendExceptionFailsTransactionWithoutMaskingFailure() {
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = new MavlinkTransactionManager(
        (message, destination) -> { throw new java.io.IOException("simulated endpoint failure"); },
        (topic, status) -> statuses.add(status.deepCopy()), "/status", 1, 255, 190)) {
      manager.submit(request("send-failed", "COMMAND", packet(76)), "127.0.0.1:14550");
      assertEquals("FAILED", statuses.getLast().get("status").getAsString());
      assertTrue(statuses.getLast().get("reason").getAsString().contains("simulated endpoint failure"));
    }
  }

  @Test
  void duplicateActiveEngineSubmissionDoesNotResendCommand() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = request("same-id", "COMMAND", packet(76));
      manager.submit(command, "127.0.0.1:14550");
      manager.submit(command, "127.0.0.1:14550");
      assertEquals(1, sent.size());
      assertEquals("WAITING_FOR_ACK", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void duplicateCompletedEngineSubmissionReplaysResultWithoutResending() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = request("done-id", "COMMAND", packet(76));
      manager.submit(command, "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      manager.submit(command, "127.0.0.1:14550");
      assertEquals(1, sent.size());
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void outOfRangeAndOutOfOrderRequestsDoNotFailRecoverableUpload() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject count = packet(44);
      count.getAsJsonObject("payload").addProperty("count", 2);
      JsonObject first = packet(73);
      first.getAsJsonObject("payload").addProperty("seq", 0);
      JsonObject second = packet(73);
      second.getAsJsonObject("payload").addProperty("seq", 1);
      manager.submit(request("out-of-order", "MISSION_UPLOAD", count, first, second), "127.0.0.1:14550");
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 17, "mission_type", 0)));
      manager.onFrame(inbound(47, 1, 1, Map.of("type", 13, "mission_type", 0)));
      assertEquals(1, sent.size());
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 1, "mission_type", 0)));
      manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void finalMissionAckLossResendsLastRequestedItem() throws Exception {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject count = packet(44);
      count.getAsJsonObject("payload").addProperty("count", 1);
      JsonObject item = packet(73);
      item.getAsJsonObject("payload").addProperty("seq", 0);
      manager.submit(request("final-ack-lost", "MISSION_UPLOAD", count, item), "127.0.0.1:14550");
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));
      triggerTimeout(manager);
      assertEquals(3, sent.size());
      assertEquals(73, sent.getLast().getAsJsonObject("header").get("messageId").getAsInt());
      manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void conflictingActiveTransactionIdIsRejectedWithoutTransmitting() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submit(request("collision", "COMMAND", packet(76)), "127.0.0.1:14550");
      JsonObject other = packet(76);
      other.getAsJsonObject("payload").addProperty("command", 401);
      manager.submit(request("collision", "COMMAND", other), "127.0.0.1:14550");
      assertEquals(1, sent.size());
      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());
      assertTrue(statuses.getLast().get("reason").getAsString().contains("different request"));
    }
  }

  @Test
  void conflictingCompletedTransactionIdIsRejectedWithoutTransmitting() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submit(request("collision", "COMMAND", packet(76)), "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      JsonObject changed = packet(76);
      changed.getAsJsonObject("payload").addProperty("command", 401);
      manager.submit(request("collision", "COMMAND", changed), "127.0.0.1:14550");
      assertEquals(1, sent.size());
      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void udpResponseMustMatchOriginalPeerEvenWithMatchingSystemIds() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = new MavlinkTransactionManager(
        (message, destination) -> sent.add(message.deepCopy()), (topic, status) -> statuses.add(status.deepCopy()),
        "/status", 1, 255, 190, true)) {
      manager.submit(request("peer-check", "COMMAND", packet(76)), "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)), "127.0.0.2:14550");
      assertEquals("WAITING_FOR_ACK", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)), "127.0.0.1:14550");
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void invalidAckTargetAndMissionTypeCannotCompleteUpload() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject count = packet(44);
      count.getAsJsonObject("payload").addProperty("count", 1);
      JsonObject item = packet(73);
      item.getAsJsonObject("payload").addProperty("seq", 0);
      manager.submit(request("wrong-target", "MISSION_UPLOAD", count, item), "127.0.0.1:14550");
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "target_system", 17, "mission_type", 0)));
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 1)));
      assertEquals(1, sent.size());
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));
      manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 1)));
      assertEquals("UPLOADING", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void cancelledTransactionIgnoresLateAckAndDoesNotCompleteNewCommand() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submit(request("cancel-one", "COMMAND", packet(76)), "127.0.0.1:14550");
      JsonObject cancel = new JsonObject();
      cancel.addProperty("transactionId", "cancel-one");
      cancel.addProperty("operation", "CANCEL");
      manager.submit(cancel, "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      assertEquals("CANCELLED", statuses.getLast().get("status").getAsString());
      manager.submit(request("cancel-one", "COMMAND", packet(76)), "127.0.0.1:14550");
      assertEquals("CANCELLED", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  private static void triggerTimeout(MavlinkTransactionManager manager) throws Exception {
    Field activeField = MavlinkTransactionManager.class.getDeclaredField("active");
    activeField.setAccessible(true);
    Object transaction = activeField.get(manager);
    Field generationField = transaction.getClass().getDeclaredField("generation");
    generationField.setAccessible(true);
    long generation = generationField.getLong(transaction);
    Method timeoutMethod = MavlinkTransactionManager.class.getDeclaredMethod("timeout", transaction.getClass(), long.class);
    timeoutMethod.setAccessible(true);
    timeoutMethod.invoke(manager, transaction, generation);
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
