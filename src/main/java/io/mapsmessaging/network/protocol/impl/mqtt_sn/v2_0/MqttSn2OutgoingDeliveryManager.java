/*
 *
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2AckCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PublishCodec;
import io.mapsmessaging.storage.delivery.PacketIdStore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

/** Tracks the server's single outstanding QoS 1/2 request as required by CSD01. */
public final class MqttSn2OutgoingDeliveryManager {

  private enum Stage { WAIT_PUBACK, WAIT_PUBREC, WAIT_PUBCOMP }

  // Internal queue entry is not used as a value-equality key; callback identity matters.
  @SuppressWarnings("java:S6218")
  private record Delivery(String subscriptionKey, long messageId, String topic, byte[] payload, int qos, boolean retain, Runnable completion) {
    private Delivery {
      payload = payload.clone();
    }
  }

  private static final long DEFAULT_RETRY_INTERVAL_MILLIS = 10_000;
  private static final int DEFAULT_MAXIMUM_RETRIES = 3;

  private final Deque<Delivery> pending = new ArrayDeque<>();
  private final Map<PacketIdStore.Delivery, Delivery> recovered = new LinkedHashMap<>();
  private final Set<PacketIdStore.Delivery> brokerAcknowledged = new HashSet<>();
  private final Set<PacketIdStore.Delivery> restoredIdentities = new HashSet<>();
  private final Set<PacketIdStore.Delivery> completedRecovered = new HashSet<>();
  private final long retryIntervalMillis;
  private final int maximumRetries;
  private PacketIdStore packetIds = PacketIdStore.memory(65535);
  private long anonymousMessageId;
  private InFlight inFlight;
  private CompletableFuture<ByteBuffer> brokerAckBarrier;
  private long connectionGeneration;

  private static final class InFlight {
    private Delivery delivery;
    private final int packetIdentifier;
    private Stage stage;
    private ByteBuffer lastFrame;
    private long lastSentAt;
    private int retries;
    private boolean retryExhausted;

    private InFlight(Delivery delivery, int packetIdentifier, boolean duplicate, int savedStage, long now) {
      this.delivery = delivery;
      this.packetIdentifier = packetIdentifier;
      stage = delivery.qos() == 1 ? Stage.WAIT_PUBACK : Stage.WAIT_PUBREC;
      lastSentAt = now;
      if (savedStage == 1) {
        stage = Stage.WAIT_PUBCOMP;
        lastFrame = encodePubRel(packetIdentifier);
      } else {
        lastFrame = encodePublish(delivery, packetIdentifier, duplicate);
      }
    }
  }

  public MqttSn2OutgoingDeliveryManager() {
    this(DEFAULT_RETRY_INTERVAL_MILLIS, DEFAULT_MAXIMUM_RETRIES);
  }

  public MqttSn2OutgoingDeliveryManager(long retryIntervalMillis, int maximumRetries) {
    if (retryIntervalMillis < 1 || maximumRetries < 0) {
      throw new IllegalArgumentException("Invalid MQTT-SN retry configuration");
    }
    this.retryIntervalMillis = retryIntervalMillis;
    this.maximumRetries = maximumRetries;
  }

  /** Attaches session-owned state before recovered engine deliveries are resumed. */
  public synchronized void attach(PacketIdStore store) throws IOException {
    if (inFlight != null || !pending.isEmpty()) {
      throw new IllegalStateException("Cannot replace an active delivery store");
    }
    packetIds = Objects.requireNonNull(store, "store");
    connectionGeneration++;
    brokerAckBarrier = null;
    recovered.clear();
    brokerAcknowledged.clear();
    restoredIdentities.clear();
    completedRecovered.clear();
    for (Map.Entry<Integer, PacketIdStore.Delivery> entry : store.outstanding().entrySet()) {
      if (store.getStage(entry.getKey()) == 1) {
        PacketIdStore.Delivery identity = entry.getValue();
        restoredIdentities.add(identity);
        recovered.put(identity, new Delivery(identity.topic(), identity.messageId(), "", new byte[0], 2, false, null));
      }
    }
  }

  /** Replays a recovered PUBREL even when its broker interest has expired. */
  public synchronized ByteBuffer recover(long now) throws IOException {
    return inFlight == null ? startNext(now) : null;
  }

  /** Restored broker interests must finish their engine ACK before any PUBREL leaves this connection. */
  public CompletableFuture<ByteBuffer> recoverAfterBrokerAck(Function<PacketIdStore.Delivery, CompletableFuture<Void>> acknowledge, long now) throws IOException {
    java.util.List<CompletableFuture<Void>> completions = new java.util.ArrayList<>();
    long generation;
    synchronized (this) {
      generation = connectionGeneration;
      for (PacketIdStore.Delivery delivery : restoredIdentities) completions.add(acknowledge.apply(delivery));
    }
    CompletableFuture<ByteBuffer> barrier = CompletableFuture.allOf(completions.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
      synchronized (this) {
        if (generation != connectionGeneration) {
          return null;
        }
        brokerAcknowledged.addAll(restoredIdentities);
        recovered.replaceAll((identity, delivery) -> withCompletion(delivery, null));
        try {
          return recover(now);
        } catch (IOException failure) {
          throw new CompletionException(failure);
        }
      }
    });
    synchronized (this) { brokerAckBarrier = barrier; }
    return barrier;
  }

  /** Normal QoS2 delivery uses the same engine ACK barrier after persisting its PUBREL stage. */
  public CompletableFuture<ByteBuffer> acknowledgeAfterBrokerAck(MqttSn2AckCodec.Ack ack, long now,
      Function<PacketIdStore.Delivery, CompletableFuture<Void>> acknowledge) throws IOException {
    PacketIdStore.Delivery identity;
    long generation;
    CompletableFuture<ByteBuffer> barrier;
    synchronized (this) {
      if (brokerAckBarrier != null && (!brokerAckBarrier.isDone() || brokerAckBarrier.isCompletedExceptionally())) {
        if (ack.type() == MqttSn2PacketType.PUBREC && inFlight != null && ack.packetIdentifier() == inFlight.packetIdentifier) {
          return brokerAckBarrier;
        }
        throw new IOException("MQTT-SN broker acknowledgement is still pending");
      }
      boolean requiresBrokerAck = ack.type() == MqttSn2PacketType.PUBREC && inFlight != null
          && ack.packetIdentifier() == inFlight.packetIdentifier
          && (inFlight.stage == Stage.WAIT_PUBCOMP || inFlight.stage == Stage.WAIT_PUBREC && (ack.reasonCode() == null || ack.reasonCode() < 0x80));
      if (requiresBrokerAck) {
        identity = identity(inFlight.delivery);
        generation = connectionGeneration;
        // Publish the gate before the stage changes or engine submission can block.
        barrier = new CompletableFuture<>();
        brokerAckBarrier = barrier;
      } else {
        identity = null;
        generation = 0;
        barrier = null;
      }
    }
    if (barrier == null) {
      return CompletableFuture.completedFuture(acknowledge(ack, now));
    }
    try {
      ByteBuffer response = acknowledge(ack, now);
      acknowledge.apply(identity).whenComplete((ignored, failure) -> {
        ByteBuffer ready = null;
        synchronized (this) {
          if (generation != connectionGeneration || brokerAckBarrier != barrier || inFlight == null
              || inFlight.packetIdentifier != ack.packetIdentifier() || !identity(inFlight.delivery).equals(identity)) {
            failure = null;
          } else if (failure == null) {
            brokerAcknowledged.add(identity);
            inFlight.delivery = withCompletion(inFlight.delivery, null);
            ready = response;
          }
        }
        if (failure == null) barrier.complete(ready);
        else barrier.completeExceptionally(failure);
      });
    } catch (IOException failure) {
      barrier.completeExceptionally(failure);
      throw failure;
    } catch (RuntimeException failure) {
      barrier.completeExceptionally(failure);
    }
    return barrier;
  }

  /** Releases connection-local callbacks without deleting session-owned protocol state. */
  public synchronized void disconnect() {
    connectionGeneration++;
    pending.clear();
    recovered.clear();
    brokerAcknowledged.clear();
    restoredIdentities.clear();
    completedRecovered.clear();
    inFlight = null;
    brokerAckBarrier = null;
  }

  /** QoS changes on resubscription do not abandon an already acknowledged QoS2 PUBLISH. */
  public synchronized boolean bindRecoveredCompletion(String subscriptionKey, long messageId, Runnable completion) {
    PacketIdStore.Delivery identity = new PacketIdStore.Delivery(subscriptionKey, messageId);
    if (completedRecovered.remove(identity)) {
      restoredIdentities.remove(identity);
      if (!brokerAcknowledged.remove(identity) && completion != null) completion.run();
      return true;
    }
    if (inFlight != null && identity(inFlight.delivery).equals(identity) && inFlight.stage == Stage.WAIT_PUBCOMP) {
      inFlight.delivery = withCompletion(inFlight.delivery, brokerAcknowledged.contains(identity) ? null : completion);
      return true;
    }
    Delivery waiting = recovered.get(identity);
    if (waiting == null) {
      return false;
    }
    recovered.put(identity, withCompletion(waiting, brokerAcknowledged.contains(identity) ? null : completion));
    return true;
  }

  private static Delivery withCompletion(Delivery delivery, Runnable completion) {
    return new Delivery(delivery.subscriptionKey(), delivery.messageId(), delivery.topic(), delivery.payload(), delivery.qos(), delivery.retain(), completion);
  }

  public synchronized boolean hasDeliveryMapping(String subscriptionKey, long messageId) throws IOException {
    return packetIds.getPacketId(subscriptionKey, messageId) != null;
  }

  public synchronized int getOriginalQos(String subscriptionKey, long messageId) throws IOException {
    Integer identifier = packetIds.getPacketId(subscriptionKey, messageId);
    return identifier == null ? 0 : packetIds.getQos(identifier);
  }

  /** The engine already retired its identity; never clear a possibly reused wire identifier here. */
  public synchronized ByteBuffer retireDelivery(String subscriptionKey, long messageId, long now) throws IOException {
    return retire(subscriptionKey, messageId, now);
  }

  public synchronized ByteBuffer retireDeliveries(String subscriptionKey, long now) throws IOException {
    return retire(subscriptionKey, null, now);
  }

  private ByteBuffer retire(String subscriptionKey, Long messageId, long now) throws IOException {
    if (messageId != null && packetIds.getPacketId(subscriptionKey, messageId) != null) {
      return null;
    }
    Set<PacketIdStore.Delivery> live = new HashSet<>(packetIds.outstanding().values());
    java.util.function.Predicate<PacketIdStore.Delivery> matches = delivery -> delivery.topic().equals(subscriptionKey)
        && (messageId == null || delivery.messageId() == messageId) && !live.contains(delivery);
    pending.removeIf(delivery -> matches.test(identity(delivery)));
    recovered.keySet().removeIf(matches);
    brokerAcknowledged.removeIf(matches);
    restoredIdentities.removeIf(matches);
    completedRecovered.removeIf(matches);
    if (inFlight != null && matches.test(identity(inFlight.delivery))) {
      inFlight = null;
      brokerAckBarrier = null;
    }
    return inFlight == null ? startNext(now) : null;
  }

  /** Compatibility entry point for callers with no recoverable engine identity. */
  public synchronized ByteBuffer enqueue(String topic, byte[] payload, int qos, boolean retain, Runnable completion, long now) throws IOException {
    return enqueue(topic, ++anonymousMessageId, topic, payload, qos, retain, completion, now);
  }

  /** Returns the first request when the new delivery can start immediately. */
  public synchronized ByteBuffer enqueue(String subscriptionKey, long messageId, String topic, byte[] payload, int qos, boolean retain,
      Runnable completion, long now) throws IOException {
    int originalQos = getOriginalQos(subscriptionKey, messageId);
    if (originalQos != 0) qos = originalQos;
    if (qos < 1 || qos > 2) {
      throw new IllegalArgumentException("Only QoS 1 and 2 use the acknowledgement flow");
    }
    Delivery delivery = new Delivery(Objects.requireNonNull(subscriptionKey, "subscriptionKey"), messageId, Objects.requireNonNull(topic, "topic"),
        Objects.requireNonNull(payload, "payload"), qos, retain, completion);
    PacketIdStore.Delivery identity = identity(delivery);
    if (completedRecovered.remove(identity)) {
      restoredIdentities.remove(identity);
      if (!brokerAcknowledged.remove(identity) && completion != null) completion.run();
      return null;
    }
    if (inFlight != null && identity(inFlight.delivery).equals(identity) && inFlight.stage == Stage.WAIT_PUBCOMP) {
      inFlight.delivery = brokerAcknowledged.contains(identity) ? withCompletion(delivery, null) : delivery;
      return null;
    }
    if (recovered.containsKey(identity)) {
      recovered.put(identity, brokerAcknowledged.contains(identity) ? withCompletion(delivery, null) : delivery);
      return inFlight == null ? startNext(now) : null;
    }
    if (inFlight != null || !recovered.isEmpty()) {
      pending.addLast(delivery);
      return inFlight == null ? startNext(now) : null;
    }
    return start(delivery, now);
  }

  /** Applies a peer acknowledgement and starts the next queued delivery when possible. */
  // QoS1 and QoS2 acknowledgement transitions are intentionally explicit.
  @SuppressWarnings({"java:S3776", "java:S1871"})
  public ByteBuffer acknowledge(MqttSn2AckCodec.Ack ack, long now) throws IOException {
    Runnable completion = null;
    ByteBuffer response;
    try {
      synchronized (this) {
        if (inFlight == null || ack.packetIdentifier() != inFlight.packetIdentifier) {
          throw new IOException("Unknown MQTT-SN 2.0 outgoing Packet Identifier");
        }
        if (inFlight.stage == Stage.WAIT_PUBACK && ack.type() == MqttSn2PacketType.PUBACK) {
          completion = finishCurrent();
          response = startNext(now);
        } else if (inFlight.stage == Stage.WAIT_PUBREC && ack.type() == MqttSn2PacketType.PUBREC) {
          if (ack.reasonCode() != null && ack.reasonCode() >= 0x80) {
            completion = finishCurrent();
            response = startNext(now);
          } else {
            packetIds.setStage(inFlight.packetIdentifier, 1);
            packetIds.flush();
            inFlight.stage = Stage.WAIT_PUBCOMP;
            inFlight.lastFrame = encodePubRel(inFlight.packetIdentifier);
            inFlight.lastSentAt = now;
            inFlight.retries = 0;
            response = duplicate(inFlight.lastFrame);
          }
        } else if (inFlight.stage == Stage.WAIT_PUBCOMP && ack.type() == MqttSn2PacketType.PUBREC) {
          inFlight.lastSentAt = now;
          response = duplicate(inFlight.lastFrame);
        } else if (inFlight.stage == Stage.WAIT_PUBCOMP && ack.type() == MqttSn2PacketType.PUBCOMP) {
          completion = finishCurrent();
          response = startNext(now);
        } else {
          throw new IOException("Unexpected MQTT-SN 2.0 acknowledgement " + ack.type());
        }
      }
      return response;
    } finally {
      if (completion != null) completion.run();
    }
  }

  /** Returns the exact outstanding request for retransmission, or null when not due. */
  public synchronized ByteBuffer retryExpired(long now) {
    if (brokerAckBarrier != null && (!brokerAckBarrier.isDone() || brokerAckBarrier.isCompletedExceptionally())) {
      return null;
    }
    if (inFlight == null || now - inFlight.lastSentAt < retryIntervalMillis) {
      return null;
    }
    if (inFlight.retries >= maximumRetries) {
      inFlight.retryExhausted = true;
      return null;
    }
    inFlight.retries++;
    inFlight.lastSentAt = now;
    if (inFlight.stage == Stage.WAIT_PUBACK || inFlight.stage == Stage.WAIT_PUBREC) {
      return MqttSn2PublishCodec.encode(new MqttSn2PublishCodec.Publish(false,
          inFlight.delivery.qos(), true, inFlight.delivery.retain(), 3,
          inFlight.packetIdentifier, inFlight.delivery.topic(), 0, inFlight.delivery.payload()));
    }
    return duplicate(inFlight.lastFrame);
  }

  public synchronized boolean isRetryExhausted() {
    return inFlight != null && inFlight.retryExhausted;
  }

  public synchronized boolean hasInFlightDelivery() {
    return inFlight != null;
  }

  private ByteBuffer startNext(long now) throws IOException {
    if (!recovered.isEmpty()) {
      Map.Entry<PacketIdStore.Delivery, Delivery> first = recovered.entrySet().iterator().next();
      ByteBuffer frame = start(first.getValue(), now);
      recovered.remove(first.getKey());
      return frame;
    }
    Delivery next = pending.peekFirst();
    if (next == null) {
      return null;
    }
    ByteBuffer frame = start(next, now);
    pending.removeFirst();
    return frame;
  }

  private ByteBuffer start(Delivery delivery, long now) throws IOException {
    PacketIdStore.Allocation allocation = packetIds.allocate(delivery.subscriptionKey(), delivery.messageId());
    int savedStage = packetIds.getStage(allocation.packetId());
    int originalQos = packetIds.getQos(allocation.packetId());
    if (originalQos == 0) packetIds.setQos(allocation.packetId(), delivery.qos());
    else if (originalQos != delivery.qos()) {
      delivery = new Delivery(delivery.subscriptionKey(), delivery.messageId(), delivery.topic(), delivery.payload(), originalQos, delivery.retain(), delivery.completion());
    }
    packetIds.flush();
    inFlight = new InFlight(delivery, allocation.packetId(), allocation.existing(), savedStage, now);
    return duplicate(inFlight.lastFrame);
  }

  private Runnable finishCurrent() throws IOException {
    Runnable completion = inFlight.delivery.completion();
    packetIds.clear(inFlight.packetIdentifier);
    packetIds.flush();
    PacketIdStore.Delivery identity = identity(inFlight.delivery);
    if (restoredIdentities.contains(identity)) {
      if (completion == null) completedRecovered.add(identity);
      else restoredIdentities.remove(identity);
    } else brokerAcknowledged.remove(identity);
    inFlight = null;
    return completion;
  }

  private static PacketIdStore.Delivery identity(Delivery delivery) {
    return new PacketIdStore.Delivery(delivery.subscriptionKey(), delivery.messageId());
  }

  private static ByteBuffer encodePublish(Delivery delivery, int packetIdentifier, boolean duplicate) {
    return MqttSn2PublishCodec.encode(new MqttSn2PublishCodec.Publish(false,
        delivery.qos(), duplicate, delivery.retain(), 3, packetIdentifier,
        delivery.topic(), 0, delivery.payload()));
  }

  private static ByteBuffer encodePubRel(int packetIdentifier) {
    return MqttSn2AckCodec.encode(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREL, packetIdentifier, null));
  }

  // Encapsulation helper deliberately stays beside the QoS state-machine implementation.
  @SuppressWarnings("java:S3398")
  private static ByteBuffer duplicate(ByteBuffer frame) {
    return frame.asReadOnlyBuffer();
  }
}
