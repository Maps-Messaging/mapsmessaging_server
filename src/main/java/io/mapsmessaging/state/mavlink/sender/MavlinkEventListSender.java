/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.state.mavlink.sender;

import static io.mapsmessaging.state.logging.StateLogMessages.*;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.sender.MavlinkAcknowledgementHandler.Acknowledgement;
import io.mapsmessaging.state.mavlink.sender.MavlinkAcknowledgementHandler.Action;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;

@Getter
public class MavlinkEventListSender implements AutoCloseable {

  public static final int DEFAULT_MAX_RETRIES = 3;
  public static final long DEFAULT_ACKNOWLEDGEMENT_TIMEOUT_MILLIS = 2_000L;

  private static final Logger logger = LoggerFactory.getLogger(MavlinkEventListSender.class);

  private final Object lock;
  private final Object inboundLock;
  private final UUID sequenceId;
  private final UxvModelCommandSet commandSet;
  private final List<MavlinkMessage> messages;
  private final MavlinkEventSender sender;
  private final MavlinkAcknowledgementHandler acknowledgementHandler;
  private final MavlinkSendCompletionHandler completionHandler;
  private final int maxRetries;
  private final long acknowledgementTimeoutMillis;
  private final AtomicBoolean isActive = new AtomicBoolean(true);

  private boolean started;
  private boolean terminal;
  private int nextIndex;
  private int waitingIndex;
  private int retryCount;
  private long acknowledgementTimeoutGeneration;
  private MavlinkMessage waitingMessage;
  private MavlinkMessage lastSentMessage;
  private MavlinkPacket lastReceivedMessage;
  private ScheduledFuture<?> acknowledgementTimeoutFuture;

  public MavlinkEventListSender(UxvModelCommandSet commandSet, MavlinkEventSender sender, MavlinkAcknowledgementHandler acknowledgementHandler, MavlinkSendCompletionHandler completionHandler) {
    this(commandSet, sender, acknowledgementHandler, completionHandler, DEFAULT_MAX_RETRIES, DEFAULT_ACKNOWLEDGEMENT_TIMEOUT_MILLIS);
  }

  public MavlinkEventListSender(
      UxvModelCommandSet commandSet,
      MavlinkEventSender sender,
      MavlinkAcknowledgementHandler acknowledgementHandler,
      MavlinkSendCompletionHandler completionHandler,
      int maxRetries) {
    this(commandSet, sender, acknowledgementHandler, completionHandler, maxRetries, DEFAULT_ACKNOWLEDGEMENT_TIMEOUT_MILLIS);
  }

  public MavlinkEventListSender(
      UxvModelCommandSet commandSet,
      MavlinkEventSender sender,
      MavlinkAcknowledgementHandler acknowledgementHandler,
      MavlinkSendCompletionHandler completionHandler,
      int maxRetries,
      long acknowledgementTimeoutMillis) {
    if (maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries must not be negative");
    }
    if (acknowledgementTimeoutMillis <= 0) {
      throw new IllegalArgumentException("acknowledgementTimeoutMillis must be positive");
    }

    this.lock = new Object();
    this.inboundLock = new Object();
    this.sequenceId = UUID.randomUUID();
    this.commandSet = Objects.requireNonNull(commandSet, "commandSet must not be null");
    this.messages = List.copyOf(Objects.requireNonNull(commandSet.messages(), "commandSet.messages must not be null"));
    this.sender = Objects.requireNonNull(sender, "sender must not be null");
    this.acknowledgementHandler = Objects.requireNonNull(acknowledgementHandler, "acknowledgementHandler must not be null");
    this.completionHandler = Objects.requireNonNull(completionHandler, "completionHandler must not be null");
    this.maxRetries = maxRetries;
    this.acknowledgementTimeoutMillis = acknowledgementTimeoutMillis;
    this.waitingIndex = -1;

    logger.log(MAVLINK_EVENT_LIST_SENDER_CREATED, sequenceId, commandSet.operation(), commandSet.modelName(), messages.size());
  }

  public void start() {
    synchronized (lock) {
      if (terminal) {
        logger.log(MAVLINK_EVENT_LIST_SENDER_START_IGNORED_TERMINAL, sequenceId, commandSet.operation(), commandSet.modelName());
        return;
      }
      if (started) {
        logger.log(MAVLINK_EVENT_LIST_SENDER_START_IGNORED_STARTED, sequenceId, commandSet.operation(), commandSet.modelName());
        return;
      }
      started = true;
    }

    logger.log(MAVLINK_EVENT_LIST_SENDER_STARTING, sequenceId, commandSet.operation(), commandSet.modelName());
    walk();
  }

  public void onMavlinkMessage(MavlinkPacket receivedPacket) {
    if (!isActive.get()) {
      return;
    }

    Objects.requireNonNull(receivedPacket, "receivedMessage must not be null");

    synchronized (inboundLock) {
      MavlinkMessage sentMessage;
      int sentIndex;

      synchronized (lock) {
        if (terminal) {
          logger.log(MAVLINK_EVENT_LIST_SENDER_INBOUND_IGNORED_TERMINAL, sequenceId, commandSet.operation(), commandSet.modelName());
          return;
        }
        if (waitingMessage == null) {
          logger.log(MAVLINK_EVENT_LIST_SENDER_INBOUND_IGNORED_NOT_WAITING, sequenceId, commandSet.operation(), commandSet.modelName());
          return;
        }

        sentMessage = waitingMessage;
        sentIndex = waitingIndex;
      }

      Acknowledgement acknowledgement = acknowledgementHandler.acknowledge(sentMessage, receivedPacket);
      if (acknowledgement == null) {
        acknowledgement = Acknowledgement.notRelated();
      }
      handleAcknowledgement(sentMessage, receivedPacket, sentIndex, acknowledgement);
    }
  }

  public void timeout() {
    processTimeout(null, null, null);
  }

  private void processTimeout(Integer expectedIndex, MavlinkMessage expectedMessage, Long expectedGeneration) {
    MavlinkMessage message;
    int index;
    boolean retriesExhausted;

    synchronized (lock) {
      if (terminal) {
        return;
      }

      if (expectedGeneration != null
          && (expectedGeneration != acknowledgementTimeoutGeneration
          || waitingIndex != expectedIndex
          || waitingMessage != expectedMessage)) {
        return;
      }

      if (expectedGeneration != null) {
        acknowledgementTimeoutFuture = null;
      }

      message = waitingMessage;
      index = waitingMessage == null ? nextIndex : waitingIndex;
      retriesExhausted = waitingMessage == null || retryCount >= maxRetries;

      if (!retriesExhausted) {
        retryCount++;
      }
    }

    if (retriesExhausted) {
      complete(MavlinkSendResult.Status.TIMEOUT, index, message, null, null, "MAVLink event list sender timed out");
      return;
    }

    try {
      resendWaitingMessage(index, message);
    } catch (Exception exception) {
      complete(MavlinkSendResult.Status.FAILED, index, message, null, exception, "Failed to resend MAVLink message after timeout");
    }
  }

  public void cancel() {
    completeFromCurrentState(MavlinkSendResult.Status.CANCELLED, "MAVLink event list sender cancelled");
  }

  @Override
  public void close() {
    completeFromCurrentState(MavlinkSendResult.Status.CLOSED, "MAVLink event list sender closed");
  }

  private void handleAcknowledgement(MavlinkMessage sentMessage, MavlinkPacket receivedMessage, int sentIndex, Acknowledgement acknowledgement) {
    Action action = acknowledgement.action();

    if (action != Action.NOT_RELATED) {
      synchronized (lock) {
        lastReceivedMessage = receivedMessage;
      }
    }

    switch (action) {
      case NOT_RELATED ->
          logger.log(MAVLINK_EVENT_LIST_SENDER_ACK_IGNORED_UNRELATED, sequenceId, commandSet.operation(), commandSet.modelName(), sentIndex + 1);

      case WAIT -> {
        resetRetryCount(sentMessage, sentIndex);
        scheduleAcknowledgementTimeout(sentIndex, sentMessage);
        logger.log(MAVLINK_EVENT_LIST_SENDER_ACK_PENDING, sequenceId, commandSet.operation(), commandSet.modelName(), sentIndex + 1);
      }

      case ADVANCE -> handleAdvance(sentMessage, sentIndex);
      case SEND_INDEX -> handleSendIndex(acknowledgement.index());

      case COMPLETE ->
          complete(MavlinkSendResult.Status.SUCCESS, sentIndex, sentMessage, receivedMessage, null, "MAVLink event list sender completed successfully");

      case FAIL ->
          complete(MavlinkSendResult.Status.FAILED, sentIndex, sentMessage, receivedMessage, null, failureReason(acknowledgement));
    }
  }

  private void handleAdvance(MavlinkMessage sentMessage, int sentIndex) {
    synchronized (lock) {
      if (terminal) {
        logger.log(MAVLINK_EVENT_LIST_SENDER_ACK_IGNORED_TERMINAL, sequenceId, commandSet.operation(), commandSet.modelName());
        return;
      }

      if (waitingMessage != sentMessage || waitingIndex != sentIndex) {
        logger.log(MAVLINK_EVENT_LIST_SENDER_ACK_IGNORED_STATE_CHANGED, sequenceId, commandSet.operation(), commandSet.modelName());
        return;
      }

      clearWaitingState();
    }

    logger.log(MAVLINK_EVENT_LIST_SENDER_ACK_SUCCESS, sequenceId, commandSet.operation(), commandSet.modelName(), sentIndex + 1);
    walk();
  }

  private void handleSendIndex(int requestedIndex) {
    MavlinkMessage message = messageAt(requestedIndex);
    if (message == null) {
      complete(MavlinkSendResult.Status.FAILED, requestedIndex, null, null, null, "Acknowledgement requested an invalid MAVLink message index");
      return;
    }

    synchronized (lock) {
      if (terminal) {
        logger.log(MAVLINK_EVENT_LIST_SENDER_ACK_IGNORED_TERMINAL, sequenceId, commandSet.operation(), commandSet.modelName());
        return;
      }

      clearWaitingState();
    }

    sendRequestedIndex(requestedIndex, message);
  }

  private void walk() {
    while (true) {
      MavlinkMessage message;
      int sentIndex;
      boolean requiresAcknowledgement;

      synchronized (lock) {
        if (terminal || waitingMessage != null) {
          return;
        }

        if (nextIndex >= messages.size()) {
          complete(MavlinkSendResult.Status.SUCCESS, messages.size(), null, null, null, "MAVLink event list sender completed successfully");
          return;
        }

        sentIndex = nextIndex;
        message = messages.get(nextIndex);
        nextIndex++;

        requiresAcknowledgement = acknowledgementHandler.requiresAcknowledgement(message);
        if (requiresAcknowledgement) {
          prepareWaitingState(sentIndex, message);
        }
      }

      try {
        sendMessage(sentIndex, message, requiresAcknowledgement);
      } catch (Exception exception) {
        complete(MavlinkSendResult.Status.FAILED, sentIndex, message, null, exception, "Failed to send MAVLink message");
        return;
      }

      if (requiresAcknowledgement) {
        return;
      }

      logger.log(MAVLINK_EVENT_LIST_SENDER_NO_ACK_ADVANCING, sequenceId, commandSet.operation(), commandSet.modelName(), sentIndex + 1);
    }
  }

  private void sendRequestedIndex(int requestedIndex, MavlinkMessage message) {
    boolean requiresAcknowledgement;

    synchronized (lock) {
      if (terminal) {
        return;
      }
      nextIndex = Math.max(nextIndex, requestedIndex + 1);
      requiresAcknowledgement = acknowledgementHandler.requiresAcknowledgement(message);

      if (requiresAcknowledgement) {
        prepareWaitingState(requestedIndex, message);
      }
    }

    try {
      sendMessage(requestedIndex, message, requiresAcknowledgement);
    } catch (Exception exception) {
      complete(MavlinkSendResult.Status.FAILED, requestedIndex, message, null, exception, "Failed to send requested MAVLink message");
      return;
    }

    if (requiresAcknowledgement) {
      return;
    }

    logger.log(MAVLINK_EVENT_LIST_SENDER_NO_ACK_ADVANCING, sequenceId, commandSet.operation(), commandSet.modelName(), requestedIndex + 1);
    walk();
  }

  private void resendWaitingMessage(int index, MavlinkMessage message) throws Exception {
    synchronized (lock) {
      if (terminal || waitingMessage != message || waitingIndex != index) {
        return;
      }
    }

    sendMessage(index, message, true);
  }

  private void sendMessage(int index, MavlinkMessage message, boolean requiresAcknowledgement) throws Exception {
    logger.log(MAVLINK_EVENT_LIST_SENDER_SENDING, sequenceId, commandSet.operation(), commandSet.modelName(), index + 1, messages.size(), messageName(message), requiresAcknowledgement);
    synchronized (lock) {
      lastSentMessage = message;
    }
    sender.send(message);

    if (requiresAcknowledgement) {
      scheduleAcknowledgementTimeout(index, message);
      logger.log(MAVLINK_EVENT_LIST_SENDER_WAITING_FOR_ACK, sequenceId, commandSet.operation(), commandSet.modelName(), index + 1);
    }
  }

  private void prepareWaitingState(int index, MavlinkMessage message) {
    waitingMessage = message;
    waitingIndex = index;
    retryCount = 0;
  }

  private void resetRetryCount(MavlinkMessage message, int index) {
    synchronized (lock) {
      if (waitingMessage == message && waitingIndex == index) {
        retryCount = 0;
      }
    }
  }

  private void scheduleAcknowledgementTimeout(int index, MavlinkMessage message) {
    synchronized (lock) {
      if (terminal || waitingMessage != message || waitingIndex != index) {
        return;
      }

      ScheduledFuture<?> existingFuture = acknowledgementTimeoutFuture;
      if (existingFuture != null) {
        existingFuture.cancel(false);
      }

      long generation = ++acknowledgementTimeoutGeneration;
      acknowledgementTimeoutFuture =
          SimpleTaskScheduler.getInstance()
              .schedule(
                  () -> processTimeout(index, message, generation),
                  acknowledgementTimeoutMillis,
                  TimeUnit.MILLISECONDS);
    }
  }

  private void cancelAcknowledgementTimeout() {
    acknowledgementTimeoutGeneration++;

    ScheduledFuture<?> future = acknowledgementTimeoutFuture;
    acknowledgementTimeoutFuture = null;

    if (future != null) {
      future.cancel(false);
    }
  }

  private void clearWaitingState() {
    cancelAcknowledgementTimeout();
    waitingMessage = null;
    waitingIndex = -1;
    retryCount = 0;
  }

  private void completeFromCurrentState(MavlinkSendResult.Status status, String reason) {
    MavlinkMessage message;
    int index;

    synchronized (lock) {
      message = waitingMessage;
      index = waitingMessage == null ? nextIndex : waitingIndex;
    }

    complete(status, index, message, null, null, reason);
  }

  private void complete(MavlinkSendResult.Status status, int index, MavlinkMessage sentMessage, MavlinkPacket receivedMessage, Throwable cause, String reason) {
    MavlinkSendResult result;

    synchronized (lock) {
      if (terminal) {
        logger.log(MAVLINK_EVENT_LIST_SENDER_TERMINAL_TRANSITION_IGNORED, sequenceId, commandSet.operation(), commandSet.modelName(), status);
        return;
      }

      terminal = true;
      clearWaitingState();
      MavlinkMessage retainedSentMessage = sentMessage == null ? lastSentMessage : sentMessage;
      MavlinkPacket retainedReceivedMessage = receivedMessage == null ? lastReceivedMessage : receivedMessage;
      result = new MavlinkSendResult(this, sequenceId, status, index, messages.size(), retainedSentMessage, retainedReceivedMessage, cause, reason);
    }

    isActive.set(false);
    logCompletion(result);
    notifyCompletionHandler(result);
  }

  private void notifyCompletionHandler(MavlinkSendResult result) {
    try {
      completionHandler.onComplete(result);
    } catch (Exception exception) {
      logger.log(MAVLINK_EVENT_LIST_SENDER_COMPLETION_HANDLER_FAILED, sequenceId, commandSet.operation(), commandSet.modelName(), exception.getMessage());
    }
  }

  private void logCompletion(MavlinkSendResult result) {
    if (result.status() == MavlinkSendResult.Status.SUCCESS) {
      logger.log(MAVLINK_EVENT_LIST_SENDER_COMPLETED_SUCCESS, sequenceId, commandSet.operation(), commandSet.modelName(), messages.size());
      return;
    }

    if (result.status() == MavlinkSendResult.Status.FAILED) {
      logger.log(
          MAVLINK_EVENT_LIST_SENDER_FAILED,
          sequenceId,
          commandSet.operation(),
          commandSet.modelName(),
          result.status(),
          result.index() + 1,
          result.total(),
          messageName(result.sentMessage()),
          result.reason());
    } else {
      logger.log(
          MAVLINK_EVENT_LIST_SENDER_COMPLETED,
          sequenceId,
          commandSet.operation(),
          commandSet.modelName(),
          result.status(),
          result.index() + 1,
          result.total(),
          messageName(result.sentMessage()),
          result.reason());
    }

    if (result.cause() != null) {
      logger.log(MAVLINK_EVENT_LIST_SENDER_FAILED_EXCEPTION, sequenceId, commandSet.operation(), commandSet.modelName(), result.cause().getMessage());
    }
  }

  private MavlinkMessage messageAt(int index) {
    if (index < 0 || index >= messages.size()) {
      return null;
    }
    return messages.get(index);
  }

  private String failureReason(Acknowledgement acknowledgement) {
    if (acknowledgement.reason() == null || acknowledgement.reason().isBlank()) {
      return "MAVLink acknowledgement failed";
    }
    return acknowledgement.reason();
  }

  private String messageName(MavlinkMessage message) {
    return message == null ? "" : message.getClass().getSimpleName();
  }
}
