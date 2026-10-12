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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.tasks.MessageResponse;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.TransactionSubject;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ConsumerConfig;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;
import lombok.Getter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Getter
public class NamedConsumer {

  private static final int RECENT_ACK_LIMIT = 2048;
  private static final ScheduledExecutorService ACK_DEADLINES = Executors.newSingleThreadScheduledExecutor(task -> {
    Thread thread = new Thread(task, "nats-jetstream-ack-deadlines");
    thread.setDaemon(true);
    return thread;
  });

  private io.mapsmessaging.api.Session mapsSession;

  public synchronized void attachMapsSession(io.mapsmessaging.api.Session session) {
    if (mapsSession != null) {
      throw new IllegalStateException("JetStream MAPS session already attached");
    }
    mapsSession = java.util.Objects.requireNonNull(session);
  }

  private final ConsumerConfig config;
  private final String name;
  private final String streamName;
  private final Instant created;
  private final List<StreamSubscriptionInfo> streams;
  private final List<Event> events;
  private int index;
  private boolean closed;
  private long consumerSequence;
  private long terminalCount;
  private long maxDeliveryCount;
  private final Map<StreamSubscriptionInfo, Future<Response>> inFlightPolls = new HashMap<>();
  private final Map<Long, PendingDelivery> outstanding = new HashMap<>();
  private final Map<String, Integer> deliveryAttempts = new HashMap<>();
  private final Map<Long, Long> completedSequences = new LinkedHashMap<>();

  private final ScheduledExecutorService deadlineScheduler;

  public NamedConsumer(String name, String streamName, ConsumerConfig config, List<StreamSubscriptionInfo> streams) {
    this(name, streamName, config, streams, ACK_DEADLINES);
  }

  NamedConsumer(String name, String streamName, ConsumerConfig config, List<StreamSubscriptionInfo> streams,
                ScheduledExecutorService deadlineScheduler) {
    this.deadlineScheduler = java.util.Objects.requireNonNull(deadlineScheduler);
    this.name = name;
    this.streamName = streamName;
    this.config = config;
    this.streams = streams;
    created = Instant.now();
    events = new ArrayList<>();
    index = 0;
  }

  public synchronized void receive(Message event, String destinationName, Runnable callback) {
    if (!closed) {
      events.add(new Event(event, destinationName, callback));
    }
  }

  public synchronized Event poll() {
    return events.isEmpty() ? null : events.remove(0);
  }

  public synchronized int size() {
    return events.size();
  }

  public synchronized boolean isEmpty() {
    return events.isEmpty();
  }

  public synchronized MessageEvent getNextMessage() throws IOException {
    return getNextMessage(false);
  }

  public synchronized MessageEvent getNextMessage(boolean noWait) throws IOException {
    if (closed || streams.isEmpty()) {
      return null;
    }
    Integer maximum = config.getMaxAckPending();
    if (maximum != null && maximum > 0 && outstanding.size() >= maximum) {
      return null;
    }
    for (int x = 0; x < streams.size(); x++) {
      StreamSubscriptionInfo stream = streams.get(index);
      Future<Response> response = inFlightPolls.get(stream);
      if (response == null) {
        response = stream.getSubscribedEventManager().getNext();
        inFlightPolls.put(stream, response);
      }
      try {
        if (noWait && !response.isDone()) {
          index = (index + 1) % streams.size();
          continue;
        }
        Response val = noWait ? response.get() : response.get(100, TimeUnit.MILLISECONDS);
        inFlightPolls.remove(stream);
        if (val instanceof MessageResponse messageResponse) {
          MessageEvent msg = messageResponse.getResponse();
          if (msg != null) {
            index = (index + 1) % streams.size();
            return msg;
          }
        }
      } catch (TimeoutException e) {
        // An unfinished MAPS poll remains in-flight for the next pull request.
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException(e);
      } catch (Exception e) {
        inFlightPolls.remove(stream);
        throw new IOException(e);
      }
      index = (index + 1) % streams.size();
    }
    return null;
  }

  /**
   * Map wire-level JetStream sequences to the actual MAPS resource and message identifier.
   * The MAPS storage identifier is the source sequence of a single-subject stream.
   */
  public synchronized TransactionSubject recordDelivery(String destinationName, Message message) {
    if (closed || message == null || message.getIdentifier() <= 0) {
      return null;
    }
    StreamSubscriptionInfo owner = findSubscription(destinationName);
    if (owner == null) {
      return null;
    }
    String key = owner.getDestination().getFullyQualifiedNamespace() + "\u0000" + message.getIdentifier();
    int attempt = deliveryAttempts.merge(key, 1, Integer::sum);
    long sequence = ++consumerSequence;
    long timestamp = Math.max(0, message.getCreation()) * 1_000_000L;
    long remaining = 0;
    for (StreamSubscriptionInfo stream : streams) {
      remaining += Math.max(0, stream.getSubscribedEventManager().getDepth());
    }
    PendingDelivery delivery = new PendingDelivery(owner, message.getIdentifier(), key, message.getIdentifier(), attempt);
    outstanding.put(sequence, delivery);
    scheduleDeadline(sequence, delivery, effectiveAckWait());
    return new TransactionSubject(streamName, name, attempt, message.getIdentifier(),
        sequence, timestamp, Math.max(0, remaining - outstanding.size()));
  }

  private StreamSubscriptionInfo findSubscription(String destinationName) {
    if (destinationName == null) {
      return null;
    }
    String normalised = destinationName.replace('.', '/');
    String prefix = streamName + "/";
    String path = normalised.startsWith(prefix) ? normalised : prefix + normalised;
    for (StreamSubscriptionInfo info : streams) {
      if (info.getDestination() != null && path.equals(info.getDestination().getFullyQualifiedNamespace())) {
        return info;
      }
    }
    return null;
  }

  /**
   * Move a max-delivery-exhausted message past this MAPS consumer cursor.
   * The JetStream archive flag keeps the physical stream record available.
   */
  public synchronized boolean terminateIfMaxDelivered(String destinationName, long messageId) {
    int maximum = config.getMaxDeliver();
    if (maximum <= 0) {
      return false;
    }
    StreamSubscriptionInfo owner = findSubscription(destinationName);
    if (owner == null) {
      return false;
    }
    String key = owner.getDestination().getFullyQualifiedNamespace() + "\u0000" + messageId;
    if (deliveryAttempts.getOrDefault(key, 0) < maximum) {
      return false;
    }
    deliveryAttempts.remove(key);
    maxDeliveryCount++;
    owner.getSubscribedEventManager().ackReceived(messageId);
    return true;
  }

  public synchronized boolean knowsAcknowledgement(TransactionSubject subject) {
    PendingDelivery pending = outstanding.get(subject.getConsumerSequence());
    return pending != null && pending.streamSequence == subject.getStreamSequence()
        && pending.deliveries == subject.getDeliverySequence();
  }

  public synchronized boolean isAcknowledged(TransactionSubject subject) {
    return java.util.Objects.equals(completedSequences.get(subject.getConsumerSequence()), subject.getStreamSequence());
  }

  public synchronized boolean acknowledge(TransactionSubject subject, boolean terminal) {
    PendingDelivery pending = removePending(subject);
    if (pending == null) {
      return false;
    }
    deliveryAttempts.remove(pending.key);
    if (terminal) {
      terminalCount++;
    }
    completedSequences.put(subject.getConsumerSequence(), subject.getStreamSequence());
    while (completedSequences.size() > RECENT_ACK_LIMIT) {
      completedSequences.remove(completedSequences.keySet().iterator().next());
    }
    pending.stream.getSubscribedEventManager().ackReceived(pending.messageId);
    return true;
  }

  public synchronized boolean negativeAcknowledgement(TransactionSubject subject, long delayNanos) {
    PendingDelivery pending = outstanding.get(subject.getConsumerSequence());
    if (pending == null || !knowsAcknowledgement(subject) || delayNanos < 0) {
      return false;
    }
    cancelDeadline(pending);
    if (delayNanos == 0) {
      outstanding.remove(subject.getConsumerSequence());
      pending.stream.getSubscribedEventManager().rollbackReceived(pending.messageId);
    } else {
      pending.deadline = deadlineScheduler.schedule(() -> expire(subject.getConsumerSequence(), pending),
          delayNanos, TimeUnit.NANOSECONDS);
    }
    return true;
  }

  public synchronized boolean workingProgress(TransactionSubject subject) {
    PendingDelivery pending = outstanding.get(subject.getConsumerSequence());
    if (pending == null || !knowsAcknowledgement(subject)) {
      return false;
    }
    scheduleDeadline(subject.getConsumerSequence(), pending, effectiveAckWait());
    return true;
  }

  public synchronized void rollbackUnregistered(String destinationName, long messageId) {
    StreamSubscriptionInfo owner = findSubscription(destinationName);
    if (owner != null) {
      owner.getSubscribedEventManager().rollbackReceived(messageId);
    }
  }

  public synchronized void rollbackUnsent(TransactionSubject subject) {
    PendingDelivery pending = removePending(subject);
    if (pending != null) {
      pending.stream.getSubscribedEventManager().rollbackReceived(pending.messageId);
    }
  }

  private PendingDelivery removePending(TransactionSubject subject) {
    if (!knowsAcknowledgement(subject)) {
      return null;
    }
    PendingDelivery pending = outstanding.remove(subject.getConsumerSequence());
    cancelDeadline(pending);
    return pending;
  }

  private long effectiveAckWait() {
    return config.getAckWait() <= 0 ? TimeUnit.SECONDS.toNanos(30) : config.getAckWait();
  }

  private void scheduleDeadline(long sequence, PendingDelivery delivery, long durationNanos) {
    cancelDeadline(delivery);
    delivery.deadline = deadlineScheduler.schedule(() -> expire(sequence, delivery),
        durationNanos, TimeUnit.NANOSECONDS);
  }

  private void expire(long sequence, PendingDelivery expected) {
    synchronized (this) {
      if (outstanding.remove(sequence, expected)) {
        expected.deadline = null;
        expected.stream.getSubscribedEventManager().rollbackReceived(expected.messageId);
      }
    }
  }

  private void cancelDeadline(PendingDelivery delivery) {
    if (delivery != null && delivery.deadline != null) {
      delivery.deadline.cancel(false);
      delivery.deadline = null;
    }
  }

  public synchronized void close() {
    closed = true;
    for (Future<Response> future : inFlightPolls.values()) {
      future.cancel(false);
    }
    inFlightPolls.clear();
    for (PendingDelivery delivery : outstanding.values()) {
      cancelDeadline(delivery);
      delivery.stream.getSubscribedEventManager().rollbackReceived(delivery.messageId);
    }
    outstanding.clear();
    events.clear();
  }

  public synchronized int getOutstandingCount() {
    return outstanding.size();
  }

  public int getAckPendingCount() {
    int pending = 0;
    for (StreamSubscriptionInfo stream : streams) {
      pending += Math.max(0, stream.getSubscribedEventManager().getPending());
    }
    return pending;
  }

  private static final class PendingDelivery {
    private final StreamSubscriptionInfo stream;
    private final long messageId;
    private final String key;
    private final long streamSequence;
    private final int deliveries;
    private ScheduledFuture<?> deadline;

    private PendingDelivery(StreamSubscriptionInfo stream, long messageId, String key, long streamSequence, int deliveries) {
      this.stream = stream;
      this.messageId = messageId;
      this.key = key;
      this.streamSequence = streamSequence;
      this.deliveries = deliveries;
    }
  }
}
