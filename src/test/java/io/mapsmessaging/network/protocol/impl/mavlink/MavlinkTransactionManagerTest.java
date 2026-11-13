/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import io.mapsmessaging.logging.ServerLogMessages;
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

    assertEquals("WARN", ServerLogMessages.MAVLINK_TRANSACTION_RETRYING.getLevel().name());
    assertEquals("WARN", ServerLogMessages.MAVLINK_TRANSACTION_TIMED_OUT.getLevel().name());
    assertEquals("ERROR", ServerLogMessages.MAVLINK_TRANSACTION_FAILED.getLevel().name());
    assertEquals("ERROR", ServerLogMessages.MAVLINK_TRANSACTION_SEND_FAILED.getLevel().name());
  }

  @Test
  void singlePacketIsSentAndReportedWithoutAwaitingAcknowledgement() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submitNative(request("packet-1", packet(0)), "127.0.0.1:14550");
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
      manager.submitNative(request("command-1", packet(76)), "127.0.0.1:14550");
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
  void terminalStatusIncludesMatchedAcknowledgementAndLastSentFrame() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = request("audit-evidence", packet(76));
      manager.submitNative(command, "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));

      JsonObject completed = statuses.getLast();
      assertEquals("COMPLETED", completed.get("status").getAsString());
      assertEquals(76, completed.getAsJsonObject("lastSent").getAsJsonObject("header").get("messageId").getAsInt());
      JsonObject received = completed.getAsJsonObject("lastResponse");
      assertEquals(77, received.getAsJsonObject("header").get("messageId").getAsInt());
      assertEquals(400, received.getAsJsonObject("payload").get("command").getAsInt());
      assertEquals(0, received.getAsJsonObject("payload").get("result").getAsInt());
      completed.getAsJsonObject("lastResponse").addProperty("modified", true);

      manager.submitNative(command, "127.0.0.1:14550");
      JsonObject replayed = statuses.getLast().getAsJsonObject("lastResponse");
      assertEquals(77, replayed.getAsJsonObject("header").get("messageId").getAsInt());
      assertFalse(replayed.has("modified"));
      assertEquals(1, sent.size());
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
      manager.submitNative(request("plan-1", count, item0, item1),
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
      manager.submitNative(request("command-1", packet(76)), "127.0.0.1:14550");
      manager.cancel("command-1", "127.0.0.1:14550");

      assertEquals(1, sent.size());
      assertEquals("CANCELLED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void cancellationRequestStopsOnlyTheMatchingActiveTransaction() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submitNative(request("cancel-topic", packet(76)), "127.0.0.1:14550");
      JsonObject cancellation = new JsonObject();
      cancellation.addProperty("transactionId", "cancel-topic");
      manager.cancelRequest(cancellation, "/127.0.0.1:14550");

      assertEquals("CANCELLED", statuses.getLast().get("status").getAsString());
      assertEquals("cancel-topic", statuses.getLast().get("transactionId").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void malformedCancellationRequestDoesNotStopActiveTransaction() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submitNative(request("cancel-topic", packet(76)), "127.0.0.1:14550");
      JsonObject malformed = new JsonObject();
      malformed.addProperty("transactionId", "cancel-topic");
      malformed.addProperty("operation", "CANCEL");
      manager.cancelRequest(malformed, "127.0.0.1:14550");

      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void cancellationFromDifferentPeerCannotStopActiveTransaction() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submitNative(request("cancel-topic", packet(76)), "127.0.0.1:14550");
      JsonObject cancellation = new JsonObject();
      cancellation.addProperty("transactionId", "cancel-topic");
      manager.cancelRequest(cancellation, "127.0.0.2:14550");

      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void nonMatchingConcurrentTransactionIsRejected() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submitNative(request("command-1", packet(76)), "127.0.0.1:14550");
      manager.submitNative(request("command-2", packet(76)), "127.0.0.1:14550");

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
      manager.submitNative(request("command-retry", packet(76)), "127.0.0.1:14550");
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
      manager.submitNative(request("command-progress", packet(76)), "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 5)));
      triggerTimeout(manager);
      assertEquals(1, sent.size());
      assertEquals("TIMED_OUT", statuses.getLast().get("status").getAsString());
      assertTrue(statuses.getLast().get("reason").getAsString().contains("unknown"));
    }
  }

  @Test
  void lostMissionRequestTimesOutWithoutSendingAnUnrequestedItem() throws Exception {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject count = packet(44);
      count.getAsJsonObject("payload").addProperty("count", 2);
      JsonObject item0 = packet(73);
      item0.getAsJsonObject("payload").addProperty("seq", 0);
      JsonObject item1 = packet(73);
      item1.getAsJsonObject("payload").addProperty("seq", 1);
      manager.submitNative(request("mission-retry", count, item0, item1), "127.0.0.1:14550");
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));

      triggerTimeout(manager);
      assertEquals(2, sent.size());
      assertEquals("TIMED_OUT", statuses.getLast().get("status").getAsString());
      assertEquals(1, statuses.getLast().get("requestedItems").getAsInt());
      assertEquals(0, statuses.getLast().get("retryCount").getAsInt());
      assertTrue(statuses.getLast().get("reason").getAsString().contains("unknown"));

      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 1, "mission_type", 0)));
      manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 0)));
      assertEquals(2, sent.size());
      assertEquals("TIMED_OUT", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void failedStatusPublicationDoesNotPreventTransmissionOrCompletion() {
    List<JsonObject> sent = new ArrayList<>();
    MavlinkStatusPublisher failingPublisher = (topic, message) -> {
      throw new IllegalStateException("simulated status destination failure");
    };
    try (MavlinkTransactionManager manager = new MavlinkTransactionManager(
        (packet, destination) -> sent.add(packet.deepCopy()), failingPublisher, "/status", 1, 255, 190)) {
      assertDoesNotThrow(() -> manager.submitNative(request("publish-failure", packet(0)), "127.0.0.1:14550"));
      assertEquals(1, sent.size());
      assertDoesNotThrow(() -> manager.submitNative(request("next-packet", packet(0)), "127.0.0.1:14550"));
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
      manager.submitNative(request("no-topic", packet(0)), "127.0.0.1:14550");
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
      manager.submitNative(request("mission-duplicate", count, item), "127.0.0.1:14550");
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
      manager.submitNative(request("rejected", packet(76)), "127.0.0.1:14550");
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
      manager.submitNative(request("retry-limit", count, item), "127.0.0.1:14550");
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
    manager.submitNative(request("closing", packet(76)), "127.0.0.1:14550");
    manager.close();
    assertEquals("CLOSED", statuses.getLast().get("status").getAsString());
    manager.submitNative(request("after-close", packet(76)), "127.0.0.1:14550");
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
      manager.submitNative(request("bad-mission", count, invalid), "127.0.0.1:14550");
      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());

      JsonObject wrongTarget = packet(76);
      wrongTarget.getAsJsonObject("payload").addProperty("target_system", 2);
      manager.submitNative(request("bad-target", wrongTarget), "127.0.0.1:14550");
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
      manager.submitNative(request("send-failed", packet(76)), "127.0.0.1:14550");
      assertEquals("FAILED", statuses.getLast().get("status").getAsString());
      assertTrue(statuses.getLast().get("reason").getAsString().contains("simulated endpoint failure"));
    }
  }

  @Test
  void duplicateActiveEngineSubmissionDoesNotResendCommand() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = request("same-id", packet(76));
      manager.submitNative(command, "127.0.0.1:14550");
      manager.submitNative(command, "127.0.0.1:14550");
      assertEquals(1, sent.size());
      assertEquals("WAITING_FOR_ACK", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void duplicateCompletedEngineSubmissionReplaysResultWithoutResending() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = request("done-id", packet(76));
      manager.submitNative(command, "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      manager.submitNative(command, "127.0.0.1:14550");
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
      manager.submitNative(request("out-of-order", count, first, second), "127.0.0.1:14550");
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
  void lostFinalMissionAckTimesOutWithoutResendingLastItem() throws Exception {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject count = packet(44);
      count.getAsJsonObject("payload").addProperty("count", 1);
      JsonObject item = packet(73);
      item.getAsJsonObject("payload").addProperty("seq", 0);
      manager.submitNative(request("final-ack-lost", count, item), "127.0.0.1:14550");
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));

      triggerTimeout(manager);
      assertEquals(2, sent.size());
      assertEquals(73, sent.getLast().getAsJsonObject("header").get("messageId").getAsInt());
      assertEquals("TIMED_OUT", statuses.getLast().get("status").getAsString());
      assertEquals(1, statuses.getLast().get("requestedItems").getAsInt());
      assertTrue(statuses.getLast().get("reason").getAsString().contains("unknown"));

      manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 0)));
      assertEquals("TIMED_OUT", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void conflictingActiveTransactionIdIsRejectedWithoutTransmitting() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submitNative(request("collision", packet(76)), "127.0.0.1:14550");
      JsonObject other = packet(76);
      other.getAsJsonObject("payload").addProperty("command", 401);
      manager.submitNative(request("collision", other), "127.0.0.1:14550");
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
      manager.submitNative(request("collision", packet(76)), "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      JsonObject changed = packet(76);
      changed.getAsJsonObject("payload").addProperty("command", 401);
      manager.submitNative(request("collision", changed), "127.0.0.1:14550");
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
      manager.submitNative(request("peer-check", packet(76)), "127.0.0.1:14550");
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
      manager.submitNative(request("wrong-target", count, item), "127.0.0.1:14550");
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
      manager.submitNative(request("cancel-one", packet(76)), "127.0.0.1:14550");
      manager.cancel("cancel-one", "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      assertEquals("CANCELLED", statuses.getLast().get("status").getAsString());
      manager.submitNative(request("cancel-one", packet(76)), "127.0.0.1:14550");
      assertEquals("CANCELLED", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void cancelFromDifferentPeerIsRejectedAndOriginalCommandMayStillComplete() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submitNative(request("cancel-peer", packet(76)), "127.0.0.1:14550");
      manager.cancel("cancel-peer", "127.0.0.2:14550");
      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void cancellationAcceptsEquivalentPeerAddressRepresentations() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submitNative(request("cancel-normalized", packet(76)), "127.0.0.1:14550");
      manager.cancel("cancel-normalized", "/127.0.0.1:14550");
      assertEquals("CANCELLED", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void udpResponseAcceptsSocketAddressToStringFormat() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = new MavlinkTransactionManager(
        (message, destination) -> sent.add(message.deepCopy()), (topic, status) -> statuses.add(status.deepCopy()),
        "/status", 1, 255, 190, true)) {
      manager.submitNative(request("peer-format", packet(76)), "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)), "/127.0.0.1:14550");
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void malformedTransactionFieldsProduceRejectionWithoutThrowing() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject malformed = request("malformed", packet(76));
      malformed.add("transactionId", new JsonObject());

      assertDoesNotThrow(() -> manager.submitNative(malformed, "127.0.0.1:14550"));
      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());
      assertEquals(0, sent.size());

      JsonObject obsoleteOperation = request("malformed-operation", packet(76));
      obsoleteOperation.addProperty("operation", "COMMAND");
      assertDoesNotThrow(() -> manager.submitNative(obsoleteOperation, "127.0.0.1:14550"));
      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());
      assertEquals(0, sent.size());
    }
  }

  @Test
  void malformedRequestDoesNotDisruptAnotherActiveTransaction() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submitNative(request("in-flight", packet(76)), "127.0.0.1:14550");
      JsonObject malformed = request("broken", packet(76));
      malformed.add("statusTopic", new JsonObject());

      assertDoesNotThrow(() -> manager.submitNative(malformed, "127.0.0.1:14550"));
      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void firstMissionRequestLossRetriesOnlyMissionCount() throws Exception {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject count = packet(44);
      count.getAsJsonObject("payload").addProperty("count", 1);
      JsonObject item = packet(73);
      item.getAsJsonObject("payload").addProperty("seq", 0);
      manager.submitNative(request("count-retry", count, item), "127.0.0.1:14550");

      triggerTimeout(manager);
      assertEquals(2, sent.size());
      assertEquals(44, sent.getLast().getAsJsonObject("header").get("messageId").getAsInt());
      assertEquals(1, statuses.getLast().get("retryCount").getAsInt());
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));
      manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
      assertEquals(3, sent.size());
    }
  }

  @Test
  void lateTimeoutCallbackCannotTerminateCompletedTransaction() throws Exception {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submitNative(request("late-timeout", packet(76)), "127.0.0.1:14550");
      Object transaction = activeTransaction(manager);
      long generation = transactionGeneration(transaction);
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));

      invokeTimeout(manager, transaction, generation);
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void staleTimeoutGenerationCannotRetryAnActiveTransaction() throws Exception {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submitNative(request("stale-timeout", packet(76)), "127.0.0.1:14550");
      Object transaction = activeTransaction(manager);
      long oldGeneration = transactionGeneration(transaction);
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 5)));

      invokeTimeout(manager, transaction, oldGeneration);
      assertEquals("IN_PROGRESS", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void legacyMissionRequestUsesMissionItemIntAsRequiredByMavlink() {
    for (int requestMessageId : new int[]{40, 51}) {
      List<JsonObject> sent = new ArrayList<>();
      List<JsonObject> statuses = new ArrayList<>();
      try (MavlinkTransactionManager manager = manager(sent, statuses)) {
        JsonObject count = packet(44);
        count.getAsJsonObject("payload").addProperty("count", 1);
        JsonObject item = packet(73);
        item.getAsJsonObject("payload").addProperty("seq", 0);
        manager.submitNative(request("request-" + requestMessageId, count, item), "127.0.0.1:14550");

        manager.onFrame(inbound(requestMessageId, 1, 1, Map.of("seq", 0, "mission_type", 0)));
        assertEquals(2, sent.size());
        assertEquals(73, sent.getLast().getAsJsonObject("header").get("messageId").getAsInt());
        manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 0)));
        assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
      }
    }
  }

  @Test
  void completedTransactionCacheEvictsOldestEntryAfterOneHundredTwentyEightResults() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      for (int index = 0; index < 129; index++) {
        manager.submitNative(request("packet-" + index, packet(0)), "127.0.0.1:14550");
      }
      assertEquals(129, sent.size());

      manager.submitNative(request("packet-128", packet(0)), "127.0.0.1:14550");
      assertEquals(129, sent.size());
      assertEquals("SENT", statuses.getLast().get("status").getAsString());

      manager.submitNative(request("packet-0", packet(0)), "127.0.0.1:14550");
      assertEquals(130, sent.size());
      assertEquals("SENT", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void missionNegativeAcknowledgementResultsDoNotReportSuccess() {
    for (int result : new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15}) {
      List<JsonObject> sent = new ArrayList<>();
      List<JsonObject> statuses = new ArrayList<>();
      try (MavlinkTransactionManager manager = manager(sent, statuses)) {
        JsonObject count = packet(44);
        count.getAsJsonObject("payload").addProperty("count", 1);
        JsonObject item = packet(73);
        item.getAsJsonObject("payload").addProperty("seq", 0);
        manager.submitNative(request("mission-error-" + result, count, item), "127.0.0.1:14550");
        manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));
        manager.onFrame(inbound(47, 1, 1, Map.of("type", result, "mission_type", 0)));

        assertEquals("FAILED", statuses.getLast().get("status").getAsString(), "MISSION_ACK result " + result);
        assertEquals(2, sent.size());
      }
    }
  }

  @Test
  void nativeMissionItemsGenerateMissionCountAndCompleteViaVehicleRequests() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonArray items = new JsonArray();
      JsonObject item0 = packet(73);
      JsonObject item1 = packet(73);
      item0.getAsJsonObject("payload").addProperty("seq", 0);
      item1.getAsJsonObject("payload").addProperty("seq", 1);
      items.add(item0);
      items.add(item1);
      manager.submitNative(items, "127.0.0.1:14550");
      assertEquals(1, sent.size());
      assertEquals(44, sent.getFirst().getAsJsonObject("header").get("messageId").getAsInt());
      assertEquals(2, sent.getFirst().getAsJsonObject("payload").get("count").getAsInt());
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 1, "mission_type", 0)));
      manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 0)));
      assertEquals(3, sent.size());
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
      assertEquals(2, statuses.getLast().get("totalItems").getAsInt());
    }
  }

  @Test
  void nativeCommandRequiresMatchingMavlinkAckWithoutOperationSelector() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      manager.submitNative(packet(76), "127.0.0.1:14550");
      assertEquals(1, sent.size());
      assertEquals("WAITING_FOR_ACK", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void setCurrentMissionRequiresMatchingMissionCurrentAfterCommandAck() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = packet(76);
      command.getAsJsonObject("payload").addProperty("command", 224);
      command.getAsJsonObject("payload").addProperty("param1", 3);
      manager.submitNative(command, "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 224, "result", 0)));
      assertEquals("WAITING_FOR_RESPONSE", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(42, 1, 1, Map.of("seq", 2)));
      assertEquals("WAITING_FOR_RESPONSE", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(42, 1, 1, Map.of("seq", 3)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void missionCurrentBeforeAckDoesNotCompleteEarly() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = packet(76);
      command.getAsJsonObject("payload").addProperty("command", 224);
      command.getAsJsonObject("payload").addProperty("param1", 3);
      manager.submitNative(command, "127.0.0.1:14550");
      manager.onFrame(inbound(42, 1, 1, Map.of("seq", 3)));
      assertEquals("WAITING_FOR_ACK", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 224, "result", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void acceptedSetCurrentMissionTimesOutWithoutRetransmission() throws Exception {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = packet(76);
      command.getAsJsonObject("payload").addProperty("command", 224);
      command.getAsJsonObject("payload").addProperty("param1", 3);
      manager.submitNative(command, "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 224, "result", 0)));
      triggerTimeout(manager);
      assertEquals("TIMED_OUT", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void requestedMessageIsObservedAfterCommandAccepted() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = packet(76);
      command.getAsJsonObject("payload").addProperty("command", 512);
      command.getAsJsonObject("payload").addProperty("param1", 148);
      manager.submitNative(command, "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 512, "result", 0)));
      assertEquals("WAITING_FOR_RESPONSE", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(0, 1, 1, Map.of()));
      assertEquals("WAITING_FOR_RESPONSE", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(148, 1, 1, Map.of()));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void requestedTelemetryBeforeAckDoesNotCompleteEarly() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = packet(76);
      command.getAsJsonObject("payload").addProperty("command", 512);
      command.getAsJsonObject("payload").addProperty("param1", 148);
      manager.submitNative(command, "127.0.0.1:14550");
      manager.onFrame(inbound(148, 1, 1, Map.of()));
      assertEquals("WAITING_FOR_ACK", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 512, "result", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void requestedMessageTimeoutDoesNotRetransmitAcceptedCommand() throws Exception {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = packet(76);
      command.getAsJsonObject("payload").addProperty("command", 512);
      command.getAsJsonObject("payload").addProperty("param1", 148);
      manager.submitNative(command, "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 512, "result", 0)));
      triggerTimeout(manager);
      assertEquals("TIMED_OUT", statuses.getLast().get("status").getAsString());
      assertEquals(1, sent.size());
    }
  }

  @Test
  void rejectedSetCurrentMissionDoesNotWaitForMissionCurrent() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = packet(76);
      command.getAsJsonObject("payload").addProperty("command", 224);
      command.getAsJsonObject("payload").addProperty("param1", 3);
      manager.submitNative(command, "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 224, "result", 2)));
      assertEquals("FAILED", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(42, 1, 1, Map.of("seq", 3)));
      assertEquals("FAILED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void nativeMissionRejectsNonSequentialItemsBeforeSending() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonArray items = new JsonArray();
      JsonObject first = packet(73);
      JsonObject second = packet(73);
      first.getAsJsonObject("payload").addProperty("seq", 0);
      second.getAsJsonObject("payload").addProperty("seq", 2);
      items.add(first);
      items.add(second);
      manager.submitNative(items, "127.0.0.1:14550");
      assertEquals("REJECTED", statuses.getLast().get("status").getAsString());
      assertEquals(0, sent.size());
    }
  }

  @Test
  void inProgressSetCurrentMissionCompletesOnMatchingMissionCurrent() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = packet(76);
      command.getAsJsonObject("payload").addProperty("command", 224);
      command.getAsJsonObject("payload").addProperty("param1", 4);
      manager.submitNative(command, "127.0.0.1:14550");
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 224, "result", 5)));
      assertEquals("IN_PROGRESS", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(42, 1, 1, Map.of("seq", 4)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void nativeCommandIntReceivesItsMatchingAcknowledgement() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject command = packet(75);
      command.getAsJsonObject("payload").addProperty("command", 400);
      manager.submitNative(command, "127.0.0.1:14550");
      assertEquals("WAITING_FOR_ACK", statuses.getLast().get("status").getAsString());
      manager.onFrame(inbound(77, 1, 1, Map.of("command", 400, "result", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  @Test
  void nativeMissionWrapperPreservesCallerTransactionId() {
    List<JsonObject> sent = new ArrayList<>();
    List<JsonObject> statuses = new ArrayList<>();
    try (MavlinkTransactionManager manager = manager(sent, statuses)) {
      JsonObject request = new JsonObject();
      request.addProperty("transactionId", "plan-native-1");
      JsonArray messages = new JsonArray();
      JsonObject item = packet(73);
      item.getAsJsonObject("payload").addProperty("seq", 0);
      messages.add(item);
      request.add("messages", messages);
      manager.submitNative(request, "127.0.0.1:14550");
      assertEquals("plan-native-1", statuses.getLast().get("transactionId").getAsString());
      assertEquals(44, sent.getFirst().getAsJsonObject("header").get("messageId").getAsInt());
      manager.onFrame(inbound(51, 1, 1, Map.of("seq", 0, "mission_type", 0)));
      manager.onFrame(inbound(47, 1, 1, Map.of("type", 0, "mission_type", 0)));
      assertEquals("COMPLETED", statuses.getLast().get("status").getAsString());
    }
  }

  private static void triggerTimeout(MavlinkTransactionManager manager) throws Exception {
    Object transaction = activeTransaction(manager);
    invokeTimeout(manager, transaction, transactionGeneration(transaction));
  }

  private static Object activeTransaction(MavlinkTransactionManager manager) throws Exception {
    Field activeField = MavlinkTransactionManager.class.getDeclaredField("active");
    activeField.setAccessible(true);
    return activeField.get(manager);
  }

  private static long transactionGeneration(Object transaction) throws Exception {
    Field generationField = transaction.getClass().getDeclaredField("generation");
    generationField.setAccessible(true);
    return generationField.getLong(transaction);
  }

  private static void invokeTimeout(MavlinkTransactionManager manager, Object transaction, long generation) throws Exception {
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

  private static JsonObject request(String id, JsonObject... packets) {
    JsonObject request = new JsonObject();
    request.addProperty("transactionId", id);
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
