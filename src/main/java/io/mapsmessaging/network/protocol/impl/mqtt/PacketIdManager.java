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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.storage.delivery.PacketIdStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

public class PacketIdManager {

  static final int MAX_PACKET_IDENTIFIER = 0xffff;

  private final Map<Integer, PacketIdentifierMap> outstandingPacketId;
  private final Map<SubscribedEventManager, Integer> reservations;
  private final Queue<SubscribedEventManager> waiters;
  private final Set<SubscribedEventManager> waiting;
  private final int maxPacketIdentifier;

  private PacketIdStore deliveryStore;
  private final Set<Integer> existingPacketIds = new HashSet<>();
  private final Map<Integer, Integer> runtimeQos = new HashMap<>();
  private final Set<Integer> runtimePubComp = new HashSet<>();
  private final Set<Integer> controlIds = new HashSet<>();

  private final Map<Integer, PacketIdStore.Delivery> deliveryIdentities = new HashMap<>();
  private final Set<PacketIdStore.Delivery> completedRecovery = new LinkedHashSet<>();

  private int packetId;
  private int maximumOutstanding;
  private int reservedSlots;

  public PacketIdManager() {
    this(MAX_PACKET_IDENTIFIER);
  }

  PacketIdManager(int maxPacketIdentifier) {
    this.maxPacketIdentifier = Math.clamp(maxPacketIdentifier, 1, MAX_PACKET_IDENTIFIER);
    outstandingPacketId = new TreeMap<>();
    reservations = new IdentityHashMap<>();
    waiters = new ArrayDeque<>();
    waiting = Collections.newSetFromMap(new IdentityHashMap<>());
    maximumOutstanding = this.maxPacketIdentifier;
  }

  public synchronized void close() {
    if (deliveryStore != null) {
      for (int identifier : controlIds) {
        try { deliveryStore.releaseReservedPacketId(identifier); }
        catch (IllegalStateException closedStore) {
          /* A retired endpoint may close after its session owner. */
        }
      }
    }
    runtimeQos.clear();
    runtimePubComp.clear();
    deliveryIdentities.clear();
    completedRecovery.clear();
    controlIds.clear();
    existingPacketIds.clear();
    outstandingPacketId.clear();
    reservations.clear();
    waiters.clear();
    waiting.clear();
    reservedSlots = 0;
    notifyAll();
  }

  /** Borrow session state; manager close must not close or clear persistent deliveries. */
  public synchronized void bindStore(PacketIdStore store) {
    if (store == null) {
      return;
    }
    if (deliveryStore == store) {
      return;
    }
    if (deliveryStore != null || !outstandingPacketId.isEmpty()) {
      throw new IllegalStateException("Delivery store must attach before sending");
    }
    deliveryStore = store;
    try {
      for (Map.Entry<Integer, PacketIdStore.Delivery> entry : store.outstanding().entrySet()) {
        if (store.getStage(entry.getKey()) == 1) {
          outstandingPacketId.put(entry.getKey(), new PacketIdentifierMap(entry.getKey(), null, entry.getValue().messageId()));
          existingPacketIds.add(entry.getKey());
          deliveryIdentities.put(entry.getKey(), entry.getValue());
        }
      }
    } catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
  }

  public static String deliveryKey(SubscribedEventManager subscription) {
    return subscription.getDeliveryKey();
  }

  public synchronized Integer getPacketIdentifier(SubscribedEventManager subscription, long messageId) {
    if (deliveryStore != null) {
      try { return deliveryStore.getPacketId(deliveryKey(subscription), messageId); }
      catch (IOException failure) {
        throw new UncheckedIOException(failure);
      }
    }
    for (Map.Entry<Integer, PacketIdentifierMap> entry : outstandingPacketId.entrySet()) {
      PacketIdentifierMap mapping = entry.getValue();
      if (mapping.getSubscription() == subscription && mapping.getMessageId() == messageId) {
        return entry.getKey();
      }
    }
    return null;
  }

  public synchronized PacketIdStore.Delivery getDelivery(int identifier) {
    return deliveryIdentities.get(identifier);
  }

  public synchronized boolean hasDeliveryStore() { return deliveryStore != null; }

  public synchronized int getPacketQos(int identifier) {
    if (deliveryStore == null) {
      return runtimeQos.getOrDefault(identifier, 0);
    }
    try { return deliveryStore.getQos(identifier); }
    catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
  }

  public synchronized void setPacketQos(int identifier, int qos) {
    if (deliveryStore == null) { runtimeQos.put(identifier, qos); return; }
    try { deliveryStore.setQos(identifier, qos); deliveryStore.flush(); }
    catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
  }

  /** Reconciles broker interest that arrives after a restored PUBREL already completed. */
  public synchronized boolean consumeCompletedRecovery(SubscribedEventManager subscription, long messageId) {
    return completedRecovery.remove(new PacketIdStore.Delivery(deliveryKey(subscription), messageId));
  }

  public synchronized boolean tryAcquireSendSlot(SubscribedEventManager subscription, long messageId) {
    if (subscription != null) {
      Integer existing = getPacketIdentifier(subscription, messageId);
      if ((existing != null && outstandingPacketId.containsKey(existing))
          || completedRecovery.contains(new PacketIdStore.Delivery(deliveryKey(subscription), messageId))) return true;
    }
    return tryAcquireSendSlot(subscription);
  }

  /** Called after engine retirement, outside the store monitor; never clears a reused identifier. */
  public void retireDelivery(String key, long messageId) { retireDeliveries(key, messageId); }

  public void retireDeliveries(String key) { retireDeliveries(key, null); }

  private void retireDeliveries(String key, Long messageId) {
    SubscribedEventManager wake;
    synchronized (this) {
      List<Integer> identifiers = new ArrayList<>();
      for (Map.Entry<Integer, PacketIdStore.Delivery> entry : deliveryIdentities.entrySet()) {
        PacketIdStore.Delivery identity = entry.getValue();
        if (identity.topic().equals(key) && (messageId == null || identity.messageId() == messageId)) {
          try {
            Integer current = deliveryStore == null ? null : deliveryStore.getPacketId(identity.topic(), identity.messageId());
            if (current == null || !current.equals(entry.getKey())) identifiers.add(entry.getKey());
          } catch (IOException retiredStore) {
            /* Retirement may race closure of the old endpoint's store. */
          }
        }
      }
      for (int identifier : identifiers) {
        runtimeQos.remove(identifier);
        runtimePubComp.remove(identifier);
        deliveryIdentities.remove(identifier);
        outstandingPacketId.remove(identifier);
        existingPacketIds.remove(identifier);
      }
      wake = identifiers.isEmpty() ? null : grantNextWaiter();
      notifyAll();
    }
    resume(wake);
  }

  public synchronized boolean isExistingPacketId(int identifier) { return existingPacketIds.contains(identifier); }

  public synchronized boolean isAwaitingPubComp(int identifier) {
    if (deliveryStore == null) {
      return runtimePubComp.contains(identifier);
    }
    try { return deliveryStore.getStage(identifier) == 1; }
    catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
  }

  public synchronized void markAwaitingPubComp(int identifier) {
    if (deliveryStore == null) { runtimePubComp.add(identifier); return; }
    try { deliveryStore.setStage(identifier, 1); deliveryStore.flush(); }
    catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
  }

  public synchronized List<Integer> getAwaitingPubCompPacketIds() {
    return outstandingPacketId.keySet().stream().filter(this::isAwaitingPubComp).toList();
  }

  public void discard(SubscribedEventManager subscription, long messageId) {
    String key = null;
    synchronized (this) {
      for (Map.Entry<Integer, PacketIdentifierMap> entry : outstandingPacketId.entrySet()) {
        PacketIdentifierMap mapping = entry.getValue();
        if (mapping.getSubscription() == subscription && mapping.getMessageId() == messageId) {
          PacketIdStore.Delivery identity = deliveryIdentities.get(entry.getKey());
          if (identity != null) key = identity.topic();
          else if (deliveryStore == null) { completePacketId(entry.getKey()); return; }
          break;
        }
      }
      if (deliveryStore == null) {
        return;
      }
      if (key == null && subscription.getContext() != null) key = deliveryKey(subscription);
      if (key == null) {
        return;
      }
      try {
        if (!deliveryStore.clearPendingDelivery(key, messageId)) {
          return;
        }
        deliveryStore.flush();
      } catch (IOException failure) {
        throw new UncheckedIOException(failure);
      }
    }
    retireDelivery(key, messageId);
  }

  public synchronized void setMaximumOutstanding(int maximumOutstanding) {
    this.maximumOutstanding = Math.clamp(maximumOutstanding, 1, maxPacketIdentifier);
  }

  public synchronized boolean tryAcquireSendSlot(SubscribedEventManager subscription) {
    if (subscription == null) {
      return hasPublishCapacity();
    }

    if (reservations.getOrDefault(subscription, 0) > 0) {
      return true;
    }

    if (hasPublishCapacity()) {
      reserve(subscription);
      return true;
    }

    if (waiting.add(subscription)) {
      waiters.add(subscription);
    }
    return false;
  }

  public void releaseSendSlot(SubscribedEventManager subscription) {
    SubscribedEventManager wake;
    synchronized (this) {
      if (subscription != null && waiting.remove(subscription)) {
        waiters.remove(subscription);
      }
      boolean released = releaseReservation(subscription);
      wake = released ? grantNextWaiter() : null;
      notifyAll();
    }
    resume(wake);
  }

  public void releaseUnusedSendSlot(SubscribedEventManager subscription) {
    SubscribedEventManager wake = null;
    synchronized (this) {
      if (releaseReservation(subscription)) {
        wake = grantNextWaiter();
        notifyAll();
      }
    }
    resume(wake);
  }

  public int nextPacketIdentifier(SubscribedEventManager subscription, long messageId) {
    return nextPacketIdentifier(subscription, messageId, 0);
  }

  /** Allocation and original QoS share one durable flush before transmission. */
  public int nextPacketIdentifier(SubscribedEventManager subscription, long messageId, int qos) {
    synchronized (this) {
      if (deliveryStore != null) {
        try {
          String key = deliveryKey(subscription);
          Integer existing = deliveryStore.getPacketId(key, messageId);
          boolean reserved = consumeReservation(subscription);
          boolean active = existing != null && outstandingPacketId.containsKey(existing);
          while (!active && !reserved && !hasPublishCapacity()) {
            try {
              wait();
            } catch (InterruptedException interrupted) {
              Thread.currentThread().interrupt();
              throw new PacketIdentifierExhaustedException();
            }
          }
          PacketIdStore.Allocation allocation = deliveryStore.allocate(key, messageId);
          if (qos > 0) deliveryStore.setQos(allocation.packetId(), qos);
          deliveryStore.flush();
          int identifier = allocation.packetId();
          if (allocation.existing()) existingPacketIds.add(identifier); else existingPacketIds.remove(identifier);
          PacketIdStore.Delivery identity = new PacketIdStore.Delivery(key, messageId);
          List<Integer> stale = deliveryIdentities.entrySet().stream().filter(entry -> entry.getKey() != identifier && entry.getValue().equals(identity))
              .map(Map.Entry::getKey).toList();
          for (int retired : stale) {
            deliveryIdentities.remove(retired); outstandingPacketId.remove(retired); existingPacketIds.remove(retired);
          }
          outstandingPacketId.put(identifier, new PacketIdentifierMap(identifier, subscription, messageId));
          deliveryIdentities.put(identifier, new PacketIdStore.Delivery(key, messageId));
          return identifier;
        } catch (IOException failure) {
          throw new UncheckedIOException(failure);
        }
      }
      boolean reserved = consumeReservation(subscription);
      while (!reserved && !hasPublishCapacity()) {
        try {
          wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new PacketIdentifierExhaustedException();
        }
      }
      while (!hasAvailablePacketIdentifier()) {
        try {
          wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new PacketIdentifierExhaustedException();
        }
      }
      int retVal = nextAvailablePacketIdentifier();
      PacketIdentifierMap state = new PacketIdentifierMap(retVal, subscription, messageId);
      outstandingPacketId.put(retVal, state);
      if (qos > 0) runtimeQos.put(retVal, qos);
      return retVal;
    }
  }

  public synchronized int nextPacketIdentifier() {
    if (deliveryStore != null) {
      try { int identifier = deliveryStore.reservePacketId(); controlIds.add(identifier); return identifier; }
      catch (IOException failure) {
        throw new UncheckedIOException(failure);
      }
    }
    while (!hasAvailablePacketIdentifier()) {
      try {
        wait();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new PacketIdentifierExhaustedException();
      }
    }
    return nextAvailablePacketIdentifier();
  }

  public synchronized int nextControlPacketIdentifier() {
    int identifier = nextPacketIdentifier();
    outstandingPacketId.put(identifier, new PacketIdentifierMap(identifier, null, -1));
    return identifier;
  }

  public synchronized boolean hasAvailablePacketIdentifier() {
    return outstandingPacketId.size() < maxPacketIdentifier;
  }

  public synchronized PacketIdentifierMap receivedPacket(int id) {
    return outstandingPacketId.get(id);
  }

  public PacketIdentifierMap completePacketId(int id) {
    PacketIdentifierMap result;
    SubscribedEventManager wake = null;
    synchronized (this) {
      result = outstandingPacketId.get(id);
      if (result == null && !controlIds.contains(id)) {
        return null;
      }
      if (deliveryStore != null) {
        try {
          if (controlIds.contains(id)) {
            try { deliveryStore.releaseReservedPacketId(id); }
            catch (IllegalStateException closedStore) {
              /* Connection-scoped reservation was already released by owner close. */
            }
          }
          else {
            PacketIdStore.Delivery identity = deliveryIdentities.get(id);
            boolean awaitingPubComp = deliveryStore.getStage(id) == 1;
            if (identity != null && Integer.valueOf(id).equals(deliveryStore.getPacketId(identity.topic(), identity.messageId()))
                && deliveryStore.clear(identity.topic(), identity.messageId())) {
              deliveryStore.flush();
              if (awaitingPubComp) {
                completedRecovery.add(identity);
                if (completedRecovery.size() > maxPacketIdentifier) completedRecovery.remove(completedRecovery.iterator().next());
              }
            }
          }
        } catch (IOException failure) {
          throw new UncheckedIOException(failure);
        }
      }
      runtimeQos.remove(id);
      runtimePubComp.remove(id);
      deliveryIdentities.remove(id);
      controlIds.remove(id);
      existingPacketIds.remove(id);
      outstandingPacketId.remove(id);
      if (result != null) {
        wake = grantNextWaiter();
        notifyAll();
      }
    }
    resume(wake);
    return result;
  }

  public synchronized int size() {
    return outstandingPacketId.size();
  }

  public boolean scanForTimeOut() {
    long timeout = System.currentTimeMillis() - 20000;
    List<PacketIdentifierMap> tmp;
    synchronized (this) {
      tmp = new ArrayList<>(outstandingPacketId.values());
    }
    return tmp.stream().anyMatch(map -> map.getTime() < timeout);
  }

  private boolean hasPublishCapacity() {
    return outstandingPacketId.size() + reservedSlots < maximumOutstanding
        && outstandingPacketId.size() < maxPacketIdentifier;
  }

  private void reserve(SubscribedEventManager subscription) {
    reservations.merge(subscription, 1, Integer::sum);
    reservedSlots++;
  }

  private boolean consumeReservation(SubscribedEventManager subscription) {
    if (subscription == null) {
      return false;
    }
    Integer count = reservations.get(subscription);
    if (count == null || count == 0) {
      return false;
    }
    if (count == 1) {
      reservations.remove(subscription);
    } else {
      reservations.put(subscription, count - 1);
    }
    reservedSlots--;
    return true;
  }

  private boolean releaseReservation(SubscribedEventManager subscription) {
    if (subscription == null) {
      return false;
    }
    Integer count = reservations.get(subscription);
    if (count == null || count == 0) {
      return false;
    }
    if (count == 1) {
      reservations.remove(subscription);
    } else {
      reservations.put(subscription, count - 1);
    }
    reservedSlots--;
    return true;
  }

  private SubscribedEventManager grantNextWaiter() {
    if (!hasPublishCapacity()) {
      return null;
    }
    SubscribedEventManager waiter = waiters.poll();
    if (waiter == null) {
      return null;
    }
    waiting.remove(waiter);
    reserve(waiter);
    return waiter;
  }

  private int nextAvailablePacketIdentifier() {
    for (int attempt = 0; attempt < maxPacketIdentifier; attempt++) {
      packetId++;
      if (packetId > maxPacketIdentifier) {
        packetId = 1;
      }
      if (!outstandingPacketId.containsKey(packetId)) {
        return packetId;
      }
    }
    throw new PacketIdentifierExhaustedException();
  }

  private void resume(SubscribedEventManager subscription) {
    if (subscription != null) {
      subscription.resumeDelivery();
    }
  }
}
