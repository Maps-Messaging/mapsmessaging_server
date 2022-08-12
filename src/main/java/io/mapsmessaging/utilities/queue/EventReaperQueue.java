package io.mapsmessaging.utilities.queue;

import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class EventReaperQueue {

  private final EventQueue[] localQueue;
  private final AtomicLong index;

  public EventReaperQueue() {
    index = new AtomicLong(0);
    int numberOfCPUs = Runtime.getRuntime().availableProcessors();
    localQueue = new EventQueue[numberOfCPUs];
    for (int x = 0; x < numberOfCPUs; x++) {
      localQueue[x] = new EventQueue();
    }
  }

  public void close() {
    for (EventQueue threadLocalQueue : localQueue) {
      threadLocalQueue.getAndClear();
    }
  }

  public void add(long id) {
    int idx = (int) index.incrementAndGet() % localQueue.length;
    localQueue[idx].add(id);
  }

  public Queue<Long> getAndClear() {
    if (Arrays.stream(localQueue).anyMatch(threadLocalQueue -> !threadLocalQueue.queue.isEmpty())) {
      Queue<Long> result = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(8192));
      for (EventQueue threadLocalQueue : localQueue) {
        Queue<Long> tQueue = threadLocalQueue.getAndClear();
        result.addAll(tQueue);
      }
      return result;
    }
    return new LinkedList<>();
  }

  private static final class EventQueue {

    private Queue<Long> queue;

    public EventQueue() {
      queue = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(8192));
    }

    public synchronized void add(long entry) {
      queue.offer(entry);
    }

    public synchronized Queue<Long> getAndClear() {
      Queue<Long> copy = queue;
      queue = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(8192));
      return copy;
    }
  }
}
