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

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.BasePublish;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import lombok.Getter;

public class EventManager<T extends BasePublish> {

  private final int maxEvents;
  private final TreeMap<String, Queue<T>> pendingMessages;
  private final Map<String, TopicRegister> registerMap;

  public EventManager(int maxEvents) {
    this.maxEvents = maxEvents;
    pendingMessages = new TreeMap<>();
    registerMap = new LinkedHashMap<>();
  }

  public boolean hasEvents() {
    return !pendingMessages.isEmpty();
  }

  public Set<String> getDestinationList() {
    return pendingMessages.keySet();
  }

  public int size() {
    int count = 0;
    for (Queue<T> queue : pendingMessages.values()) {
      count += queue.size();
    }
    return count;
  }

  public boolean sendRegister(String destination){
    TopicRegister register = registerMap.get(destination);
    if(register != null){
      boolean res = register.send;
      register.send = false;
      return res;
    }
    return false;
  }

  public Iterator<T> getMessages(String destination) {
    Queue<T> queue = pendingMessages.get(destination);
    if (queue == null) {
      queue = new LinkedList<>();
    }
    return new MessageIterator(destination, queue);
  }

  public void storeEvent(String destinationName, T message) {
    Queue<T> currentList = pendingMessages.computeIfAbsent(destinationName, k -> new LinkedList<>());
    currentList.add(message);
    if (currentList.size() > maxEvents) {
      currentList.poll(); // Pop the oldest
    }
    if(!registerMap.containsKey(destinationName)){
      registerMap.put(destinationName, new TopicRegister(message.getTopicId()));
    }
  }

  private final class MessageIterator implements Iterator<T> {

    private final Queue<T> messageQueue;
    private final String destination;

    public MessageIterator(String destination, Queue<T> queue) {
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
    public T next() throws NoSuchElementException {
      if (messageQueue.isEmpty()) {
        throw new NoSuchElementException();
      }
      T message = messageQueue.poll();
      if (messageQueue.isEmpty()) {
        pendingMessages.remove(destination);
      }
      return message;
    }

    @Override
    public void remove() {
      messageQueue.poll();
      if (messageQueue.isEmpty()) {
        pendingMessages.remove(destination);
      }
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
      while (!messageQueue.isEmpty()) {
        action.accept(messageQueue.poll());
      }
    }
  }

  private static class TopicRegister {

    @Getter private final int id;
    @Getter private boolean send;

    public TopicRegister(int id){
      this.id = id;
      send = true;
    }
  }
}
