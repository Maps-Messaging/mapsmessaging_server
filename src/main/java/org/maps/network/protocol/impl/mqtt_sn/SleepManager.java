/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.protocol.impl.mqtt_sn;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.maps.network.protocol.impl.mqtt_sn.packet.Publish;

public class SleepManager {

  private final int maxEvents;
  private final TreeMap<String, Queue<Publish>> sleepingMessages;

  public SleepManager(int maxEvents) {
    this.maxEvents = maxEvents;
    sleepingMessages = new TreeMap<>();
  }

  public boolean hasEvents() {
    return !sleepingMessages.isEmpty();
  }

  public Set<String> getDestinationList() {
    return sleepingMessages.keySet();
  }

  public int size() {
    int count = 0;
    for (Queue<Publish> queue : sleepingMessages.values()) {
      count += queue.size();
    }
    return count;
  }

  public Iterator<Publish> getMessages(String destination) {
    Queue<Publish> queue = sleepingMessages.get(destination);
    if (queue == null) {
      queue = new LinkedList<>();
    }
    return new MessageIterator(destination, queue);
  }

  public boolean storeEvent(String destinationName, Publish message) {
    Queue<Publish> currentList = sleepingMessages.computeIfAbsent(destinationName, k -> new LinkedList<>());
    currentList.add(message);
    if (currentList.size() > maxEvents) {
      currentList.poll(); // Pop the oldest
      return false;
    }
    return true;
  }

  private final class MessageIterator implements Iterator<Publish> {

    private final Queue<Publish> messageQueue;
    private final String destination;

    public MessageIterator(String destination, Queue<Publish> queue) {
      messageQueue = queue;
      this.destination = destination;
    }

    @Override
    public boolean hasNext() {
      return !messageQueue.isEmpty();
    }

    // We are matching the Iterator javadocs which says Next can throw a NoSuchElementException
    @java.lang.SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    @Override
    public Publish next() throws NoSuchElementException {
      if (messageQueue.isEmpty()) {
        throw new NoSuchElementException();
      }
      Publish message = messageQueue.poll();
      if (messageQueue.isEmpty()) {
        sleepingMessages.remove(destination);
      }
      return message;
    }

    @Override
    public void remove() {
      messageQueue.poll();
      if (messageQueue.isEmpty()) {
        sleepingMessages.remove(destination);
      }
    }

    @Override
    public void forEachRemaining(Consumer<? super Publish> action) {
      while (!messageQueue.isEmpty()) {
        action.accept(messageQueue.poll());
      }
    }
  }
}
