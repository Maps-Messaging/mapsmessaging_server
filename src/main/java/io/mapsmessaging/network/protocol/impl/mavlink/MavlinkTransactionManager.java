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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * MAVLink wire transaction state for one protocol connection. An engine command is accepted
 * through the existing outbound subscription; the twin never owns a protocol sender.
 *
 * <p>The transaction envelope is opt-in. Ordinary single-frame outbound messages continue
 * through the existing, unchanged MavlinkProtocol send path.
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

  private static final int COMMAND_INT = 75;
  private static final int COMMAND_LONG = 76;
  private static final int COMMAND_ACK = 77;
  private static final int MISSION_COUNT = 44;
  private static final int MISSION_ITEM_INT = 73;
  private static final int MISSION_REQUEST = 40;
  private static final int MISSION_REQUEST_INT = 51;
  private static final int MISSION_ACK = 47;

  private final WireSender wireSender;
  private final StatusPublisher statusPublisher;
  private final String defaultStatusTopic;
  private final int targetSystem;
  private final int localSystem;
  private final int localComponent;
  private Transaction active;
  private boolean closed;

  MavlinkTransactionManager(WireSender wireSender, StatusPublisher statusPublisher,
      String defaultStatusTopic, int targetSystem, int localSystem, int localComponent) {
    this.wireSender = Objects.requireNonNull(wireSender);
    this.statusPublisher = Objects.requireNonNull(statusPublisher);
    this.defaultStatusTopic = defaultStatusTopic;
    this.targetSystem = targetSystem;
    this.localSystem = localSystem;
    this.localComponent = localComponent;
  }

  synchronized void submit(JsonObject request, String destination) {
    String id = text(request, "transactionId", null);
    String topic = text(request, "statusTopic", defaultStatusTopic);
    if (id == null || id.isBlank()) {
      publish(topic, id, "REJECTED", "Missing transactionId", 0, 0, 0, null, 0, 0, 0);
      return;
    }
    String operation = text(request, "operation", "");
    if ("CANCEL".equals(operation)) {
      if (active != null && active.id.equals(id)) {
        finish(active, "CANCELLED", "Cancelled by requester");
      } else {
        publish(topic, id, "REJECTED", "No matching active transaction", 0, 0, 0, null, 0, 0, 0);
      }
      return;
    }
    if (closed) {
      publish(topic, id, "REJECTED", "Protocol connection is closed", 0, 0, 0, null, 0, 0, 0);
      return;
    }
    if (active != null) {
      publish(topic, id, "REJECTED", "Another transaction is active", 0, 0, 0, null, 0, 0, 0);
      return;
    }

    try {
      Transaction transaction = prepare(request, destination, topic, id, operation);
      active = transaction;
      publish(transaction, "QUEUED", null);
      send(transaction, 0);
    } catch (RuntimeException exception) {
      publish(topic, id, "REJECTED", exception.getMessage(), 0, 0, 0, operation, 0, 0, 0);
    }
  }

  private Transaction prepare(JsonObject request, String destination, String topic, String id,
      String operation) {
    if (!"PACKET".equals(operation) && !"COMMAND".equals(operation)
        && !"MISSION_UPLOAD".equals(operation)) {
      throw new IllegalArgumentException("Unsupported transaction operation: " + operation);
    }
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
      if (!packet.has("header") || !packet.get("header").isJsonObject()
          || !packet.has("payload") || !packet.get("payload").isJsonObject()) {
        throw new IllegalArgumentException("Each MAVLink message requires header and payload");
      }
      messages.add(packet.deepCopy());
    }
    if (messages.isEmpty()) {
      throw new IllegalArgumentException("Transaction contains no MAVLink messages");
    }
    JsonObject firstPayload = messages.getFirst().getAsJsonObject("payload");
    int system = "PACKET".equals(operation)
        ? integer(firstPayload, "target_system", targetSystem) : requiredInt(firstPayload, "target_system");
    int component = "PACKET".equals(operation)
        ? integer(firstPayload, "target_component", 0) : requiredInt(firstPayload, "target_component");
    if (system < 0 || system > 255 || component < 0 || component > 255
        || (!"PACKET".equals(operation) && system == 0)
        || (targetSystem > 0 && system != 0 && system != targetSystem)) {
      throw new IllegalArgumentException("Invalid MAVLink transaction target");
    }

    if ("PACKET".equals(operation)) {
      if (messages.size() != 1) {
        throw new IllegalArgumentException("PACKET requires exactly one MAVLink message");
      }
    } else if ("COMMAND".equals(operation)) {
      if (messages.size() != 1 || (messageId(messages.getFirst()) != COMMAND_LONG
          && messageId(messages.getFirst()) != COMMAND_INT)) {
        throw new IllegalArgumentException("COMMAND requires exactly one COMMAND_LONG/COMMAND_INT");
      }
      requiredInt(firstPayload, "command");
    } else {
      if (messageId(messages.getFirst()) != MISSION_COUNT
          || requiredInt(firstPayload, "count") != messages.size() - 1) {
        throw new IllegalArgumentException("Mission must start with matching MISSION_COUNT");
      }
      int missionType = integer(firstPayload, "mission_type", 0);
      for (int i = 1; i < messages.size(); i++) {
        JsonObject packet = messages.get(i);
        JsonObject payload = packet.getAsJsonObject("payload");
        if (messageId(packet) != MISSION_ITEM_INT
            || requiredInt(payload, "seq") != i - 1
            || requiredInt(payload, "target_system") != system
            || requiredInt(payload, "target_component") != component
            || integer(payload, "mission_type", 0) != missionType) {
          throw new IllegalArgumentException("Invalid mission item at index " + (i - 1));
        }
      }
    }
    return new Transaction(id, topic, operation, destination, List.copyOf(messages), system,
        component, integer(firstPayload, "mission_type", 0), localSystem, localComponent);
  }

  synchronized void onFrame(ProcessedFrame received) {
    Transaction transaction = active;
    if (transaction == null || !received.isValid()) {
      return;
    }
    int sourceSystem = received.getFrame().getSystemId();
    int sourceComponent = received.getFrame().getComponentId();
    if (sourceSystem != transaction.system
        || (transaction.component != 0 && sourceComponent != transaction.component)) {
      return;
    }
    int id = received.getFrame().getMessageId();
    if ("COMMAND".equals(transaction.operation)) {
      if (id != COMMAND_ACK
          || integer(received.getFields(), "command", -1)
              != requiredInt(transaction.messages.getFirst().getAsJsonObject("payload"), "command")
          || !ackTargetMatches(received, transaction)) {
        return;
      }
      int result = integer(received.getFields(), "result", -1);
      if (result == 5) {
        scheduleTimeout(transaction);
        publish(transaction, "IN_PROGRESS", null);
      } else {
        finish(transaction, result == 0 ? "COMPLETED" : "FAILED",
            result == 0 ? null : "COMMAND_ACK result " + result);
      }
      return;
    }
    if (id == MISSION_REQUEST || id == MISSION_REQUEST_INT) {
      if (!ackTargetMatches(received, transaction)
          || integer(received.getFields(), "mission_type", 0) != transaction.missionType) {
        return;
      }
      int sequence = integer(received.getFields(), "seq", -1);
      if (sequence < 0 || sequence >= transaction.messages.size() - 1) {
        finish(transaction, "FAILED", "Mission request index out of bounds: " + sequence);
        return;
      }
      // Ignore stale requests from an earlier upload until this upload starts at item zero.
      if (transaction.requested.isEmpty() && sequence != 0) {
        return;
      }
      send(transaction, sequence + 1);
      if (active == transaction) {
        transaction.requested.set(sequence);
        publish(transaction, "UPLOADING", null);
      }
    } else if (id == MISSION_ACK && ackTargetMatches(received, transaction)
        && integer(received.getFields(), "mission_type", 0) == transaction.missionType) {
      int result = integer(received.getFields(), "type", -1);
      if (result == 0 && transaction.requested.cardinality() == transaction.messages.size() - 1) {
        finish(transaction, "COMPLETED", null);
      } else {
        finish(transaction, "FAILED", result == 0
            ? "Mission accepted before every item was requested" : "MISSION_ACK result " + result);
      }
    }
  }

  private boolean ackTargetMatches(ProcessedFrame received, Transaction transaction) {
    int target = integer(received.getFields(), "target_system", 0);
    int component = integer(received.getFields(), "target_component", 0);
    return (target == 0 || transaction.localSystem == 0 || target == transaction.localSystem)
        && (component == 0 || transaction.localComponent == 0 || component == transaction.localComponent);
  }

  private void send(Transaction transaction, int index) {
    try {
      wireSender.send(transaction.messages.get(index).deepCopy(), transaction.destination);
      transaction.lastIndex = index;
      transaction.attempts++;
      if ("PACKET".equals(transaction.operation)) {
        finish(transaction, "SENT", null);
        return;
      }
      scheduleTimeout(transaction);
      publish(transaction, "MISSION_UPLOAD".equals(transaction.operation)
          ? "WAITING_FOR_MISSION_REQUEST" : "WAITING_FOR_ACK", null);
    } catch (Exception exception) {
      finish(transaction, "FAILED", "MAVLink transmission failed: " + exception.getMessage());
    }
  }

  private void scheduleTimeout(Transaction transaction) {
    if (transaction.future != null) {
      transaction.future.cancel(false);
    }
    long generation = ++transaction.generation;
    transaction.future = SimpleTaskScheduler.getInstance().schedule(
        () -> timeout(transaction, generation), DEFAULT_ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
  }

  private synchronized void timeout(Transaction transaction, long generation) {
    if (active != transaction || transaction.generation != generation) {
      return;
    }
    if (transaction.retries >= DEFAULT_MAX_RETRIES) {
      finish(transaction, "TIMED_OUT", "MAVLink response timeout");
      return;
    }
    transaction.retries++;
    publish(transaction, "RETRYING", null);
    // Mission uploads re-send MISSION_COUNT to re-establish the handshake, not an
    // unsolicited MISSION_ITEM_INT that the vehicle has not requested.
    send(transaction, "MISSION_UPLOAD".equals(transaction.operation) ? 0 : transaction.lastIndex);
  }

  private void finish(Transaction transaction, String status, String reason) {
    if (active != transaction) {
      return;
    }
    active = null;
    if (transaction.future != null) {
      transaction.future.cancel(false);
    }
    publish(transaction, status, reason);
  }

  private void publish(Transaction transaction, String status, String reason) {
    publish(transaction.topic, transaction.id, status, reason, transaction.attempts,
        transaction.retries, transaction.requested.cardinality(), transaction.operation,
        transaction.system, transaction.component, transaction.messages.size() -
            ("MISSION_UPLOAD".equals(transaction.operation) ? 1 : 0));
  }

  private void publish(String topic, String id, String status, String reason,
      int attempts, int retries, int requested, String operation,
      int system, int component, int total) {
    if (topic == null || topic.isBlank()) {
      return;
    }
    JsonObject event = new JsonObject();
    if (id != null) {
      event.addProperty("transactionId", id);
    }
    if (operation != null) {
      event.addProperty("operation", operation);
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
    statusPublisher.publish(topic, event);
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
    return object != null && object.has(name) && !object.get(name).isJsonNull()
        ? object.get(name).getAsInt() : fallback;
  }

  private static int integer(java.util.Map<String, Object> map, String name, int fallback) {
    Object value = map.get(name);
    return value instanceof Number number ? number.intValue() : fallback;
  }

  private static String text(JsonObject object, String name, String fallback) {
    return object.has(name) && !object.get(name).isJsonNull()
        ? object.get(name).getAsString() : fallback;
  }

  @Override
  public synchronized void close() {
    closed = true;
    if (active != null) {
      finish(active, "CLOSED", "MAVLink connection closed");
    }
  }

  private static final class Transaction {
    private final String id;
    private final String topic;
    private final String operation;
    private final String destination;
    private final List<JsonObject> messages;
    private final int system;
    private final int component;
    private final int missionType;
    private final int localSystem;
    private final int localComponent;
    private final BitSet requested = new BitSet();
    private ScheduledFuture<?> future;
    private long generation;
    private int lastIndex;
    private int attempts;
    private int retries;

    private Transaction(String id, String topic, String operation, String destination,
        List<JsonObject> messages, int system, int component, int missionType,
        int localSystem, int localComponent) {
      this.id = id;
      this.topic = topic;
      this.operation = operation;
      this.destination = destination;
      this.messages = messages;
      this.system = system;
      this.component = component;
      this.missionType = missionType;
      this.localSystem = localSystem;
      this.localComponent = localComponent;
    }
  }
}
