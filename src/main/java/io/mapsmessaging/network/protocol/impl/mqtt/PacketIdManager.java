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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
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

  private int packetId;
  private int maximumOutstanding;
  private int reservedSlots;

  public PacketIdManager() {
    this(MAX_PACKET_IDENTIFIER);
  }

  PacketIdManager(int maxPacketIdentifier) {
    this.maxPacketIdentifier = Math.max(1, Math.min(maxPacketIdentifier, MAX_PACKET_IDENTIFIER));
    outstandingPacketId = new TreeMap<>();
    reservations = new IdentityHashMap<>();
    waiters = new ArrayDeque<>();
    waiting = Collections.newSetFromMap(new IdentityHashMap<>());
    maximumOutstanding = this.maxPacketIdentifier;
  }

  public synchronized void close() {
    outstandingPacketId.clear();
    reservations.clear();
    waiters.clear();
    waiting.clear();
    reservedSlots = 0;
    notifyAll();
  }

  public synchronized void setMaximumOutstanding(int maximumOutstanding) {
    this.maximumOutstanding = Math.max(1, Math.min(maximumOutstanding, maxPacketIdentifier));
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
      releaseReservation(subscription);
      wake = grantNextWaiter();
      notifyAll();
    }
    resume(wake);
  }

  public int nextPacketIdentifier(SubscribedEventManager subscription, long messageId) {
    synchronized (this) {
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
      return retVal;
    }
  }

  public synchronized int nextPacketIdentifier() {
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
      result = outstandingPacketId.remove(id);
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

  private void releaseReservation(SubscribedEventManager subscription) {
    if (subscription == null) {
      return;
    }
    Integer count = reservations.get(subscription);
    if (count == null || count == 0) {
      return;
    }
    if (count == 1) {
      reservations.remove(subscription);
    } else {
      reservations.put(subscription, count - 1);
    }
    reservedSlots--;
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
