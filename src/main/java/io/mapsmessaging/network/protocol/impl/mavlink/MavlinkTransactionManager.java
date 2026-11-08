/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_TRANSACTION_CANCELLED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_TRANSACTION_CONNECTION_CLOSED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_TRANSACTION_FAILED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_TRANSACTION_REJECTED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_TRANSACTION_RETRYING;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_TRANSACTION_SEND_FAILED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_TRANSACTION_STATUS_PUBLISH_FAILED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_TRANSACTION_TIMED_OUT;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandInt;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionCount;
import io.mapsmessaging.state.mavlink.messages.MavlinkMissionItemInt;
import io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MAVLink wire transaction state for one protocol connection. An engine command is accepted
 * through the existing outbound subscription; the twin never owns a protocol sender.
 *
 * <p>Native MAVLink command messages and mission-item arrays use protocol-owned transactions.
 * Other single-frame outbound messages retain their existing one-way send path.
 */
final class MavlinkTransactionManager implements AutoCloseable {

  static final int DEFAULT_MAX_RETRIES = 6;
  static final long DEFAULT_ACK_TIMEOUT_MILLIS = 5_000L;

  @FunctionalInterface
  interface WireSender {
    void send(JsonObject message, String destination) throws Exception;
  }

  @FunctionalInterface
  interface StatusPublisher {
    void publish(String topic, JsonObject status);
  }

  private static final Logger logger = LoggerFactory.getLogger(MavlinkTransactionManager.class);
  private static final Gson AUDIT_GSON = GsonFactory.createStrictJsonWithSafeFloats();
  private static final int MAX_RECENT_RESULTS = 128;

  private final WireSender wireSender;
  private final StatusPublisher statusPublisher;
  private final String defaultStatusTopic;
  private final int targetSystem;
  private final int localSystem;
  private final int localComponent;
  private final boolean verifySourceAddress;
  private final Map<String, MavlinkTerminalResult> recentResults = new LinkedHashMap<>();
  private MavlinkTransaction active;
  private boolean closed;

  MavlinkTransactionManager(WireSender wireSender, StatusPublisher statusPublisher, String defaultStatusTopic, int targetSystem, int localSystem, int localComponent) {
    this(wireSender, statusPublisher, defaultStatusTopic, targetSystem, localSystem, localComponent, false);
  }

  MavlinkTransactionManager(WireSender wireSender, StatusPublisher statusPublisher, String defaultStatusTopic, int targetSystem, int localSystem,
      int localComponent, boolean verifySourceAddress) {
    this.wireSender = Objects.requireNonNull(wireSender);
    this.statusPublisher = Objects.requireNonNull(statusPublisher);
    this.defaultStatusTopic = defaultStatusTopic;
    this.targetSystem = targetSystem;
    this.localSystem = localSystem;
    this.localComponent = localComponent;
    this.verifySourceAddress = verifySourceAddress;
  }

  synchronized void submitNative(JsonElement input, String destination) {
    JsonObject request;
    if (input.isJsonArray()) {
      request = new JsonObject();
      request.add("messages", input.deepCopy());
    } else if (input.isJsonObject()) {
      JsonObject object = input.getAsJsonObject();
      if (object.has("operation")) {
        reject(defaultStatusTopic, null, null, destination, "Unsupported operation envelope; submit MAVLink JSON directly");
        return;
      }
      if (object.has("header")) {
        request = new JsonObject();
        JsonArray messages = new JsonArray();
        JsonObject packet = object.deepCopy();
        packet.remove("transactionId");
        packet.remove("statusTopic");
        messages.add(packet);
        request.add("messages", messages);
        if (object.has("transactionId")) {
          request.add("transactionId", object.get("transactionId").deepCopy());
        }
        if (object.has("statusTopic")) {
          request.add("statusTopic", object.get("statusTopic").deepCopy());
        }
      } else {
        request = object.deepCopy();
      }
    } else {
      reject(defaultStatusTopic, null, null, destination, "Expected MAVLink JSON message or mission item array");
      return;
    }
    submit(request, destination);
  }

  private void submit(JsonObject request, String destination) {
    String id = null;
    String topic = defaultStatusTopic;
    String operation = null;
    String fingerprint;
    try {
      id = text(request, "transactionId", null);
      topic = text(request, "statusTopic", defaultStatusTopic);
      operation = inferOperation(request);
      if (id == null || id.isBlank()) {
        id = UUID.randomUUID().toString();
      }
      fingerprint = fingerprint(request, destination);
    } catch (RuntimeException exception) {
      reject(topic, id, operation, destination, "Invalid transaction request: " + exception.getMessage());
      return;
    }
    if (id == null || id.isBlank()) {
      reject(topic, id, operation, destination, "Missing transactionId");
      return;
    }
    if (active != null && active.id.equals(id)) {
      if (!active.fingerprint.equals(fingerprint)) {
        reject(topic, id, operation, destination, "Transaction ID reused with different request");
      } else {
        publish(active, active.status, null);
      }
      return;
    }
    MavlinkTerminalResult previous = recentResults.get(id);
    if (previous != null) {
      if (!previous.fingerprint().equals(fingerprint)) {
        reject(topic, id, operation, destination, "Transaction ID reused with different request");
      } else {
        publish(previous.topic(), id, previous.status(), previous.reason(), previous.attempts(), previous.retries(), previous.requested(),
            previous.operation(), previous.system(), previous.component(), previous.total(), previous.lastSent(), previous.lastResponse());
      }
      return;
    }
    if (closed) {
      reject(topic, id, operation, destination, "Protocol connection is closed");
      return;
    }
    if (active != null) {
      reject(topic, id, operation, destination, "Another transaction is active");
      return;
    }

    try {
      MavlinkTransaction transaction = prepare(request, destination, topic, id, operation);
      transaction.fingerprint = fingerprint;
      active = transaction;
      publish(transaction, "QUEUED", null);
      send(transaction, 0);
    } catch (RuntimeException exception) {
      reject(topic, id, operation, destination, exception.getMessage());
    }
  }

  synchronized void cancelRequest(JsonObject request, String destination) {
    JsonElement element = request.get("transactionId");
    if (request.size() != 1 || element == null || !element.isJsonPrimitive()
        || !element.getAsJsonPrimitive().isString() || element.getAsString().isBlank()) {
      reject(defaultStatusTopic, null, null, destination, "Cancellation requires only a nonblank transactionId");
      return;
    }
    cancel(element.getAsString(), destination);
  }

  synchronized void cancel(String id, String destination) {
    if (active != null && active.id.equals(id) && samePeer(active.destination, destination)) {
      finish(active, "CANCELLED", "Cancelled by requester");
    } else {
      reject(defaultStatusTopic, id, null, destination, "No matching active transaction for destination");
    }
  }

  private static String inferOperation(JsonObject request) {
    JsonElement element = request.get("messages");
    if (element == null || !element.isJsonArray() || element.getAsJsonArray().size() == 0) {
      throw new IllegalArgumentException("MAVLink message or mission item array is required");
    }
    JsonArray messages = element.getAsJsonArray();
    JsonElement first = messages.get(0);
    if (!first.isJsonObject() || !first.getAsJsonObject().has("header")) {
      throw new IllegalArgumentException("Each MAVLink item requires a header");
    }
    int firstMessageId = messageId(first.getAsJsonObject());
    if (firstMessageId == MavlinkMissionItemInt.MESSAGE_ID_MISSION_ITEM_INT
        || firstMessageId == MavlinkMissionCount.MESSAGE_ID_MISSION_COUNT) {
      return "MISSION_UPLOAD";
    }
    if (messages.size() == 1 && (firstMessageId == MavlinkCommandLong.MESSAGE_ID_COMMAND_LONG
        || firstMessageId == MavlinkCommandInt.MESSAGE_ID_COMMAND_INT)) {
      return "COMMAND";
    }
    return "PACKET";
  }

  private MavlinkTransaction prepare(JsonObject request, String destination, String topic, String id, String operation) {
    JsonElement element = request.get("messages");
    if (element == null || !element.isJsonArray()) {
      throw new IllegalArgumentException("Transaction requires an array of MAVLink messages");
    }
    JsonArray input = element.getAsJsonArray();
    List<JsonObject> messages = new ArrayList<>(input.size());
    for (JsonElement item : input) {
      if (!item.isJsonObject()) {
        throw new IllegalArgumentException("Each MAVLink message must be an object");
      }
      JsonObject packet = item.getAsJsonObject();
      if (!packet.has("header") || !packet.get("header").isJsonObject() || !packet.has("payload") || !packet.get("payload").isJsonObject()) {
        throw new IllegalArgumentException("Each MAVLink message requires header and payload");
      }
      messages.add(packet.deepCopy());
    }
    if (messages.isEmpty()) {
      throw new IllegalArgumentException("Transaction contains no MAVLink messages");
    }
    if ("MISSION_UPLOAD".equals(operation)
        && messageId(messages.getFirst()) == MavlinkMissionItemInt.MESSAGE_ID_MISSION_ITEM_INT) {
      JsonObject first = messages.getFirst();
      JsonObject firstPayload = first.getAsJsonObject("payload");
      JsonObject count = new JsonObject();
      JsonObject header = first.getAsJsonObject("header").deepCopy();
      header.addProperty("messageId", MavlinkMissionCount.MESSAGE_ID_MISSION_COUNT);
      JsonObject payload = new JsonObject();
      payload.addProperty("target_system", requiredInt(firstPayload, "target_system"));
      payload.addProperty("target_component", requiredInt(firstPayload, "target_component"));
      payload.addProperty("count", messages.size());
      payload.addProperty("mission_type", integer(firstPayload, "mission_type", 0));
      count.add("header", header);
      count.add("payload", payload);
      messages.add(0, count);
    }
    JsonObject firstPayload = messages.getFirst().getAsJsonObject("payload");
    int system = "PACKET".equals(operation) ? integer(firstPayload, "target_system", targetSystem) : requiredInt(firstPayload, "target_system");
    int component = "PACKET".equals(operation) ? integer(firstPayload, "target_component", 0) : requiredInt(firstPayload, "target_component");
    if (system < 0 || system > 255 || component < 0 || component > 255 || (!"PACKET".equals(operation) && system == 0)
        || (targetSystem > 0 && system != 0 && system != targetSystem)) {
      throw new IllegalArgumentException("Invalid MAVLink transaction target");
    }

    if ("PACKET".equals(operation)) {
      if (messages.size() != 1) {
        throw new IllegalArgumentException("PACKET requires exactly one MAVLink message");
      }
    } else if ("COMMAND".equals(operation)) {
      if (messages.size() != 1 || (messageId(messages.getFirst()) != MavlinkCommandLong.MESSAGE_ID_COMMAND_LONG
          && messageId(messages.getFirst()) != MavlinkCommandInt.MESSAGE_ID_COMMAND_INT)) {
        throw new IllegalArgumentException("COMMAND requires exactly one COMMAND_LONG/COMMAND_INT");
      }
      requiredInt(firstPayload, "command");
    } else {
      if (messageId(messages.getFirst()) != MavlinkMissionCount.MESSAGE_ID_MISSION_COUNT
          || requiredInt(firstPayload, "count") != messages.size() - 1) {
        throw new IllegalArgumentException("Mission must start with matching MISSION_COUNT");
      }
      int missionType = integer(firstPayload, "mission_type", 0);
      for (int i = 1; i < messages.size(); i++) {
        JsonObject packet = messages.get(i);
        JsonObject payload = packet.getAsJsonObject("payload");
        if (messageId(packet) != MavlinkMissionItemInt.MESSAGE_ID_MISSION_ITEM_INT
            || requiredInt(payload, "seq") != i - 1
            || requiredInt(payload, "target_system") != system
            || requiredInt(payload, "target_component") != component
            || integer(payload, "mission_type", 0) != missionType) {
          throw new IllegalArgumentException("Invalid mission item at index " + (i - 1));
        }
      }
    }
    MavlinkTransaction transaction = new MavlinkTransaction(id, topic, operation, destination, List.copyOf(messages), system, component,
        integer(firstPayload, "mission_type", 0), localSystem, localComponent);
    if ("COMMAND".equals(operation)) {
      JsonObject payload = messages.getFirst().getAsJsonObject("payload");
      int command = requiredInt(payload, "command");
      if (command == 224) {
        float sequence = payload.has("param1") ? payload.get("param1").getAsFloat() : -1f;
        if (!Float.isFinite(sequence) || sequence < -1 || sequence > 65535 || (sequence >= 0 && sequence != Math.floor(sequence))) {
          throw new IllegalArgumentException("Invalid MAVLink current mission sequence");
        }
        transaction.expectedMissionSequence = sequence >= 0 ? (int) sequence : -1;
      } else if (command == 512) {
        float requested = payload.has("param1") ? payload.get("param1").getAsFloat() : -1f;
        if (!Float.isFinite(requested) || requested < 0 || requested != Math.floor(requested) || requested > 16777215) {
          throw new IllegalArgumentException("MAV_CMD_REQUEST_MESSAGE requires a valid message id in param1");
        }
        transaction.requestedMessageId = (int) requested;
      }
    }
    return transaction;
  }

  synchronized void onFrame(ProcessedFrame received) {
    onFrame(received, null);
  }

  synchronized void onFrame(ProcessedFrame received, String sourceAddress) {
    MavlinkTransaction transaction = active;
    if (transaction == null || received == null || !received.isValid()
        || (verifySourceAddress && !samePeer(transaction.destination, sourceAddress))) {
      return;
    }
    int sourceSystem = received.getFrame().getSystemId();
    int sourceComponent = received.getFrame().getComponentId();
    if (sourceSystem != transaction.system || (transaction.component != 0 && sourceComponent != transaction.component)) {
      return;
    }
    int id = received.getFrame().getMessageId();
    if ("COMMAND".equals(transaction.operation)) {
      if (transaction.expectedMissionSequence >= 0 && id == 42
          && integer(received.getFields(), "seq", -1) == transaction.expectedMissionSequence) {
        transaction.lastResponse = responseEvidence(received);
        transaction.missionCurrentSeen = true;
        if (transaction.commandAccepted || transaction.commandInProgress) {
          finish(transaction, "COMPLETED", null);
        }
        return;
      }
      if (transaction.requestedMessageId >= 0 && id == transaction.requestedMessageId) {
        transaction.lastResponse = responseEvidence(received);
        transaction.requestedMessageSeen = true;
        if (transaction.commandAccepted || transaction.commandInProgress) {
          finish(transaction, "COMPLETED", null);
        }
        return;
      }
      if (id != MavlinkMessageIds.COMMAND_ACK
          || integer(received.getFields(), "command", -1)
              != requiredInt(transaction.messages.getFirst().getAsJsonObject("payload"), "command")
          || !ackTargetMatches(received, transaction)) {
        return;
      }
      transaction.lastResponse = responseEvidence(received);
      int result = integer(received.getFields(), "result", -1);
      if (result == 5) {
        transaction.commandInProgress = true;
        scheduleTimeout(transaction);
        publish(transaction, "IN_PROGRESS", null);
      } else if (result == 0) {
        transaction.commandAccepted = true;
        boolean awaitingMissionCurrent = transaction.expectedMissionSequence >= 0 && !transaction.missionCurrentSeen;
        boolean awaitingRequestedMessage = transaction.requestedMessageId >= 0 && !transaction.requestedMessageSeen;
        if (awaitingMissionCurrent || awaitingRequestedMessage) {
          scheduleTimeout(transaction);
          publish(transaction, "WAITING_FOR_RESPONSE", null);
        } else {
          finish(transaction, "COMPLETED", null);
        }
      } else {
        finish(transaction, "FAILED", "COMMAND_ACK result " + result);
      }
      return;
    }
    if (id == MavlinkMessageIds.MISSION_REQUEST || id == MavlinkMessageIds.MISSION_REQUEST_INT) {
      if (!ackTargetMatches(received, transaction) || integer(received.getFields(), "mission_type", 0) != transaction.missionType) {
        return;
      }
      int sequence = integer(received.getFields(), "seq", -1);
      if (sequence < 0 || sequence >= transaction.messages.size() - 1) {
        // A delayed or corrupt request must not abort an otherwise recoverable upload.
        return;
      }
      // Ignore stale requests from an earlier upload until this upload starts at item zero.
      if (transaction.requested.isEmpty() && sequence != 0) {
        return;
      }
      transaction.lastResponse = responseEvidence(received);
      send(transaction, sequence + 1);
      if (active == transaction) {
        transaction.requested.set(sequence);
        publish(transaction, "UPLOADING", null);
      }
    } else if (id == MavlinkMessageIds.MISSION_ACK && ackTargetMatches(received, transaction)
        && integer(received.getFields(), "mission_type", 0) == transaction.missionType) {
      transaction.lastResponse = responseEvidence(received);
      int result = integer(received.getFields(), "type", -1);
      if (result == 13) { // MAV_MISSION_INVALID_SEQUENCE: out-of-order requests are recoverable.
        scheduleTimeout(transaction);
        return;
      }
      if (result == 0 && transaction.requested.cardinality() == transaction.messages.size() - 1) {
        finish(transaction, "COMPLETED", null);
      } else if (result == 0) {
        finish(transaction, "FAILED", "Mission accepted before every item was requested");
      } else {
        finish(transaction, "FAILED", "MISSION_ACK result " + result);
      }
    }
  }

  private boolean ackTargetMatches(ProcessedFrame received, MavlinkTransaction transaction) {
    int target = integer(received.getFields(), "target_system", 0);
    int component = integer(received.getFields(), "target_component", 0);
    return (target == 0 || transaction.localSystem == 0 || target == transaction.localSystem)
        && (component == 0 || transaction.localComponent == 0 || component == transaction.localComponent);
  }

  private void send(MavlinkTransaction transaction, int index) {
    try {
      wireSender.send(transaction.messages.get(index).deepCopy(), transaction.destination);
      transaction.lastSent = transaction.messages.get(index).deepCopy();
      transaction.lastIndex = index;
      transaction.attempts++;
      if ("PACKET".equals(transaction.operation)) {
        finish(transaction, "SENT", null);
        return;
      }
      scheduleTimeout(transaction);
      publish(transaction, "MISSION_UPLOAD".equals(transaction.operation) ? "WAITING_FOR_MISSION_REQUEST" : "WAITING_FOR_ACK", null);
    } catch (Exception exception) {
      logger.log(MAVLINK_TRANSACTION_SEND_FAILED, transaction.id, transaction.operation, transaction.destination, exception);
      finish(transaction, "FAILED", "MAVLink transmission failed: " + exception.getMessage());
    }
  }

  private void scheduleTimeout(MavlinkTransaction transaction) {
    if (transaction.future != null) {
      transaction.future.cancel(false);
    }
    long generation = ++transaction.generation;
    transaction.future = SimpleTaskScheduler.getInstance().schedule(() -> timeout(transaction, generation), DEFAULT_ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
  }

  private synchronized void timeout(MavlinkTransaction transaction, long generation) {
    if (active != transaction || transaction.generation != generation) {
      return;
    }
    if (transaction.retries >= DEFAULT_MAX_RETRIES) {
      finish(transaction, "TIMED_OUT", "MAVLink response timeout");
      return;
    }
    if ("COMMAND".equals(transaction.operation) && (transaction.commandInProgress || transaction.commandAccepted)) {
      finish(transaction, "TIMED_OUT", transaction.commandAccepted
          ? "MAVLink command accepted but expected response not observed; outcome unknown"
          : "Command progress stopped; outcome unknown");
      return;
    }
    if ("MISSION_UPLOAD".equals(transaction.operation) && !transaction.requested.isEmpty()) {
      // The vehicle requests mission items; do not transmit an unsolicited item after requests stop.
      finish(transaction, "TIMED_OUT", "Mission request or final acknowledgement not received; upload outcome unknown");
      return;
    }
    transaction.retries++;
    logger.log(MAVLINK_TRANSACTION_RETRYING, transaction.id, transaction.retries, DEFAULT_MAX_RETRIES, transaction.operation, transaction.destination);
    publish(transaction, "RETRYING", null);
    if ("COMMAND".equals(transaction.operation) && messageId(transaction.messages.getFirst()) == MavlinkCommandLong.MESSAGE_ID_COMMAND_LONG) {
      JsonObject payload = transaction.messages.getFirst().getAsJsonObject("payload");
      payload.addProperty("confirmation", integer(payload, "confirmation", 0) + 1);
    }
    // Before the first vehicle request, retry MISSION_COUNT. Never send an unsolicited mission item.
    send(transaction, "MISSION_UPLOAD".equals(transaction.operation) ? 0 : transaction.lastIndex);
  }

  private void finish(MavlinkTransaction transaction, String status, String reason) {
    if (active != transaction) {
      return;
    }
    active = null;
    if (transaction.future != null) {
      transaction.future.cancel(false);
    }
    int total = transaction.messages.size() - ("MISSION_UPLOAD".equals(transaction.operation) ? 1 : 0);
    recentResults.put(transaction.id, new MavlinkTerminalResult(transaction.topic, status, reason, transaction.attempts, transaction.retries,
        transaction.requested.cardinality(), transaction.operation, transaction.system, transaction.component, total, transaction.fingerprint,
        transaction.lastSent == null ? null : transaction.lastSent.deepCopy(),
        transaction.lastResponse == null ? null : transaction.lastResponse.deepCopy()));
    if (recentResults.size() > MAX_RECENT_RESULTS) {
      recentResults.remove(recentResults.keySet().iterator().next());
    }
    switch (status) {
      case "FAILED" -> {
        // The endpoint failure already logs the original exception and context.
        if (reason == null || !reason.startsWith("MAVLink transmission failed: ")) {
          logger.log(MAVLINK_TRANSACTION_FAILED, transaction.id, transaction.operation, transaction.destination, reason);
        }
      }
      case "TIMED_OUT" -> logger.log(MAVLINK_TRANSACTION_TIMED_OUT, transaction.id, transaction.operation, transaction.destination,
          transaction.attempts, transaction.retries, reason);
      case "CANCELLED" -> logger.log(MAVLINK_TRANSACTION_CANCELLED, transaction.id, transaction.operation, transaction.destination);
      case "CLOSED" -> logger.log(MAVLINK_TRANSACTION_CONNECTION_CLOSED, transaction.id, transaction.operation, transaction.destination);
      default -> {
        // Successful completion and normal packet progress are reported through status events only.
      }
    }
    publish(transaction, status, reason);
  }

  private void reject(String topic, String id, String operation, String destination, String reason) {
    logger.log(MAVLINK_TRANSACTION_REJECTED, id, operation, destination, reason);
    publish(topic, id, "REJECTED", reason, 0, 0, 0, operation, 0, 0, 0);
  }

  private void publish(MavlinkTransaction transaction, String status, String reason) {
    transaction.status = status;
    publish(transaction.topic, transaction.id, status, reason, transaction.attempts,
        transaction.retries, transaction.requested.cardinality(), transaction.operation,
        transaction.system, transaction.component, transaction.messages.size() -
            ("MISSION_UPLOAD".equals(transaction.operation) ? 1 : 0),
        transaction.lastSent, transaction.lastResponse);
  }

  private void publish(String topic, String id, String status, String reason,
      int attempts, int retries, int requested, String operation,
      int system, int component, int total) {
    publish(topic, id, status, reason, attempts, retries, requested, operation, system, component, total, null, null);
  }

  private void publish(String topic, String id, String status, String reason,
      int attempts, int retries, int requested, String operation,
      int system, int component, int total, JsonObject lastSent, JsonObject lastResponse) {
    if (topic == null || topic.isBlank()) {
      return;
    }
    JsonObject event = new JsonObject();
    if (id != null) {
      event.addProperty("transactionId", id);
    }
    event.addProperty("status", status);
    event.addProperty("attempts", attempts);
    event.addProperty("retryCount", retries);
    event.addProperty("requestedItems", requested);
    event.addProperty("totalItems", total);
    if (system > 0) {
      event.addProperty("systemId", system);
      event.addProperty("componentId", component);
    }
    if (reason != null) {
      event.addProperty("reason", reason);
    }
    if (lastSent != null) {
      event.add("lastSent", lastSent.deepCopy());
    }
    if (lastResponse != null) {
      event.add("lastResponse", lastResponse.deepCopy());
    }
    try {
      statusPublisher.publish(topic, event);
    } catch (RuntimeException exception) {
      logger.log(MAVLINK_TRANSACTION_STATUS_PUBLISH_FAILED, topic, exception);
    }
  }

  private static JsonObject responseEvidence(ProcessedFrame received) {
    JsonObject evidence = new JsonObject();
    JsonObject header = new JsonObject();
    header.addProperty("messageId", received.getFrame().getMessageId());
    header.addProperty("systemId", received.getFrame().getSystemId());
    header.addProperty("componentId", received.getFrame().getComponentId());
    evidence.add("header", header);
    evidence.add("payload", AUDIT_GSON.toJsonTree(received.getFields()));
    return evidence;
  }

  private static int messageId(JsonObject message) {
    return requiredInt(message.getAsJsonObject("header"), "messageId");
  }

  private static int requiredInt(JsonObject object, String name) {
    if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
      throw new IllegalArgumentException("Missing MAVLink field " + name);
    }
    return object.get(name).getAsInt();
  }

  private static int integer(JsonObject object, String name, int fallback) {
    return object != null && object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsInt() : fallback;
  }

  private static int integer(java.util.Map<String, Object> map, String name, int fallback) {
    Object value = map.get(name);
    return value instanceof Number number ? number.intValue() : fallback;
  }

  private static String text(JsonObject object, String name, String fallback) {
    return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsString() : fallback;
  }

  @Override
  public synchronized void close() {
    closed = true;
    if (active != null) {
      finish(active, "CLOSED", "MAVLink connection closed");
    }
  }

  private static boolean samePeer(String expected, String actual) {
    if (expected == null || actual == null) {
      return Objects.equals(expected, actual);
    }
    return normalisePeer(expected).equals(normalisePeer(actual));
  }

  private static String normalisePeer(String address) {
    String value = address.trim();
    int portSeparator = value.lastIndexOf(':');
    if (portSeparator < 0) {
      return value.toLowerCase(Locale.ROOT);
    }
    String host = value.substring(0, portSeparator);
    int hostSeparator = host.lastIndexOf('/');
    if (hostSeparator >= 0 && hostSeparator < host.length() - 1) {
      host = host.substring(hostSeparator + 1);
    } else if (host.startsWith("/")) {
      host = host.substring(1);
    }
    if (host.startsWith("[") && host.endsWith("]")) {
      host = host.substring(1, host.length() - 1);
    }
    return host.toLowerCase(Locale.ROOT) + value.substring(portSeparator);
  }

  private static String fingerprint(JsonObject request, String destination) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest((destination + "\n" + request).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 digest is unavailable", exception);
    }
  }

}
